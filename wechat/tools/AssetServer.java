import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetServer {
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: AssetServer <root> <port>");
			System.exit(2);
		}
		Path root = Paths.get(args[0]).toAbsolutePath().normalize();
		int port = Integer.parseInt(args[1]);
		if (!Files.isDirectory(root)) {
			System.err.println("Asset root does not exist: " + root);
			System.exit(1);
		}
		HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
		server.createContext("/", exchange -> handle(root, exchange));
		server.start();
		System.out.println("Serving " + root + " at http://127.0.0.1:" + port + "/");
		Thread.currentThread().join();
	}

	private static void handle(Path root, HttpExchange exchange) throws IOException {
		boolean started = false;
		try {
			String method = exchange.getRequestMethod();
			if ("OPTIONS".equalsIgnoreCase(method)) {
				exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
				exchange.sendResponseHeaders(204, -1);
				started = true;
				return;
			}
			if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
				sendText(exchange, 405, "method");
				return;
			}
			String raw = exchange.getRequestURI().getPath();
			if (raw == null || raw.isEmpty() || "/".equals(raw)) {
				sendText(exchange, 200, "ok");
				return;
			}
			String relative = raw.startsWith("/") ? raw.substring(1) : raw;
			Path file = root.resolve(relative).normalize();
			if (!file.startsWith(root) || !Files.isRegularFile(file)) {
				sendText(exchange, 404, "missing");
				return;
			}
			exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.getResponseHeaders().add("Cache-Control", "no-cache");
			long size = Files.size(file);
			exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(method) ? -1 : size);
			started = true;
			if (!"HEAD".equalsIgnoreCase(method)) {
				try (OutputStream out = exchange.getResponseBody()) {
					Files.copy(file, out);
				}
			}
		} catch (Exception err) {
			if (!started) sendText(exchange, 500, err.getMessage() == null ? "error" : err.getMessage());
		} finally {
			exchange.close();
		}
	}

	private static void sendText(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}
}
