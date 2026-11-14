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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;

public class HungerAdButton extends StyledButton {

	private boolean busy;

	public HungerAdButton() {
		super(Chrome.Type.TOAST_TR, Messages.get(HungerAdButton.class, "label"), 6);
		visible = false;
		active = false;
	}

	public void place(float top) {
		boolean show = showing();
		visible = show;
		active = show && !busy;
		if (!show) return;
		text(Messages.get(this, "label", SPDSettings.adFoodLeft()));
		float width = Math.min(reqWidth() + 8, camera.width - 8);
		float height = Math.max(16, reqHeight());
		setRect((camera.width - width) / 2f, top, width, height);
	}

	private boolean showing() {
		if (busy) return true;
		if (Dungeon.hero == null || !Dungeon.hero.isAlive() || Game.platform == null) return false;
		if (!Game.platform.supportsRewardedAds() || SPDSettings.adFoodLeft() <= 0) return false;
		Hunger hunger = Dungeon.hero.buff(Hunger.class);
		return hunger != null && hunger.isHungry();
	}

	@Override
	protected void onClick() {
		if (busy || Dungeon.hero == null || Game.platform == null) return;
		busy = true;
		enable(false);
		Game.platform.showRewardedAd(new PlatformSupport.RewardedAdListener() {
			@Override
			public void onResult(int kind) {
				busy = false;
				enable(true);
				if (Dungeon.hero == null) return;
				if (kind == PlatformSupport.RewardedAdListener.REWARDED) {
					if (SPDSettings.adFoodLeft() > 0) grant();
					else GLog.w(Messages.get(HungerAdButton.class, "limit"));
				} else if (kind == PlatformSupport.RewardedAdListener.CLOSED) {
					GLog.w(Messages.get(HungerAdButton.class, "closed"));
				} else {
					GLog.w(Messages.get(HungerAdButton.class, "unavailable"));
				}
			}
		});
	}

	private void grant() {
		if (!SPDSettings.takeAdFood()) {
			GLog.w(Messages.get(HungerAdButton.class, "limit"));
			return;
		}
		Food food = new Food();
		food.quantity(5);
		if (food.collect(Dungeon.hero.belongings.backpack)) {
			GLog.p(Messages.get(HungerAdButton.class, "reward", food.quantity()));
		} else {
			Dungeon.level.drop(food, Dungeon.hero.pos).sprite.drop();
			GLog.w(Messages.get(HungerAdButton.class, "dropped", food.quantity()));
		}
	}
}
