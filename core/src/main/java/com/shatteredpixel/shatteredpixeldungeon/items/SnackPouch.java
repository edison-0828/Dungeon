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
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class SnackPouch extends Item {

	public static final String AC_EAT = "EAT";
	public static final String AC_STORE = "STORE";

	private static final int MAX_SERVINGS = 6;
	private static final float SNACK_ENERGY = Hunger.HUNGRY / 3f;
	private static final float TIME_TO_EAT = 1f;

	private static final String TXT_STATUS = "%d/%d";

	{
		image = ItemSpriteSheet.SUPPLY_RATION;

		defaultAction = AC_EAT;

		unique = true;
		bones = false;
	}

	private int servings = 2;

	public static boolean canStore(Item item) {
		return item instanceof Food
				&& !(item instanceof MysteryMeat)
				&& !(item instanceof Blandfruit);
	}

	public static int servingsFor(Food food) {
		return Math.max(1, Math.round(food.energy / SNACK_ENERGY));
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (servings > 0) {
			actions.add(AC_EAT);
		}
		if (servings < MAX_SERVINGS) {
			actions.add(AC_STORE);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_EAT)) {
			eat(hero);
		} else if (action.equals(AC_STORE)) {
			curUser = hero;
			GameScene.selectItem(itemSelector);
		}
	}

	private void eat(Hero hero) {
		if (servings <= 0) {
			GLog.w(Messages.get(this, "empty"));
			return;
		}

		servings--;
		Catalog.countUse(getClass());

		float energy = SNACK_ENERGY;
		if (Dungeon.isChallenged(Challenges.NO_FOOD)) {
			energy /= 3f;
		}
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) {
			hunger.satisfy(energy);
		}

		GLog.i(Messages.get(this, "eat_msg"));
		hero.sprite.operate(hero.pos);
		hero.busy();
		SpellSprite.show(hero, SpellSprite.FOOD);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		hero.spend(TIME_TO_EAT);

		Statistics.foodEaten++;
		updateQuickslot();
	}

	private void store(Food food) {
		int gained = servingsFor(food);
		int space = MAX_SERVINGS - servings;
		if (space <= 0) {
			GLog.w(Messages.get(this, "full"));
			return;
		}
		if (gained > space) {
			GLog.w(Messages.get(this, "no_space", gained, space));
			return;
		}

		food.detach(curUser.belongings.backpack);
		servings += gained;
		GLog.i(Messages.get(this, "stored", gained));
		updateQuickslot();
	}

	@Override
	public String info() {
		String info = infoBody();
		info += "\n\n" + Messages.get(this, "desc_servings", servings, MAX_SERVINGS);
		return appendGuide(info);
	}

	@Override
	public String status() {
		return Messages.format(TXT_STATUS, servings, MAX_SERVINGS);
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
		return 0;
	}

	private static final String SERVINGS = "servings";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SERVINGS, servings);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		servings = bundle.getInt(SERVINGS);
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(SnackPouch.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return canStore(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof Food) {
				store((Food) item);
			}
		}
	};
}
