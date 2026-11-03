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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.GateMerchant;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.SupporterScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ShopkeeperSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.PlatformSupport;

import java.util.LinkedHashMap;
import java.util.Map;

public class WndGateMerchant extends Window {

	private static final int WIDTH = 120;
	private static final int GAP = 2;

	private boolean alive = true;
	private boolean busy = false;

	private IconTitle title;
	private RenderedTextBlock intro;
	private RenderedTextBlock pending;
	private RenderedTextBlock status;
	private RedButton[] buys;
	private RedButton connect;
	private RenderedTextBlock account;
	private RedButton install;

	public WndGateMerchant() {
		super();

		title = new IconTitle(new ShopkeeperSprite(), Messages.get(this, "title"));
		add(title);

		intro = PixelScene.renderTextBlock(6);
		intro.text(Messages.get(this, "intro"), WIDTH);
		add(intro);

		pending = PixelScene.renderTextBlock(6);
		add(pending);

		connect = new RedButton(walletButtonLabel(), 8) {
			@Override
			protected void onClick() {
				if (GateMerchant.wallet().isEmpty()) startConnect();
				else startDisconnect();
			}
		};
		connect.multiline = true;
		add(connect);

		account = PixelScene.renderTextBlock(6);
		add(account);

		GateMerchant.Offer[] offers = GateMerchant.Offer.values();
		buys = new RedButton[offers.length];
		for (int i = 0; i < offers.length; i++) {
			final GateMerchant.Offer offer = offers[i];
			RedButton button = new RedButton(label(offer), 8) {
				@Override
				protected void onClick() {
					buy(offer);
				}
			};
			button.icon(Icons.GOLD.get());
			button.multiline = true;
			add(button);
			buys[i] = button;
		}

		status = PixelScene.renderTextBlock(6);
		add(status);

		install = new RedButton(Messages.get(SupporterScene.class, "tip_install",
				Messages.get(SupporterScene.class, "wallet_1")), 8) {
			@Override
			protected void onClick() {
				String[] uris = ShatteredPixelDungeon.platform.solanaWalletInstallUris();
				if (uris.length > 0) {
					ShatteredPixelDungeon.platform.openURI(uris[0]);
				}
			}
		};
		install.multiline = true;
		install.visible = false;
		add(install);

		refreshPending();
		refreshStatus();
		layoutContents();
	}

	private void startConnect() {
		if (!alive || busy) return;
		busy = true;
		install.visible = false;
		status.text(Messages.get(SupporterScene.class, "tip_working"));
		refreshButtons();
		layoutContents();
		boolean started = ShatteredPixelDungeon.platform.requestSolanaConnect(walletListener());
		if (!started) {
			busy = false;
			refreshButtons();
			layoutContents();
		}
	}

	private void startDisconnect() {
		if (!alive || busy) return;
		busy = true;
		install.visible = false;
		status.text(Messages.get(SupporterScene.class, "tip_working"));
		refreshButtons();
		layoutContents();
		boolean started = ShatteredPixelDungeon.platform.requestSolanaDisconnect(walletListener());
		if (!started) {
			busy = false;
			refreshButtons();
			layoutContents();
		}
	}

	private PlatformSupport.SolanaTipListener walletListener() {
		return new PlatformSupport.SolanaTipListener() {
			@Override
			public void onResult(int kind, String detail) {
				if (!alive) return;
				if (kind != PlatformSupport.SolanaTip.RECORD) busy = false;
				install.visible = false;
				if (kind == PlatformSupport.SolanaTip.CONNECTED) {
					status.text(Messages.get(SupporterScene.class, "tip_identity"));
				} else if (kind == PlatformSupport.SolanaTip.RECORD) {
					if (!GateMerchant.wallet().isEmpty()) applyRecord(detail);
				} else if (kind == PlatformSupport.SolanaTip.DISCONNECTED) {
					status.text(Messages.get(SupporterScene.class, "tip_disconnected"));
				} else {
					showWalletResult(kind, detail);
				}
				refreshPending();
				refreshButtons();
				layoutContents();
			}
		};
	}

	private static String walletButtonLabel() {
		return GateMerchant.wallet().isEmpty()
				? Messages.get(WndGateMerchant.class, "connect")
				: Messages.get(SupporterScene.class, "disconnect");
	}

	private void applyRecord(String detail) {
		String wallet = detail == null ? "" : detail;
		String memos = "";
		int split = wallet.indexOf('\n');
		if (split >= 0) {
			memos = wallet.substring(split + 1);
			wallet = wallet.substring(0, split);
		}
		if (PlatformSupport.SolanaTip.UNREAD.equals(memos)) {
			GateMerchant.noteChain(wallet, "");
			status.text(Messages.get(this, "record_unread", shortWallet(wallet)));
			return;
		}
		GateMerchant.noteChain(wallet, memos);
		if (memos == null || memos.isEmpty()) {
			status.text(Messages.get(this, "record_empty", shortWallet(wallet)));
		} else {
			status.text(Messages.get(this, "record_found", shortWallet(wallet), ownedNames()));
		}
	}

	private String ownedNames() {
		StringBuilder names = new StringBuilder();
		for (GateMerchant.Offer offer : GateMerchant.Offer.values()) {
			if (!GateMerchant.claimed(offer)) continue;
			if (names.length() > 0) names.append(Messages.get(this, "sep"));
			names.append(Messages.get(this, offer.id));
		}
		return names.toString();
	}

	private String label(GateMerchant.Offer offer) {
		String name = Messages.get(this, offer.id);
		if (GateMerchant.claimed(offer)) return Messages.get(this, "bought", name);
		return Messages.get(this, "buy", name, GateMerchant.formatSol(offer.lamports), Messages.get(this, offer.id + "_body"));
	}

	private void buy(GateMerchant.Offer offer) {
		if (!alive || busy || GateMerchant.claimed(offer)) return;

		busy = true;
		install.visible = false;
		status.text(Messages.get(SupporterScene.class, "tip_working"));
		refreshButtons();
		layoutContents();

		boolean started = ShatteredPixelDungeon.platform.requestSolanaTransfer(offer.lamports, offer.memo, new PlatformSupport.SolanaTipListener() {
			@Override
			public void onResult(int kind, String detail) {
				if (kind == PlatformSupport.SolanaTip.SUBMITTED) {
					GateMerchant.Result result = GateMerchant.remember(offer, detail);
					if (!alive) return;
					busy = false;
					install.visible = false;
					if (result == GateMerchant.Result.DUPLICATE) {
						status.text(Messages.get(WndGateMerchant.this, "duplicate"));
					} else {
						status.text(Messages.get(WndGateMerchant.this, "paid",
								Messages.get(WndGateMerchant.this, offer.id), shortSignature(detail)));
					}
					refreshPending();
					refreshButtons();
					layoutContents();
					return;
				}
				if (kind == PlatformSupport.SolanaTip.ALREADY) {
					if (!alive) return;
					busy = false;
					GateMerchant.noteChain(GateMerchant.wallet(), offer.memo);
					status.text(Messages.get(WndGateMerchant.this, "already"));
					refreshPending();
					refreshButtons();
					layoutContents();
					return;
				}
				if (!alive) return;
				busy = false;
				install.visible = false;
				showWalletResult(kind, detail);
				refreshButtons();
				layoutContents();
			}
		});
		if (!started) {
			busy = false;
			refreshButtons();
			layoutContents();
		}
	}

	private void showWalletResult(int kind, String detail) {
		switch (kind) {
					case PlatformSupport.SolanaTip.CANCELLED:
						status.text(Messages.get(SupporterScene.class, "tip_cancelled"));
						break;
					case PlatformSupport.SolanaTip.NO_WALLET:
						install.visible = true;
						status.text(Messages.get(SupporterScene.class, "tip_no_wallet"));
						break;
					case PlatformSupport.SolanaTip.NOT_CONFIGURED:
						status.text(Messages.get(SupporterScene.class, "tip_unconfigured"));
						break;
					case PlatformSupport.SolanaTip.OFFLINE:
						status.text(Messages.get(SupporterScene.class, "tip_offline"));
						break;
					default:
						if (PlatformSupport.SolanaTip.SHORT.equals(detail)) {
							status.text(Messages.get(SupporterScene.class, "tip_short"));
						} else if (PlatformSupport.SolanaTip.UNCONFIRMED.equals(detail)) {
							status.text(Messages.get(this, "unconfirmed"));
						} else {
							status.text(Messages.get(SupporterScene.class, "tip_failed", detail == null ? "" : detail));
						}
						break;
				}
	}

	private void refreshPending() {
		LinkedHashMap<GateMerchant.Offer, Integer> counts = new LinkedHashMap<>();
		for (GateMerchant.Offer offer : GateMerchant.Offer.values()) {
			if (!GateMerchant.claimed(offer)) continue;
			counts.put(offer, 1);
		}
		if (counts.isEmpty()) {
			pending.text(Messages.get(this, "pending_none"), WIDTH);
			return;
		}
		StringBuilder list = new StringBuilder();
		for (Map.Entry<GateMerchant.Offer, Integer> entry : counts.entrySet()) {
			if (list.length() > 0) list.append(Messages.get(this, "sep"));
			list.append(Messages.get(this, entry.getKey().id));
			if (entry.getValue() > 1) {
				list.append(Messages.get(this, "times", entry.getValue()));
			}
		}
		pending.text(Messages.get(this, "pending", list.toString()), WIDTH);
	}

	private void refreshStatus() {
		PlatformSupport platform = ShatteredPixelDungeon.platform;
		if (!platform.supportsSolanaTips()) {
			status.text(Messages.get(this, "desktop"));
		} else if (!platform.solanaTipsConfigured()) {
			status.text(Messages.get(SupporterScene.class, "tip_unconfigured"));
		} else if (!platform.solanaNetworkAvailable()) {
			status.text(Messages.get(SupporterScene.class, "tip_offline"));
		} else if (!GateMerchant.wallet().isEmpty() && (status.text() == null || status.text().length() == 0)) {
			String names = ownedNames();
			if (names.isEmpty()) status.text(Messages.get(this, "record_empty", shortWallet(GateMerchant.wallet())));
			else status.text(Messages.get(this, "record_found", shortWallet(GateMerchant.wallet()), names));
		} else if (status.text() == null || status.text().length() == 0) {
			status.text(Messages.get(this, "connect_first"));
		}
	}

	private void refreshAccount() {
		String wallet = GateMerchant.wallet();
		if (account != null) {
			if (wallet.isEmpty()) account.text(Messages.get(SupporterScene.class, "tip_signed_out"), WIDTH);
			else account.text(Messages.get(SupporterScene.class, "tip_account", wallet), WIDTH);
		}
		if (connect != null) connect.text(walletButtonLabel());
	}

	private void refreshButtons() {
		refreshAccount();
		PlatformSupport platform = ShatteredPixelDungeon.platform;
		boolean ready = !busy
				&& platform.supportsSolanaTips()
				&& platform.solanaTipsConfigured();
		connect.enable(ready);
		connect.alpha(ready ? 1f : 0.6f);
		boolean online = ready && platform.solanaNetworkAvailable();
		GateMerchant.Offer[] offers = GateMerchant.Offer.values();
		for (int i = 0; i < buys.length; i++) {
			boolean enabled = online && !GateMerchant.claimed(offers[i]);
			buys[i].text(label(offers[i]));
			buys[i].enable(enabled);
			buys[i].alpha(enabled ? 1f : 0.6f);
		}
	}

	private void layoutContents() {
		refreshAccount();
		float y = 0;
		title.setRect(0, y, WIDTH, 0);
		y = title.bottom() + GAP;

		intro.setPos(0, y);
		y = intro.bottom() + GAP;

		pending.setPos(0, y);
		y = pending.bottom() + GAP;

		connect.setRect(0, y, WIDTH, 18);
		float connectHeight = Math.max(18, connect.reqHeight() + 4);
		connect.setRect(0, y, WIDTH, connectHeight);
		y = connect.bottom() + GAP;

		account.setPos(0, y);
		y = account.height() > 0 ? account.bottom() + GAP : y;

		for (RedButton button : buys) {
			button.setRect(0, y, WIDTH, 24);
			float height = Math.max(24, button.reqHeight() + 4);
			button.setRect(0, y, WIDTH, height);
			y = button.bottom() + GAP;
		}

		status.setPos(0, y);
		y = status.height() > 0 ? status.bottom() + GAP : y;

		if (install.visible) {
			install.setRect(0, y, WIDTH, 18);
			float height = Math.max(18, install.reqHeight() + 4);
			install.setRect(0, y, WIDTH, height);
			y = install.bottom() + GAP;
		}

		resize(WIDTH, (int) Math.ceil(y));
		refreshButtons();
	}

	private static String shortWallet(String wallet) {
		return shortSignature(wallet);
	}

	private static String shortSignature(String signature) {
		if (signature == null || signature.isEmpty()) return "";
		if (signature.length() <= 12) return signature;
		return signature.substring(0, 4) + "..." + signature.substring(signature.length() - 4);
	}

	@Override
	public void destroy() {
		alive = false;
		super.destroy();
	}
}
