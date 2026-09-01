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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class FieldWrap extends Item {

	public static final String AC_USE = "USE";
	public static final int INSTANT_HEAL = 8;
	public static final int REGEN_HEAL = 8;
	private static final float TIME_TO_USE = 1f;

	{
		image = ItemSpriteSheet.CLOAK_SCRAP;

		stackable = true;
		defaultAction = AC_USE;
		bones = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_USE)) {
			detach(hero.belongings.backpack);
			Catalog.countUse(getClass());

			int instant = Math.min(INSTANT_HEAL, hero.HT - hero.HP);
			if (instant > 0) {
				hero.HP += instant;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(instant), FloatingText.HEALING);
			}

			Buff.affect(hero, Bandage.class).set(REGEN_HEAL);

			GLog.i(Messages.get(this, "used"));
			Sample.INSTANCE.play(Assets.Sounds.DEWDROP);

			hero.spend(TIME_TO_USE);
			hero.busy();
			hero.sprite.operate(hero.pos);
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 12 * quantity;
	}

	public static class Bandage extends Buff {

		{
			type = buffType.POSITIVE;
			actPriority = HERO_PRIO - 1;
		}

		private int left;

		public void set(int amount) {
			left = Math.max(left, amount);
		}

		@Override
		public boolean act() {
			if (target.HP < target.HT && left > 0) {
				target.HP++;
				left--;
				target.sprite.showStatusWithIcon(CharSprite.POSITIVE, "1", FloatingText.HEALING);
			} else {
				left--;
			}

			if (left <= 0) {
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEALING;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", left);
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
		}
	}
}
