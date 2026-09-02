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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PetAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndPetReplace;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PetWhistle extends Item {

	public static final String AC_SUMMON = "SUMMON";
	public static final String AC_DISMISS = "DISMISS";
	public static final String AC_DIRECT = "DIRECT";

	/** One dungeon turn is the game's second; companions return after a minute. */
	public static final float REVIVE_TURNS = 60f;

	{
		image = ItemSpriteSheet.BEACON;

		defaultAction = AC_SUMMON;

		unique = true;
		bones = false;
	}

	private PetAlly pet = null;
	private int petID = 0;
	private int storedHP = -1;
	private int storedHT = -1;
	private PetAlly.Quality quality = null;
	private PetAlly.Appearance appearance = null;

	static boolean suppressAutoRevive = false;

	public void ensureIdentity() {
		if (quality == null) {
			quality = PetAlly.Quality.roll();
		}
		if (appearance == null) {
			appearance = PetAlly.Appearance.roll();
		}
	}

	public void bind(PetAlly.Quality quality, PetAlly.Appearance appearance) {
		this.quality = quality;
		this.appearance = appearance;
		ensureIdentity();
		storedHP = -1;
		storedHT = -1;
		updateQuickslot();
		if (Dungeon.hero != null) {
			PetBond.refresh(Dungeon.hero, this);
		}
	}

	public PetAlly.Quality quality() {
		ensureIdentity();
		return quality;
	}

	public PetAlly.Appearance appearance() {
		ensureIdentity();
		return appearance;
	}

	public static boolean isReplacePrompt(Item item) {
		if (!(item instanceof PetWhistle) || Dungeon.hero == null) {
			return false;
		}
		PetWhistle existing = Dungeon.hero.belongings.getItem(PetWhistle.class);
		return existing != null && existing != item;
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		PetWhistle existing = hero.belongings.getItem(PetWhistle.class);
		if (existing != null && existing != this) {
			ensureIdentity();
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndPetReplace(existing, PetWhistle.this));
				}
			});
			return false;
		}
		ensureIdentity();
		boolean picked = super.doPickUp(hero, pos);
		if (picked) {
			GLog.p(Messages.get(this, "bound", quality.title(), appearance.title()));
			PetBond.refresh(hero, this);
		}
		return picked;
	}

	public void replaceWith(PetWhistle candidate) {
		if (candidate == null) return;
		candidate.ensureIdentity();
		PetAlly ally = pet();
		suppressAutoRevive = true;
		try {
			if (ally != null) {
				ally.dismiss();
				pet = null;
				petID = 0;
			}
			ReviveCooldown cd = Dungeon.hero == null ? null : Dungeon.hero.buff(ReviveCooldown.class);
			if (cd != null) {
				cd.detach();
			}
		} finally {
			suppressAutoRevive = false;
		}
		bind(candidate.quality(), candidate.appearance());
		tryAutoRevive();
		GLog.p(Messages.get(this, "replaced", quality.title(), appearance.title()));
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (pet() != null) {
			actions.add(AC_DIRECT);
			actions.add(AC_DISMISS);
		} else {
			actions.add(AC_SUMMON);
		}
		return actions;
	}

	@Override
	public String defaultAction() {
		return pet() != null ? AC_DIRECT : AC_SUMMON;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_SUMMON)) {
			summon(hero, false);
		} else if (action.equals(AC_DISMISS)) {
			dismiss(hero);
		} else if (action.equals(AC_DIRECT)) {
			if (pet() != null) {
				GameScene.selectCell(director);
			}
		}
	}

	public boolean summon(Hero hero, boolean automatic) {
		if (pet() != null) {
			if (!automatic) GLog.i(Messages.get(this, "spawned"));
			return true;
		}
		if (hero.buff(ReviveCooldown.class) != null) {
			if (!automatic) {
				GLog.w(Messages.get(this, "cooldown", (int)hero.buff(ReviveCooldown.class).visualcooldown()));
			}
			return false;
		}

		ArrayList<Integer> spawnPoints = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			int p = hero.pos + i;
			if (Actor.findChar(p) == null && Dungeon.level.passable[p]) {
				spawnPoints.add(p);
			}
		}

		if (spawnPoints.isEmpty()) {
			if (!automatic) GLog.i(Messages.get(this, "no_space"));
			return false;
		}

		pet = new PetAlly();
		pet.setIdentity(quality(), appearance());
		if (storedHP >= 0) {
			pet.applyStoredHealth(storedHP, storedHT);
		}
		petID = pet.id();
		pet.pos = Random.element(spawnPoints);

		if (hero.sprite != null) {
			GameScene.add(pet, 1f);
			if (pet.sprite != null) {
				pet.sprite.emitter().burst(Speck.factory(Speck.STEAM), 5);
			}
			Sample.INSTANCE.play(Assets.Sounds.BEACON);
			if (!automatic) {
				hero.spend(1f);
				hero.busy();
				hero.sprite.operate(hero.pos);
				Invisibility.dispel(hero);
			}
		} else {
			Actor.add(pet);
		}
		Dungeon.level.occupyCell(pet);
		updateQuickslot();
		GLog.p(Messages.get(this, automatic ? "revived" : "summoned", quality().title(), appearance().title()));
		return true;
	}

	public void tryAutoRevive() {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || Dungeon.level == null) {
			return;
		}
		if (pet() != null) {
			return;
		}
		if (!summon(hero, true)) {
			Buff.prolong(hero, ReviveCooldown.class, 5f);
		}
		PetBond.refresh(hero, this);
	}

	private void dismiss(Hero hero) {
		PetAlly ally = pet();
		if (ally == null) {
			GLog.i(Messages.get(this, "not_spawned"));
			return;
		}

		storedHP = ally.HP;
		storedHT = ally.HT;
		ally.dismiss();
		pet = null;
		petID = 0;

		Sample.INSTANCE.play(Assets.Sounds.PUFF);
		hero.spend(1f);
		hero.busy();
		hero.sprite.operate(hero.pos);
		updateQuickslot();
		GLog.i(Messages.get(this, "dismissed"));
	}

	public void linkPet(PetAlly ally) {
		ensureIdentity();
		ally.setIdentity(quality, appearance);
		pet = ally;
		petID = ally.id();
	}

	public void onPetRemoved(PetAlly ally, boolean died) {
		if (pet != ally && petID != ally.id()) {
			return;
		}
		if (died) {
			storedHP = -1;
			storedHT = -1;
			if (Dungeon.hero != null) {
				Buff.prolong(Dungeon.hero, ReviveCooldown.class, REVIVE_TURNS);
				GLog.w(Messages.get(this, "died", (int)REVIVE_TURNS));
			}
		}
		pet = null;
		petID = 0;
		updateQuickslot();
		PetBond.refresh(Dungeon.hero);
	}

	public PetAlly pet() {
		if (pet == null && petID != 0) {
			Actor a = Actor.findById(petID);
			if (a instanceof PetAlly) {
				pet = (PetAlly) a;
				if (quality != null && appearance != null) {
					pet.setIdentity(quality, appearance);
				}
			} else {
				petID = 0;
			}
		}
		return pet != null && pet.isAlive() ? pet : null;
	}

	public static PetWhistle get() {
		if (Dungeon.hero != null) {
			return Dungeon.hero.belongings.getItem(PetWhistle.class);
		}
		return null;
	}

	@Override
	public String status() {
		PetAlly ally = pet();
		if (ally != null) {
			return ((ally.HP * 100) / ally.HT) + "%";
		}
		if (Dungeon.hero != null) {
			ReviveCooldown cd = Dungeon.hero.buff(ReviveCooldown.class);
			if (cd != null) {
				return Integer.toString((int) cd.visualcooldown());
			}
		}
		return super.status();
	}

	@Override
	public String name() {
		if (quality == null || appearance == null) {
			return super.name();
		}
		return Messages.get(this, "name_bound", quality.title(), appearance.title());
	}

	@Override
	public String desc() {
		ensureIdentity();
		return Messages.get(this, "desc",
				quality.title(),
				appearance.title(),
				quality.bonusPercent(),
				PetBond.bonusText(appearance, quality));
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return quality == null ? null : quality.glow;
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

	private static final String PET_ID = "pet_id";
	private static final String STORED_HP = "stored_hp";
	private static final String STORED_HT = "stored_ht";
	private static final String QUALITY = "pet_quality";
	private static final String APPEARANCE = "pet_appearance";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		ensureIdentity();
		bundle.put(PET_ID, petID);
		bundle.put(STORED_HP, storedHP);
		bundle.put(STORED_HT, storedHT);
		bundle.put(QUALITY, quality.name());
		bundle.put(APPEARANCE, appearance.name());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		petID = bundle.getInt(PET_ID);
		storedHP = bundle.getInt(STORED_HP);
		storedHT = bundle.getInt(STORED_HT);
		if (bundle.contains(QUALITY)) {
			try {
				quality = PetAlly.Quality.valueOf(bundle.getString(QUALITY));
			} catch (Exception ignored) {
				quality = null;
			}
		}
		if (bundle.contains(APPEARANCE)) {
			try {
				appearance = PetAlly.Appearance.valueOf(bundle.getString(APPEARANCE));
			} catch (Exception ignored) {
				appearance = null;
			}
		}
		ensureIdentity();
	}

	public CellSelector.Listener director = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			PetAlly ally = pet();
			if (ally != null) {
				Sample.INSTANCE.play(Assets.Sounds.CLICK);
				ally.directTocell(cell);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(PetAlly.class, "direct_prompt");
		}
	};

	public static class ReviveCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}

		@Override
		public void detach() {
			super.detach();
			if (!suppressAutoRevive && Dungeon.hero != null && Dungeon.hero.isAlive()) {
				PetWhistle whistle = PetWhistle.get();
				if (whistle != null) {
					whistle.tryAutoRevive();
				}
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (REVIVE_TURNS - visualcooldown()) / REVIVE_TURNS);
		}

		@Override
		public void tintIcon(com.watabou.noosa.Image icon) {
			icon.hardlight(0.7f, 0.5f, 0.2f);
		}
	}
}
