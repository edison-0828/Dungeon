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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.shatteredpixel.shatteredpixeldungeon.GateMerchant;
import com.watabou.utils.PlatformSupport;

import java.util.ArrayList;
import java.util.HashMap;

public class WebPlatformSupport extends PlatformSupport {

	private static FreeTypeFontGenerator basicFontGenerator;
	private static FreeTypeFontGenerator asianFontGenerator;

	@Override
	public void updateDisplaySize() {
	}

	@Override
	public void updateSystemUI() {
	}

	@Override
	public void openTextEntry(String initial, boolean multiline, int maxLength, TextEntryListener listener) {
		super.openTextEntry(initial, multiline, maxLength, listener);
		KeyboardBridge.show(initial == null ? "" : initial, multiline, maxLength,
				new KeyboardBridge.JsStringFn() {
					@Override
					public void accept(String value) {
						onTextEntryChanged(value);
					}
				},
				new KeyboardBridge.JsStringFn() {
					@Override
					public void accept(String value) {
						onTextEntryConfirmed(value);
					}
				});
	}

	@Override
	public void closeTextEntry() {
		super.closeTextEntry();
		KeyboardBridge.hide();
	}

	@Override
	public boolean connectedToUnmeteredNetwork() {
		return true;
	}

	@Override
	public boolean supportsVibration() {
		return false;
	}

	private static final String[] WALLET_INSTALL_URIS = {
			"https://phantom.com/download",
			"https://www.solflare.com/download",
			"https://backpack.app/",
			"https://web3.okx.com/download",
			"https://www.binance.com/en/web3wallet"
	};
	private static final long[] DEFAULT_TIP_LAMPORTS = {10_000_000L, 50_000_000L, 100_000_000L};

	private static final int ACTION_CONNECT = 1;
	private static final int ACTION_DISCONNECT = 2;
	private static final int ACTION_PAY = 3;

	private volatile int sessionEpoch;
	private volatile boolean requestInFlight;
	private boolean bridgeReady;

	@Override
	public boolean supportsSolanaTips() {
		return true;
	}

	@Override
	public boolean solanaTipsConfigured() {
		return looksLikeSolanaPubkey(readRecipient());
	}

	@Override
	public boolean solanaNetworkAvailable() {
		ensureBridge();
		return CinderWalletBridge.online();
	}

	@Override
	public String solanaNetworkName() {
		return "mainnet-beta";
	}

	@Override
	public long[] solanaTipLamports() {
		ensureBridge();
		String raw = CinderWalletBridge.lamports();
		if (raw == null || raw.trim().isEmpty()) return DEFAULT_TIP_LAMPORTS.clone();
		String[] parts = raw.split(",");
		long[] values = new long[parts.length];
		int count = 0;
		for (String part : parts) {
			try {
				long lamports = Long.parseLong(part.trim());
				if (lamports > 0) values[count++] = lamports;
			} catch (NumberFormatException ignored) {
			}
		}
		if (count == 0) return DEFAULT_TIP_LAMPORTS.clone();
		long[] trimmed = new long[count];
		System.arraycopy(values, 0, trimmed, 0, count);
		return trimmed;
	}

	@Override
	public String[] solanaWalletInstallUris() {
		return WALLET_INSTALL_URIS.clone();
	}

	@Override
	public String solanaTipRecipient() {
		String recipient = readRecipient();
		return looksLikeSolanaPubkey(recipient) ? recipient : "";
	}

	@Override
	public boolean requestSolanaTransfer(long lamports, String memo, SolanaTipListener listener) {
		return begin(ACTION_PAY, lamports, memo, listener);
	}

	@Override
	public boolean requestSolanaConnect(SolanaTipListener listener) {
		return begin(ACTION_CONNECT, 0, null, listener);
	}

	@Override
	public boolean requestSolanaDisconnect(SolanaTipListener listener) {
		return begin(ACTION_DISCONNECT, 0, null, listener);
	}

	private boolean begin(int action, long lamports, String memo, SolanaTipListener listener) {
		if (listener == null) return false;
		if (!solanaTipsConfigured()) {
			listener.onResult(SolanaTip.NOT_CONFIGURED, "");
			return false;
		}
		if (!solanaNetworkAvailable()) {
			listener.onResult(SolanaTip.OFFLINE, "");
			return false;
		}
		if (requestInFlight || (action == ACTION_PAY && lamports <= 0)) {
			listener.onResult(SolanaTip.FAILED, "");
			return false;
		}
		ensureBridge();
		requestInFlight = true;
		if (action == ACTION_DISCONNECT) sessionEpoch++;
		final int epoch = sessionEpoch;
		final String payMemo = memo == null ? "" : memo;
		CinderWalletBridge.JsStringFn ok = new CinderWalletBridge.JsStringFn() {
			@Override
			public void accept(String value) {
				requestInFlight = false;
				if (epoch != sessionEpoch) return;
				if (action == ACTION_DISCONNECT) {
					deliver(listener, epoch, SolanaTip.DISCONNECTED, "");
				} else if (action == ACTION_CONNECT) {
					String pubkey = value == null ? "" : value.trim();
					if (pubkey.isEmpty()) {
						deliver(listener, epoch, SolanaTip.FAILED, "");
					} else {
						deliver(listener, epoch, SolanaTip.CONNECTED, pubkey);
						readRecord(pubkey, listener, epoch);
					}
				} else if (value == null || value.isEmpty()) {
					deliver(listener, epoch, SolanaTip.FAILED, "");
				} else {
					deliver(listener, epoch, SolanaTip.SUBMITTED, value.trim());
				}
			}
		};
		CinderWalletBridge.JsStringFn err = new CinderWalletBridge.JsStringFn() {
			@Override
			public void accept(String value) {
				requestInFlight = false;
				if (epoch != sessionEpoch) return;
				deliverPacked(listener, epoch, value);
			}
		};
		if (action == ACTION_CONNECT) CinderWalletBridge.connect(ok, err);
		else if (action == ACTION_DISCONNECT) CinderWalletBridge.disconnect(ok, err);
		else CinderWalletBridge.transfer(Long.toString(lamports), payMemo, ok, err);
		return true;
	}

	private void readRecord(final String owner, final SolanaTipListener listener, final int epoch) {
		CinderWalletBridge.packRecord(owner, new CinderWalletBridge.JsStringFn() {
			@Override
			public void accept(String value) {
				if (epoch != sessionEpoch) return;
				deliver(listener, epoch, SolanaTip.RECORD, owner + "\n" + (value == null ? "" : value));
			}
		}, new CinderWalletBridge.JsStringFn() {
			@Override
			public void accept(String value) {
				if (epoch != sessionEpoch) return;
				deliver(listener, epoch, SolanaTip.RECORD, owner + "\n" + SolanaTip.UNREAD);
			}
		});
	}

	private void deliverPacked(SolanaTipListener listener, int epoch, String packed) {
		String code = packed == null ? "FAILED" : packed;
		String detail = "";
		int split = code.indexOf('\n');
		if (split >= 0) {
			detail = code.substring(split + 1);
			code = code.substring(0, split);
		}
		int kind = SolanaTip.FAILED;
		String out = detail;
		if ("CANCELLED".equals(code)) {
			kind = SolanaTip.CANCELLED;
			out = "";
		} else if ("NO_WALLET".equals(code)) {
			kind = SolanaTip.NO_WALLET;
			out = "";
		} else if ("NOT_CONFIGURED".equals(code)) {
			kind = SolanaTip.NOT_CONFIGURED;
			out = "";
		} else if ("OFFLINE".equals(code)) {
			kind = SolanaTip.OFFLINE;
			out = "";
		} else if ("ALREADY".equals(code)) {
			kind = SolanaTip.ALREADY;
		} else if ("SHORT".equals(code)) {
			out = SolanaTip.SHORT;
		} else if ("UNCONFIRMED".equals(code)) {
			out = SolanaTip.UNCONFIRMED;
		} else if ("POLICY".equals(code)) {
			out = detail == null ? "" : detail.trim();
		}
		deliver(listener, epoch, kind, out);
	}

	private void deliver(final SolanaTipListener listener, final int epoch, final int kind, final String detail) {
		if (Gdx.app == null || epoch != sessionEpoch) return;
		final String safe = detail == null ? "" : detail;
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				if (epoch != sessionEpoch) return;
				if (kind == SolanaTip.CONNECTED) GateMerchant.noteSession(safe, "");
				else if (kind == SolanaTip.DISCONNECTED) GateMerchant.forgetWallet();
				listener.onResult(kind, safe);
			}
		});
	}

	private String readRecipient() {
		ensureBridge();
		String recipient = CinderWalletBridge.recipient();
		return recipient == null ? "" : recipient.trim();
	}

	private void ensureBridge() {
		if (bridgeReady) return;
		CinderWalletBridge.install();
		bridgeReady = true;
	}

	private static boolean looksLikeSolanaPubkey(String value) {
		if (value == null) return false;
		String pubkey = value.trim();
		if (pubkey.isEmpty() || "TIP_RECIPIENT_PUBKEY".equals(pubkey)) return false;
		if (pubkey.length() < 32 || pubkey.length() > 44) return false;
		for (int i = 0; i < pubkey.length(); i++) {
			char c = pubkey.charAt(i);
			boolean ok = (c >= '1' && c <= '9')
					|| (c >= 'A' && c <= 'H')
					|| (c >= 'J' && c <= 'N')
					|| (c >= 'P' && c <= 'Z')
					|| (c >= 'a' && c <= 'k')
					|| (c >= 'm' && c <= 'z');
			if (!ok) return false;
		}
		return true;
	}

	@Override
	public void setupFontGenerators(int pageSize, boolean systemfont) {
		if (fonts != null && this.pageSize == pageSize && this.systemfont == systemfont) {
			return;
		}
		this.pageSize = pageSize;
		this.systemfont = systemfont;

		resetGenerators(false);
		fonts = new HashMap<>();

		if (systemfont) {
			basicFontGenerator = asianFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/droid_sans.ttf"));
		} else {
			basicFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pixel_font.ttf"));
			asianFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/droid_sans.ttf"));
		}

		fonts.put(basicFontGenerator, new HashMap<>());
		fonts.put(asianFontGenerator, new HashMap<>());

		packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 1, false);
	}

	@Override
	protected FreeTypeFontGenerator getGeneratorForString(String input) {
		if (containsCjk(input)) {
			return asianFontGenerator;
		} else {
			return basicFontGenerator;
		}
	}

	@Override
	public String[] splitforTextBlock(String text, boolean multiline) {
		ArrayList<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int i = 0;
		while (i < text.length()) {
			if (text.charAt(i) == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
				flush(parts, current);
				parts.add("**");
				i += 2;
				continue;
			}
			int cp = text.codePointAt(i);
			if (isSplitPoint(cp, multiline)) {
				flush(parts, current);
				parts.add(new String(Character.toChars(cp)));
			} else {
				current.appendCodePoint(cp);
			}
			i += Character.charCount(cp);
		}
		flush(parts, current);
		return parts.toArray(new String[0]);
	}

	private static void flush(ArrayList<String> parts, StringBuilder current) {
		if (current.length() > 0) {
			parts.add(current.toString());
			current.setLength(0);
		}
	}

	private static boolean isSplitPoint(int cp, boolean multiline) {
		return cp == '\n' || cp == '_' || (multiline && cp == ' ') || isCjk(cp);
	}

	private static boolean containsCjk(String input) {
		int i = 0;
		while (i < input.length()) {
			int cp = input.codePointAt(i);
			if (isCjk(cp)) return true;
			i += Character.charCount(cp);
		}
		return false;
	}

	private static boolean isCjk(int cp) {
		return (cp >= 0x4E00 && cp <= 0x9FFF)
				|| (cp >= 0x3000 && cp <= 0x303F)
				|| (cp >= 0x3040 && cp <= 0x30FF)
				|| (cp >= 0xAC00 && cp <= 0xD7AF)
				|| (cp >= 0xFF00 && cp <= 0xFFEF);
	}
}
