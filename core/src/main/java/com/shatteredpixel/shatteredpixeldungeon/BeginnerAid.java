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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PetAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EscapeDust;
import com.shatteredpixel.shatteredpixeldungeon.items.FieldWrap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.items.PocketLantern;
import com.shatteredpixel.shatteredpixeldungeon.items.SnackPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.SparePocket;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.util.HashSet;

/**
 * A first-clear safety net which rewards good play instead of flattening the whole game's difficulty.
 * The tracker is a hero buff so every one-shot reward survives saving and loading.
 */
public final class BeginnerAid {

	private BeginnerAid() {}

	private static final HashSet<String> shown = new HashSet<>();

	/** Slightly softer enemy hits so early mistakes are survivable. */
	public static final float ENEMY_DAMAGE = 0.85f;
	/** Enemy accuracy roll vs the hero. */
	public static final float ENEMY_ACCURACY = 0.85f;
	public static final int SAFETY_SHIELD = 8;
	public static final int DOORWAY_SHIELD = 3;
	public static final float FIRST_SURPRISE_DAMAGE = 1.5f;

	public static void resetForRun() {
		shown.clear();
	}

	public static void beginRun(Hero hero) {
		if (hero == null) return;
		Buff.affect(hero, PetChoice.class).ensureOffers();
		if (isNovice()) {
			Buff.affect(hero, Tracker.class);
		}
	}

	public static boolean isActive() {
		return Dungeon.hero != null
				&& Dungeon.challenges == 0
				&& Dungeon.hero.buff(Tracker.class) != null;
	}

	public static int scaleEnemyDamage(int dmg, Object src) {
		if (!isActive() || dmg <= 0 || !isHostileDamage(src)) return dmg;
		return Math.max(1, Math.round(dmg * ENEMY_DAMAGE));
	}

	public static boolean isHostileDamage(Object src) {
		if (src instanceof Char) {
			return ((Char) src).alignment == Char.Alignment.ENEMY;
		}
		if (src == null) return false;
		Class<?> enclosing = src.getClass().getEnclosingClass();
		while (enclosing != null) {
			if (Mob.class.isAssignableFrom(enclosing)) return true;
			enclosing = enclosing.getEnclosingClass();
		}
		return false;
	}

	public static float enemyAccuracyFactor(Char attacker, Char defender) {
		if (isActive()
				&& defender instanceof Hero
				&& attacker != null
				&& attacker.alignment == Char.Alignment.ENEMY) {
			return ENEMY_ACCURACY;
		}
		return 1f;
	}

	/** Sewers: first five floors, before the first boss, and only for a tracked novice run. */
	public static boolean isEarlyGame() {
		return isActive() && Dungeon.depth >= 1 && Dungeon.depth <= 5;
	}

	/** Faster levels until 6, so the first hour feels like growth instead of a grind. */
	public static float expFactor() {
		if (isActive() && Dungeon.hero.lvl < 6) return 1.5f;
		return 1f;
	}

	/** Hunger ticks more slowly in the sewers. */
	public static float hungerDelayFactor() {
		return isEarlyGame() ? 1.75f : 1f;
	}

	/** First region does not spawn cursed weapons or armor. */
	public static boolean skipSpawnCurses() {
		return isEarlyGame();
	}

	public static boolean starterRewardPending() {
		return Dungeon.hero != null && Dungeon.hero.buff(PetChoice.class) != null;
	}

	public static PetOffer[] starterPets() {
		PetChoice choice = Dungeon.hero == null ? null : Dungeon.hero.buff(PetChoice.class);
		if (choice == null) return new PetOffer[0];
		choice.ensureOffers();
		return choice.offers;
	}

	public static Item starterSupply(Hero hero) {
		if (hero == null || hero.heroClass == null) return new PotionOfHealing().identify();
		if (hero.heroClass == HeroClass.MAGE)     return new ScrollOfRecharging().identify();
		if (hero.heroClass == HeroClass.ROGUE)    return new PotionOfInvisibility().identify();
		if (hero.heroClass == HeroClass.HUNTRESS) return new PotionOfHaste().identify();
		if (hero.heroClass == HeroClass.DUELIST)  return new PotionOfStrength().identify();
		if (hero.heroClass == HeroClass.CLERIC)   return new ScrollOfRemoveCurse().identify();
		return new PotionOfHealing().identify();
	}

	public static Item starterWeapon(Hero hero) {
		if (hero == null) return null;
		SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
		return bow != null ? bow : hero.belongings.weapon;
	}

	public static void claimStarterReward(int selection) {
		Hero hero = Dungeon.hero;
		PetChoice choice = hero == null ? null : hero.buff(PetChoice.class);
		if (choice == null) return;
		choice.ensureOffers();
		if (selection < 0 || selection >= choice.offers.length) {
			selection = 0;
		}
		PetOffer offer = choice.offers[selection];
		choice.detach();

		PetWhistle whistle = new PetWhistle();
		whistle.bind(offer.quality, offer.appearance);
		if (!whistle.collect() && Dungeon.level != null) {
			Dungeon.level.drop(whistle, hero.pos).sprite.drop();
		}
		Dungeon.LimitedDrops.PET_WHISTLE.drop();

		if (hero.sprite != null) {
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(BeginnerAid.class, "rewarded"));
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					whistle.tryAutoRevive();
				}
			});
		}
		GLog.p(Messages.get(BeginnerAid.class, "rewarded_log",
				offer.quality.title(), offer.appearance.title(), offer.quality.bonusPercent(),
				PetBond.bonusText(offer.appearance, offer.quality)));
	}

	public static void trySafetyNet(Hero hero) {
		Tracker tracker = tracker();
		if (tracker == null || tracker.safetyNetUsed || hero == null || hero.HP <= 0 || hero.HP * 4 > hero.HT) return;
		tracker.safetyNetUsed = true;
		Barrier barrier = Buff.affect(hero, Barrier.class);
		barrier.setShield(Math.max(barrier.shielding(), SAFETY_SHIELD));
		if (hero.sprite != null) {
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(SAFETY_SHIELD), FloatingText.SHIELDING);
		}
		GLog.p(Messages.get(BeginnerAid.class, "safety_net"));
	}

	public static float modifyHeroAttackDamage(Char attacker, Char defender, float damage, boolean surpriseAttack) {
		Tracker tracker = tracker();
		if (tracker == null || tracker.surpriseRewardUsed || attacker != Dungeon.hero
				|| defender == null || defender.alignment != Char.Alignment.ENEMY || !surpriseAttack) return damage;
		tracker.surpriseRewardUsed = true;
		if (defender.sprite != null) defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(BeginnerAid.class, "perfect_surprise"));
		GLog.p(Messages.get(BeginnerAid.class, "surprise_reward"));
		return damage * FIRST_SURPRISE_DAMAGE;
	}

	public static boolean isDoorwayFight(Char attacker, Char defender) {
		if (Dungeon.level == null || attacker == null || defender == null) return false;
		int a = Dungeon.level.map[attacker.pos];
		int d = Dungeon.level.map[defender.pos];
		return a == Terrain.DOOR || a == Terrain.OPEN_DOOR || d == Terrain.DOOR || d == Terrain.OPEN_DOOR;
	}

	public static void onHeroDefeatedEnemy(Char enemy, boolean doorwayFight) {
		Tracker tracker = tracker();
		if (tracker == null || tracker.doorwayRewardUsed || !doorwayFight || enemy == null) return;
		tracker.doorwayRewardUsed = true;
		Barrier barrier = Buff.affect(Dungeon.hero, Barrier.class);
		barrier.incShield(DOORWAY_SHIELD);
		if (Dungeon.hero.sprite != null) {
			Dungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(DOORWAY_SHIELD), FloatingText.SHIELDING);
		}
		GLog.p(Messages.get(BeginnerAid.class, "doorway_reward"));
	}

	public static void onLevelUp(Hero hero) {
		if (!isActive() || hero == null || hero.HP <= 0) return;
		int healing = Math.max(1, Math.round(hero.HT * 0.30f));
		hero.HP = Math.min(hero.HT, hero.HP + healing);
		if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);
		GLog.p(Messages.get(BeginnerAid.class, "level_heal"));
	}

	public static void onGooChargeDodged(Mob goo) {
		if (!isActive() || goo == null) return;
		Buff.prolong(goo, Vulnerable.class, 2f);
		if (goo.sprite != null) goo.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(BeginnerAid.class, "opening"));
		GLog.p(Messages.get(BeginnerAid.class, "goo_opening"));
	}

	public static void completeFirstClear() {
		Tracker tracker = tracker();
		if (tracker != null) tracker.detach();
		shown.clear();
	}

	public static boolean isNovice() {
		if (Dungeon.challenges != 0) return false;
		try {
			Badges.loadGlobal();
			return !Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_1);
		} catch (Exception e) {
			return false;
		}
	}

	public static void hint(String id) {
		if (!isActive() || !shown.add(id)) return;
		GLog.p(Messages.get(BeginnerAid.class, id));
	}

	public static void onItemCollected(Item item) {
		if (item != null && !item.isIdentified() && (item instanceof Weapon || item instanceof Armor)) {
			hint("unidentified");
		}
		if (item instanceof Scroll && !((Scroll) item).isKnown()) {
			hint("unidentified_scroll");
		}
		if (item instanceof Potion && !((Potion) item).isKnown()) {
			hint("unidentified_potion");
		}
		if (item instanceof Ring && !((Ring) item).isKnown()) {
			hint("unidentified_ring");
		}
		if (item instanceof Wand && !item.isIdentified()) {
			hint("unidentified_wand");
		}
		if (item instanceof MysteryMeat) {
			hint("mystery_meat");
		} else if (item instanceof FrozenCarpaccio) {
			hint("frozen_meat");
		} else if (item instanceof StewedMeat || item instanceof ChargrilledMeat) {
			hint("cooked_meat");
		} else if (item instanceof MeatPie) {
			hint("meat_pie");
		} else if (item instanceof Food) {
			hint("food");
		}
		if (item instanceof PetWhistle) {
			hint("pet_whistle");
		}
		if (item instanceof SnackPouch) {
			hint("snack_pouch");
		}
		if (item instanceof PocketLantern) {
			hint("pocket_lantern");
		}
		if (item instanceof SparePocket) {
			hint("spare_pocket");
		}
		if (item instanceof EscapeDust) {
			hint("escape_dust");
		}
		if (item instanceof FieldWrap) {
			hint("field_wrap");
		}
		if (item instanceof Dewdrop) {
			hint("dewdrop");
		}
		if (item instanceof Plant.Seed) {
			hint("seed");
		}
		if (item instanceof Runestone) {
			hint("runestone");
		}
		if (item instanceof Key) {
			hint("key");
		}
	}

	private static Tracker tracker() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(Tracker.class);
	}

	public static class Tracker extends Buff {
		public boolean starterRewardPending;
		public boolean safetyNetUsed;
		public boolean surpriseRewardUsed;
		public boolean doorwayRewardUsed;

		private static final String STARTER_REWARD = "starter_reward";
		private static final String SAFETY_NET = "safety_net";
		private static final String SURPRISE_REWARD = "surprise_reward";
		private static final String DOORWAY_REWARD = "doorway_reward";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(STARTER_REWARD, starterRewardPending);
			bundle.put(SAFETY_NET, safetyNetUsed);
			bundle.put(SURPRISE_REWARD, surpriseRewardUsed);
			bundle.put(DOORWAY_REWARD, doorwayRewardUsed);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			starterRewardPending = bundle.getBoolean(STARTER_REWARD);
			safetyNetUsed = bundle.getBoolean(SAFETY_NET);
			surpriseRewardUsed = bundle.getBoolean(SURPRISE_REWARD);
			doorwayRewardUsed = bundle.getBoolean(DOORWAY_REWARD);
		}
	}

	public static class PetOffer {
		public PetAlly.Quality quality;
		public PetAlly.Appearance appearance;
	}

	public static class PetChoice extends Buff {
		{
			revivePersists = true;
			type = buffType.NEUTRAL;
		}

		public PetOffer[] offers = new PetOffer[3];

		public void ensureOffers() {
			HashSet<PetAlly.Appearance> used = new HashSet<>();
			for (int i = 0; i < offers.length; i++) {
				if (offers[i] != null && offers[i].quality != null && offers[i].appearance != null) {
					used.add(offers[i].appearance);
					continue;
				}
				PetOffer offer = new PetOffer();
				offer.quality = PetAlly.Quality.roll();
				PetAlly.Appearance look;
				int tries = 0;
				do {
					look = PetAlly.Appearance.roll();
				} while (used.contains(look) && tries++ < 20);
				used.add(look);
				offer.appearance = look;
				offers[i] = offer;
			}
		}

		private static final String QUALITY = "pet_quality_";
		private static final String LOOK = "pet_look_";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			ensureOffers();
			for (int i = 0; i < offers.length; i++) {
				bundle.put(QUALITY + i, offers[i].quality.name());
				bundle.put(LOOK + i, offers[i].appearance.name());
			}
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			for (int i = 0; i < offers.length; i++) {
				PetOffer offer = new PetOffer();
				if (bundle.contains(QUALITY + i)) {
					try {
						offer.quality = PetAlly.Quality.valueOf(bundle.getString(QUALITY + i));
					} catch (Exception ignored) {
						offer.quality = PetAlly.Quality.roll();
					}
				}
				if (bundle.contains(LOOK + i)) {
					try {
						offer.appearance = PetAlly.Appearance.valueOf(bundle.getString(LOOK + i));
					} catch (Exception ignored) {
						offer.appearance = PetAlly.Appearance.roll();
					}
				}
				offers[i] = offer;
			}
			ensureOffers();
		}
	}

}
