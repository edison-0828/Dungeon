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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class PocketLantern extends Item {

	public static final String AC_LIGHT = "LIGHT";
	public static final String AC_SNUFF = "SNUFF";
	public static final String AC_REFILL = "REFILL";

	public static final int FUEL_PER_TORCH = 200;
	public static final int MAX_FUEL = 400;
	private static final float TIME_TO_TOGGLE = 1f;

	private static final ItemSprite.Glowing LIT_GLOW = new ItemSprite.Glowing(0xFFAA33);

	{
		image = ItemSpriteSheet.TORCH;

		defaultAction = AC_LIGHT;

		unique = true;
		bones = false;
	}

	private int fuel = FUEL_PER_TORCH;
	private boolean lit = false;

	public static PocketLantern get() {
		if (Dungeon.hero != null) {
			return Dungeon.hero.belongings.getItem(PocketLantern.class);
		}
		return null;
	}

	public int fuel() {
		return fuel;
	}

	public boolean lit() {
		return lit;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (lit) {
			actions.add(AC_SNUFF);
		} else if (fuel > 0) {
			actions.add(AC_LIGHT);
		}
		if (fuel < MAX_FUEL) {
			actions.add(AC_REFILL);
		}
		return actions;
	}

	@Override
	public String defaultAction() {
		if (lit) {
			return AC_SNUFF;
		} else if (fuel > 0) {
			return AC_LIGHT;
		} else {
			return AC_REFILL;
		}
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_LIGHT)) {
			light(hero);
		} else if (action.equals(AC_SNUFF)) {
			snuff(hero, true);
		} else if (action.equals(AC_REFILL)) {
			curUser = hero;
			GameScene.selectItem(itemSelector);
		}
	}

	private void light(Hero hero) {
		if (fuel <= 0) {
			GLog.w(Messages.get(this, "empty"));
			return;
		}
		if (lit) {
			return;
		}

		lit = true;
		Buff.affect(hero, Shine.class);
		Catalog.countUse(getClass());

		hero.spend(TIME_TO_TOGGLE);
		hero.busy();
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		Emitter emitter = hero.sprite.centerEmitter();
		emitter.start(FlameParticle.FACTORY, 0.2f, 3);
		updateQuickslot();
	}

	public void snuff(Hero hero, boolean spendTime) {
		if (!lit) {
			return;
		}
		lit = false;
		Shine shine = hero.buff(Shine.class);
		if (shine != null) {
			shine.detach();
		}
		if (spendTime) {
			hero.spend(TIME_TO_TOGGLE);
			hero.busy();
			hero.sprite.operate(hero.pos);
		}
		updateQuickslot();
	}

	private void refill(Torch torch) {
		if (fuel >= MAX_FUEL) {
			GLog.w(Messages.get(this, "full"));
			return;
		}
		int added = Math.min(FUEL_PER_TORCH, MAX_FUEL - fuel);
		torch.detach(curUser.belongings.backpack);
		fuel += added;
		GLog.i(Messages.get(this, "refilled", added));
		updateQuickslot();
	}

	@Override
	public void doDrop(Hero hero) {
		snuff(hero, false);
		super.doDrop(hero);
	}

	@Override
	public String info() {
		String info = infoBody();
		info += "\n\n" + Messages.get(this, "desc_fuel", fuel, MAX_FUEL);
		if (lit) {
			info += " " + Messages.get(this, "desc_lit");
		}
		return appendGuide(info);
	}

	@Override
	public String status() {
		return Integer.toString(fuel);
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return lit ? LIT_GLOW : null;
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

	private static final String FUEL = "fuel";
	private static final String LIT = "lit";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(FUEL, fuel);
		bundle.put(LIT, lit);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		fuel = bundle.getInt(FUEL);
		lit = bundle.getBoolean(LIT);
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(PocketLantern.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Torch;
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof Torch) {
				refill((Torch) item);
			}
		}
	};

	public static class Shine extends Buff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public boolean attachTo(Char target) {
			if (super.attachTo(target)) {
				if (Dungeon.level != null) {
					target.viewDistance = Math.max(Dungeon.level.viewDistance, Light.DISTANCE);
					Dungeon.observe();
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean act() {
			PocketLantern lantern = PocketLantern.get();
			if (lantern == null || !lantern.lit || lantern.fuel <= 0) {
				if (lantern != null) {
					lantern.lit = false;
					lantern.updateQuickslot();
				}
				detach();
				return true;
			}

			lantern.fuel--;
			lantern.updateQuickslot();
			if (lantern.fuel <= 0) {
				lantern.lit = false;
				GLog.w(Messages.get(PocketLantern.class, "burnt_out"));
				lantern.updateQuickslot();
				detach();
				return true;
			}

			spend(TICK);
			return true;
		}

		@Override
		public void detach() {
			if (target.buff(Light.class) == null) {
				target.viewDistance = Dungeon.level.viewDistance;
			} else {
				target.viewDistance = Math.max(Dungeon.level.viewDistance, Light.DISTANCE);
			}
			Dungeon.observe();
			super.detach();
		}

		@Override
		public int icon() {
			return BuffIndicator.LIGHT;
		}

		@Override
		public float iconFadePercent() {
			PocketLantern lantern = PocketLantern.get();
			if (lantern == null || lantern.fuel <= 0) {
				return 1;
			}
			return Math.max(0, (MAX_FUEL - lantern.fuel) / (float) MAX_FUEL);
		}

		@Override
		public String iconTextDisplay() {
			PocketLantern lantern = PocketLantern.get();
			return lantern == null ? "" : Integer.toString(lantern.fuel);
		}

		@Override
		public String desc() {
			PocketLantern lantern = PocketLantern.get();
			int left = lantern == null ? 0 : lantern.fuel;
			return Messages.get(this, "desc", left);
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.ILLUMINATED);
			else if (target.buff(Light.class) == null) {
				target.sprite.remove(CharSprite.State.ILLUMINATED);
			}
		}
	}
}
