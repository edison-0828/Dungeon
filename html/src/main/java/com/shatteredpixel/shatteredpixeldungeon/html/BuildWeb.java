/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.html;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;

import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

public class BuildWeb {

	public static void main(String[] args) throws IOException {
		File moduleDir = new File("").getAbsoluteFile();
		File coreAssets = new File(moduleDir, "../core/src/main/assets");
		File desktopAssets = new File(moduleDir, "../desktop/src/main/assets");
		if (!coreAssets.isDirectory()) {
			throw new IOException("Missing core assets: " + coreAssets.getAbsolutePath());
		}

		File merged = new File(moduleDir, "build/web-assets");
		if (merged.exists()) {
			deleteTree(merged.toPath());
		}
		copyTree(coreAssets.toPath(), merged.toPath());
		if (desktopAssets.isDirectory()) {
			copyTree(desktopAssets.toPath(), merged.toPath());
		}

		File output = new File(moduleDir, "build/dist");
		WebBackend backend = new WebBackend()
				.setHtmlTitle("Cinder Vault")
				.setHtmlWidth(480)
				.setHtmlHeight(800)
				.setCopyLoadingAsset(false);

		new TeaCompiler(backend)
				.addAssets(new AssetFileHandle(merged.getAbsolutePath()))
				.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
				.setMainClass(WebLauncher.class.getName())
				.setObfuscated(false)
				.setOutputName("game")
				.addReflectionClass("com.shatteredpixel.shatteredpixeldungeon.**")
				.addReflectionClass("com.watabou.**")
				.addReflectionClass("org.json.**")
				.build(output);

		File webapp = new File(output, "webapp");
		writeWalletAssets(moduleDir, webapp);
		File index = new File(webapp, "index.html");
		String html = Files.readString(index.toPath(), StandardCharsets.UTF_8);
		html = insertPageChrome(html);
		Files.writeString(index.toPath(), html, StandardCharsets.UTF_8);
		Files.copy(new File(merged, "fonts/pixel_font.png").toPath(),
				new File(webapp, "assets/startup-logo.png").toPath(),
				StandardCopyOption.REPLACE_EXISTING);
	}

	private static void writeWalletAssets(File moduleDir, File webapp) throws IOException {
		File bundled = new File(moduleDir, "wallet/dist/cinder-wallet.js");
		if (!bundled.isFile()) {
			throw new IOException("Missing wallet bundle: " + bundled.getAbsolutePath());
		}
		File scripts = new File(webapp, "scripts");
		Files.createDirectories(scripts.toPath());
		Files.copy(bundled.toPath(), new File(scripts, "cinder-wallet.js").toPath(), StandardCopyOption.REPLACE_EXISTING);

		Properties properties = new Properties();
		File local = new File(moduleDir, "../local.properties");
		if (local.isFile()) {
			try (InputStream in = new FileInputStream(local)) {
				properties.load(in);
			}
		}
		String recipient = sanitize(properties.getProperty("tip.recipient.pubkey", ""));
		String rpc = sanitize(properties.getProperty("tip.rpc.url", ""));
		String lamports = sanitize(properties.getProperty("tip.lamports", ""));
		if (lamports.isEmpty()) lamports = "10000000,50000000,100000000";
		String config = "window.cinderWalletConfig = {\n"
				+ "  recipient: \"" + jsString(recipient) + "\",\n"
				+ "  rpcUrl: \"" + jsString(rpc) + "\",\n"
				+ "  lamports: \"" + jsString(lamports) + "\"\n"
				+ "};\n";
		Files.writeString(new File(scripts, "wallet-config.js").toPath(), config, StandardCharsets.UTF_8);

		File wellKnown = new File(webapp, ".well-known");
		Files.createDirectories(wellKnown.toPath());
		Files.writeString(new File(wellKnown, "cherry-app.json").toPath(),
				"{\"appId\": \"4b472823-9404-4550-a58e-a8f985bae418\"}\n",
				StandardCharsets.UTF_8);
	}

	private static String insertPageChrome(String html) throws IOException {
		if (!html.contains("name=\"viewport\"")) {
			html = html.replace("<head>",
					"<head>\n        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		}
		String fit = "\n            html, body { width: 100%; max-width: 100%; overflow: hidden; }\n"
				+ "            body > div { max-width: 100vw; max-height: 100vh; }\n"
				+ "            #canvas {\n"
				+ "                width: min(100vw, calc(100vh * 480 / 800)) !important;\n"
				+ "                height: min(100vh, calc(100vw * 800 / 480)) !important;\n"
				+ "                max-width: 100vw !important;\n"
				+ "                max-height: 100vh !important;\n"
				+ "            }\n        ";
		if (!html.contains("100vw, calc(100vh * 480 / 800)")) {
			html = html.replace("</style>", fit + "</style>");
		}
		String game = "<script type=\"text/javascript\" charset=\"utf-8\" src=\"game.js\"></script>";
		if (!html.contains(game)) {
			throw new IOException("index.html has no game.js script");
		}
		if (html.contains("scripts/cinder-wallet.js")) return html;
		String block = "<script src=\"scripts/freetype.js\"></script>\n        "
				+ "<script src=\"scripts/wallet-config.js\"></script>\n        "
				+ "<script src=\"scripts/cinder-wallet.js?v=4\"></script>\n        "
				+ game;
		return html.replace(game, block);
	}

	private static String sanitize(String value) {
		if (value == null) return "";
		return value.replace("\\", "").replace("\"", "").replace("\r", "").replace("\n", "").trim();
	}

	private static String jsString(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c");
	}

	private static void copyTree(Path from, Path to) throws IOException {
		Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(to.resolve(from.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path dest = to.resolve(from.relativize(file));
				Files.createDirectories(dest.getParent());
				Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void deleteTree(Path root) throws IOException {
		if (!Files.exists(root)) return;
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
