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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.RectF;

import java.util.Locale;

public class SupporterScene extends PixelScene {

	private static final int BTN_HEIGHT = 22;
	private static final int GAP = 2;

	private boolean alive = true;
	private boolean busy = false;
	private boolean showInstallLinks = false;
	private int elementWidth;
	private float columnWidth;
	private Component column;
	private ScrollPane list;
	private RenderedTextBlock intro;
	private RenderedTextBlock status;
	private StyledButton[] tiers = new StyledButton[0];
	private long[] tierLamports = new long[0];
	private StyledButton copyAddress;
	private StyledButton[] installLinks = new StyledButton[0];
	private SupporterMessage msg;
	private StyledButton patreon;

	@Override
	public void create() {
		super.create();

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		elementWidth = PixelScene.landscape() ? 202 : 120;

		TitleBackground BG = new TitleBackground(w, h);
		add(BG);

		w -= insets.right + insets.left;
		h -= insets.top + insets.bottom;

		ExitButton btnExit = new ExitButton();
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle(Icons.GOLD.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		if (!ShatteredPixelDungeon.platform.supportsSolanaTips()) {
			layoutPatreonOnly(insets, w, h);
			return;
		}

		columnWidth = w;
		column = new Component();
		list = new ScrollPane(column);
		add(list);

		boolean nativeWallet = ShatteredPixelDungeon.platform.solanaNativeDevice();

		intro = PixelScene.renderTextBlock(6);
		intro.text(Messages.get(this, nativeWallet ? "tip_intro" : "tip_manual",
				ShatteredPixelDungeon.platform.solanaNetworkName()));
		column.add(intro);

		if (nativeWallet) {
			tierLamports = ShatteredPixelDungeon.platform.solanaTipLamports();
			tiers = new StyledButton[tierLamports.length];
			for (int i = 0; i < tierLamports.length; i++) {
				final long lamports = tierLamports[i];
				StyledButton tier = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
						Messages.get(this, "tip_button", formatSol(lamports)), 8) {
					@Override
					protected void onClick() {
						startTip(lamports);
					}
				};
				tier.multiline = true;
				column.add(tier);
				tiers[i] = tier;
			}
		} else {
			final String address = ShatteredPixelDungeon.platform.solanaTipRecipient();
			if (!address.isEmpty()) {
				copyAddress = new StyledButton(Chrome.Type.GREY_BUTTON_TR, wrapAddress(address), 6) {
					@Override
					protected void onClick() {
						Gdx.app.getClipboard().setContents(address);
						status.text(Messages.get(SupporterScene.this, "tip_copied"));
						layoutColumn();
						scrollStatusIntoView();
					}
				};
				copyAddress.multiline = true;
				copyAddress.leftJustify = true;
				column.add(copyAddress);
			}
		}

		status = PixelScene.renderTextBlock(6);
		column.add(status);

		if (nativeWallet) {
			String[] uris = ShatteredPixelDungeon.platform.solanaWalletInstallUris();
			installLinks = new StyledButton[uris.length];
			for (int i = 0; i < uris.length; i++) {
				final String uri = uris[i];
				String wallet = Messages.get(this, "wallet_" + (i + 1));
				StyledButton install = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
						Messages.get(this, "tip_install", wallet), 8) {
					@Override
					protected void onClick() {
						ShatteredPixelDungeon.platform.openURI(uri);
					}
				};
				install.multiline = true;
				install.visible = false;
				column.add(install);
				installLinks[i] = install;
			}
		}

		msg = new SupporterMessage();
		column.add(msg);
		patreon = patreonButton();
		column.add(patreon);

		list.setRect(insets.left, insets.top + 18, w, h - 18);
		refreshStatus();
		layoutColumn();
	}

	private void layoutPatreonOnly(RectF insets, int w, int h) {
		msg = new SupporterMessage();
		msg.setSize(elementWidth, 0);
		add(msg);

		patreon = patreonButton();
		patreon.setSize(elementWidth, BTN_HEIGHT);
		add(patreon);

		float elementHeight = msg.height() + BTN_HEIGHT + GAP;
		float top = insets.top + 16 + (h - 16 - elementHeight) / 2f;
		float left = insets.left + (w - elementWidth) / 2f;
		msg.setPos(left, top);
		align(msg);
		patreon.setPos(left, msg.bottom() + GAP);
		align(patreon);
	}

	private StyledButton patreonButton() {
		StyledButton link = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "supporter_link")){
			@Override
			protected void onClick() {
				super.onClick();
				String link = "https://www.patreon.com/ShatteredPixel";
				//tracking codes, so that the website knows where this pageview came from
				link += "?utm_source=shatteredpd";
				link += "&utm_medium=supporter_page";
				link += "&utm_campaign=ingame_link";
				ShatteredPixelDungeon.platform.openURI(link);
			}
		};
		link.icon(Icons.get(Icons.GOLD));
		link.textColor(Window.TITLE_COLOR);
		return link;
	}

	private void startTip(long lamports) {
		if (!alive || busy) return;
		busy = true;
		showInstallLinks = false;
		status.text(Messages.get(this, "tip_working"));
		refreshButtons();
		layoutColumn();
		boolean started = ShatteredPixelDungeon.platform.requestSolanaTip(lamports, new PlatformSupport.SolanaTipListener() {
			@Override
			public void onResult(int kind, String detail) {
				if (!alive) return;
				busy = false;
				switch (kind) {
					case PlatformSupport.SolanaTip.SUBMITTED:
						showInstallLinks = false;
						status.text(Messages.get(SupporterScene.this, "tip_submitted", shortSignature(detail)));
						break;
					case PlatformSupport.SolanaTip.CANCELLED:
						showInstallLinks = false;
						status.text(Messages.get(SupporterScene.this, "tip_cancelled"));
						break;
					case PlatformSupport.SolanaTip.NO_WALLET:
						showInstallLinks = true;
						status.text(Messages.get(SupporterScene.this, "tip_no_wallet"));
						break;
					case PlatformSupport.SolanaTip.NOT_CONFIGURED:
						showInstallLinks = false;
						status.text(Messages.get(SupporterScene.this, "tip_unconfigured"));
						break;
					case PlatformSupport.SolanaTip.OFFLINE:
						showInstallLinks = false;
						status.text(Messages.get(SupporterScene.this, "tip_offline"));
						break;
					default:
						showInstallLinks = false;
						if (PlatformSupport.SolanaTip.SHORT.equals(detail)) {
							status.text(Messages.get(SupporterScene.this, "tip_short"));
						} else {
							status.text(Messages.get(SupporterScene.this, "tip_failed", detail == null ? "" : detail));
						}
						break;
				}
				refreshButtons();
				layoutColumn();
				scrollStatusIntoView();
			}
		});
		if (!started) {
			busy = false;
			refreshButtons();
			layoutColumn();
			scrollStatusIntoView();
		}
	}

	private void scrollStatusIntoView() {
		if (list == null || status == null || status.height() <= 0) return;
		list.scrollTo(0, status.top());
	}

	private void refreshStatus() {
		PlatformSupport platform = ShatteredPixelDungeon.platform;
		if (!platform.solanaTipsConfigured()) {
			status.text(Messages.get(this, "tip_unconfigured"));
		} else if (platform.solanaNativeDevice() && !platform.solanaNetworkAvailable()) {
			status.text(Messages.get(this, "tip_offline"));
		} else {
			status.text("");
		}
	}

	private void refreshButtons() {
		boolean enabled = !busy && ShatteredPixelDungeon.platform.solanaTipsConfigured();
		for (StyledButton tier : tiers) {
			tier.alpha(enabled ? 1f : 0.5f);
		}
		for (StyledButton install : installLinks) {
			install.visible = showInstallLinks;
		}
	}

	private void layoutColumn() {
		if (column == null) return;
		float left = Math.max(0, (columnWidth - elementWidth) / 2f);
		float y = 0;

		intro.maxWidth(elementWidth);
		intro.setPos(left, y);
		y = intro.bottom() + GAP;

		for (StyledButton tier : tiers) {
			tier.setRect(left, y, elementWidth, BTN_HEIGHT);
			y = tier.bottom() + GAP;
		}

		if (copyAddress != null) {
			copyAddress.setRect(left, y, elementWidth, BTN_HEIGHT);
			float buttonHeight = Math.max(BTN_HEIGHT, copyAddress.reqHeight() + 6);
			copyAddress.setRect(left, y, elementWidth, buttonHeight);
			y = copyAddress.bottom() + GAP;
		}

		status.maxWidth(elementWidth);
		status.setPos(left, y);
		y = status.height() > 0 ? status.bottom() + GAP : y;

		if (showInstallLinks) {
			for (StyledButton install : installLinks) {
				install.setRect(left, y, elementWidth, BTN_HEIGHT);
				y = install.bottom() + GAP;
			}
		}

		msg.setSize(elementWidth, 0);
		msg.setPos(left, y);
		y = msg.bottom() + GAP;

		patreon.setRect(left, y, elementWidth, BTN_HEIGHT);
		y = patreon.bottom() + 4;

		column.setSize(columnWidth, y);
		if (list != null) list.setSize(list.width(), list.height());
	}

	private static String wrapAddress(String address) {
		StringBuilder wrapped = new StringBuilder(address.length() + 4);
		for (int i = 0; i < address.length(); i++) {
			if (i > 0 && i % 16 == 0) wrapped.append('\n');
			wrapped.append(address.charAt(i));
		}
		return wrapped.toString();
	}

	private static String formatSol(long lamports) {
		long whole = lamports / 1_000_000_000L;
		int frac = (int) (lamports % 1_000_000_000L);
		if (frac == 0) return Long.toString(whole);
		String digits = String.format(Locale.US, "%09d", frac);
		int end = digits.length();
		while (end > 0 && digits.charAt(end - 1) == '0') end--;
		return whole + "." + digits.substring(0, end);
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

	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade( TitleScene.class );
	}

	private static class SupporterMessage extends Component {

		NinePatch bg;
		RenderedTextBlock text;
		Image icon;

		@Override
		protected void createChildren() {
			bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			add(bg);

			String message = Messages.get(SupporterScene.class, "intro");
			message += "\n\n" + Messages.get(SupporterScene.class, "patreon_msg");
			if (Messages.lang() != Languages.ENGLISH) {
				message += "\n" + Messages.get(SupporterScene.class, "patreon_english");
			}
			message += "\n\n- Evan";

			text = PixelScene.renderTextBlock(message, 6);
			add(text);

			icon = Icons.get(Icons.SHPX);
			add(icon);

		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;

			text.maxWidth((int)width - bg.marginHor());
			text.setPos(x + bg.marginLeft(), y + bg.marginTop() + 1);

			icon.y = text.bottom() - icon.height() + 4;
			icon.x = x + 25;

			height = (text.bottom() + 3) - y;

			height += bg.marginBottom();

			bg.size(width, height);

		}

	}

}
