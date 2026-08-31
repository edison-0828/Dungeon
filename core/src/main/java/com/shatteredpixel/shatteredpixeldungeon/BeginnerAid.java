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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

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
		if (hero != null && isNovice()) {
			Tracker tracker = Buff.affect(hero, Tracker.class);
			tracker.starterRewardPending = true;
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
		Tracker tracker = tracker();
		return tracker != null && tracker.starterRewardPending;
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
		Tracker tracker = tracker();
		Hero hero = Dungeon.hero;
		if (tracker == null || !tracker.starterRewardPending || hero == null) return;
		tracker.starterRewardPending = false;

		Item starterWeapon = starterWeapon(hero);
		if (selection == 0 && starterWeapon != null) {
			starterWeapon.upgrade();
			starterWeapon.updateQuickslot();
		} else if (selection == 1 && hero.belongings.armor != null) {
			hero.belongings.armor.upgrade();
			hero.belongings.armor.updateQuickslot();
		} else {
			Item supply = starterSupply(hero);
			if (!supply.collect() && Dungeon.level != null) {
				Dungeon.level.drop(supply, hero.pos).sprite.drop();
			}
		}

		if (hero.sprite != null) hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(BeginnerAid.class, "rewarded"));
		GLog.p(Messages.get(BeginnerAid.class, "rewarded_log"));
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
}
