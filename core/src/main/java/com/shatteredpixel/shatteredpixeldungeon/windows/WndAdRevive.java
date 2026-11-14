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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;

public class WndAdRevive extends Window {

	private static final int WIDTH = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP = 2;

	private final Object cause;
	private final RedButton btnWatch;
	private boolean busy;
	private boolean closed;

	public WndAdRevive(Object cause) {
		super();
		this.cause = cause;
		WndResurrect.instance = this;

		RenderedTextBlock message = PixelScene.renderTextBlock(Messages.get(this, "message"), 6);
		message.maxWidth(WIDTH);
		message.setPos(0, 0);
		add(message);

		btnWatch = new RedButton(Messages.get(this, "watch")) {
			@Override
			protected void onClick() {
				watch();
			}
		};
		btnWatch.setRect(0, message.bottom() + GAP, WIDTH, BTN_HEIGHT);
		add(btnWatch);

		RedButton btnGiveUp = new RedButton(Messages.get(this, "giveup")) {
			@Override
			protected void onClick() {
				giveUp();
			}
		};
		btnGiveUp.setRect(0, btnWatch.bottom() + GAP, WIDTH, BTN_HEIGHT);
		add(btnGiveUp);

		resize(WIDTH, (int)btnGiveUp.bottom());
	}

	private void watch() {
		if (busy || closed || Dungeon.hero == null) return;
		if (Dungeon.hero.adReviveUsed) {
			giveUp();
			return;
		}
		busy = true;
		btnWatch.enable(false);
		Game.platform.showRewardedAd(new PlatformSupport.RewardedAdListener() {
			@Override
			public void onResult(int kind) {
				busy = false;
				if (btnWatch.parent != null) btnWatch.enable(true);
				if (closed || Dungeon.hero == null) return;
				if (kind == PlatformSupport.RewardedAdListener.REWARDED) {
					if (Dungeon.hero.adReviveUsed) {
						giveUp();
						return;
					}
					closed = true;
					Dungeon.hero.reviveFromAd();
					hide();
				} else if (kind == PlatformSupport.RewardedAdListener.CLOSED) {
					GLog.w(Messages.get(WndAdRevive.class, "closed"));
				} else {
					GLog.w(Messages.get(WndAdRevive.class, "unavailable"));
				}
			}
		});
	}

	private void giveUp() {
		if (closed) return;
		closed = true;
		hide();
		if (Dungeon.hero != null) {
			Dungeon.hero.finishDeath(cause);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (WndResurrect.instance == this) {
			WndResurrect.instance = null;
		}
	}

	@Override
	public void onBackPressed() {
	}
}
