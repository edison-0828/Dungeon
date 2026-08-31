/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.BeginnerAid;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arithmetic behind every fight.
 *
 * <p>Balance numbers are edited by hand, one weapon at a time, and a slip in any of them is invisible
 * until a player notices a dagger out-damaging a greatsword. These tests do not re-state the tuning —
 * that would just be the same numbers typed twice — they assert the shape every entry has to have: a
 * damage range that is the right way round, curves that never bend downwards as you upgrade, and a
 * strength requirement that eases off on the schedule the code documents.
 */
public class CombatFormulaTest {

	/** As high as any item can be upgraded in practice, with room to spare. */
	private static final int MAX_UPGRADE = 30;

	@BeforeAll
	public static void boot() {
		HeadlessDungeon.startRun(0xC0FFEEL);
	}

	// ------------------------------------------------------------- the roster

	private static Stream<Weapon> allWeapons() {
		List<Weapon> weapons = new ArrayList<>();
		for (Generator.Category category : Generator.Category.values()) {
			if (!Weapon.class.isAssignableFrom(category.superClass)) continue;
			for (Class<?> cls : category.classes) {
				Weapon weapon = (Weapon) Reflection.newInstance(cls);
				assertNotNull(weapon, cls.getSimpleName() + " could not be instantiated");
				weapons.add(weapon);
			}
		}
		assertTrue(weapons.size() > 20, "only found " + weapons.size() + " weapons, the roster is bigger");
		return weapons.stream();
	}

	private static Stream<Armor> allArmour() {
		List<Armor> armour = new ArrayList<>();
		for (Class<?> cls : Generator.Category.ARMOR.classes) {
			Armor piece = (Armor) Reflection.newInstance(cls);
			assertNotNull(piece, cls.getSimpleName() + " could not be instantiated");
			armour.add(piece);
		}
		return armour.stream();
	}

	private static String name(Object item) {
		return item.getClass().getSimpleName();
	}

	// ------------------------------------------------------------- weapon damage

	@ParameterizedTest(name = "{0}")
	@MethodSource("allWeapons")
	@DisplayName("a weapon's damage range is the right way round at every upgrade level")
	public void damageRangesAreOrdered(Weapon weapon) {
		for (int lvl = 0; lvl <= MAX_UPGRADE; lvl++) {
			int min = weapon.min(lvl);
			int max = weapon.max(lvl);
			//Random.NormalIntRange silently returns the lower bound when handed an inverted range, so an
			//inverted weapon would quietly deal its minimum forever rather than crashing
			assertTrue(min <= max, name(weapon) + " at +" + lvl
					+ " rolls damage in [" + min + ", " + max + "], which is inverted");
			assertTrue(min >= 0, name(weapon) + " at +" + lvl + " has a negative minimum of " + min);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allWeapons")
	public void upgradingAWeaponNeverMakesItWorse(Weapon weapon) {
		for (int lvl = 1; lvl <= MAX_UPGRADE; lvl++) {
			assertTrue(weapon.min(lvl) >= weapon.min(lvl - 1), name(weapon)
					+ " loses minimum damage going from +" + (lvl - 1) + " to +" + lvl);
			assertTrue(weapon.max(lvl) >= weapon.max(lvl - 1), name(weapon)
					+ " loses maximum damage going from +" + (lvl - 1) + " to +" + lvl);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allWeapons")
	public void upgradingAWeaponActuallyDoesSomething(Weapon weapon) {
		assertTrue(weapon.max(MAX_UPGRADE) > weapon.max(0), name(weapon)
				+ " deals the same maximum damage at +" + MAX_UPGRADE + " as it does unupgraded");
	}

	// -------------------------------------------------------- armour absorption

	@ParameterizedTest(name = "{0}")
	@MethodSource("allArmour")
	@DisplayName("armour absorbs a sane range at every upgrade level")
	public void absorptionRangesAreOrdered(Armor armour) {
		for (int lvl = 0; lvl <= MAX_UPGRADE; lvl++) {
			int min = armour.DRMin(lvl);
			int max = armour.DRMax(lvl);
			assertTrue(min >= 0, name(armour) + " at +" + lvl + " absorbs a negative minimum of " + min);
			assertTrue(min <= max, name(armour) + " at +" + lvl
					+ " absorbs [" + min + ", " + max + "], which is inverted");
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allArmour")
	public void upgradingArmourNeverMakesItWorse(Armor armour) {
		for (int lvl = 1; lvl <= MAX_UPGRADE; lvl++) {
			assertTrue(armour.DRMax(lvl) >= armour.DRMax(lvl - 1), name(armour)
					+ " absorbs less at +" + lvl + " than at +" + (lvl - 1));
		}
	}

	// ------------------------------------------------------------ strength reqs

	/**
	 * {@code Armor.STRReq} documents that "strength req decreases at +1,+3,+6,+10,etc." — the
	 * triangular numbers. It gets there by way of {@code (int)(Math.sqrt(8*lvl + 1) - 1)/2}, where an
	 * off-by-one or a misplaced cast would shift every threshold without changing the shape of the
	 * curve, so this pins the schedule rather than the expression.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource({"allWeapons", "allArmour"})
	public void strengthRequirementEasesOffOnTheTriangularNumbers(Object item) {
		int previous = strengthReq(item, 0);
		for (int lvl = 1; lvl <= MAX_UPGRADE; lvl++) {
			int current = strengthReq(item, lvl);
			int expectedDrop = isTriangular(lvl) ? 1 : 0;
			assertEquals(previous - expectedDrop, current, name(item) + " at +" + lvl
					+ (expectedDrop == 1 ? " should need one less strength than at +"
							: " should need the same strength as at +") + (lvl - 1));
			previous = current;
		}
	}

	private static int strengthReq(Object item, int lvl) {
		if (item instanceof Armor) return ((Armor) item).STRReq(lvl);
		return ((Weapon) item).STRReq(lvl);
	}

	/** 1, 3, 6, 10, 15, ... — the levels at which an upgrade also lightens the item. */
	private static boolean isTriangular(int n) {
		for (int i = 1, sum = 1; sum <= n; i++, sum += i) {
			if (sum == n) return true;
		}
		return false;
	}

	// ---------------------------------------------------------------- to-hit

	@Nested
	@DisplayName("hit chance")
	public class HitChance {

		private static final int TRIALS = 200_000;

		/**
		 * A hit is decided by two uniform rolls: the attacker rolls in {@code [0, accuracy)}, the
		 * defender in {@code [0, evasion)}, and the attacker wins ties. That gives a closed form for
		 * the hit rate, which is worth pinning because it is the one number the whole difficulty curve
		 * rests on, and because every buff in {@link Char#hit} multiplies into those same two rolls —
		 * so a modifier that stopped checking for its buff would show up here as a shifted rate.
		 */
		@Test
		public void followsTheTwoUniformRollsModel() {
			Rat attacker = new Rat();
			Rat defender = new Rat();

			float accuracy = attacker.attackSkill(defender);
			float evasion = defender.defenseSkill(attacker);
			assertTrue(accuracy > 0 && evasion > 0,
					"a rat should have both accuracy and evasion, got " + accuracy + " and " + evasion);

			Random.pushGenerator(0xA11CEL);
			int hits = 0;
			for (int i = 0; i < TRIALS; i++) {
				if (Char.hit(attacker, defender, 1f, false)) hits++;
			}
			Random.popGenerator();

			double observed = hits / (double) TRIALS;
			double expected = accuracy >= evasion
					? 1 - evasion / (2 * accuracy)
					: accuracy / (2 * evasion);
			assertEquals(expected, observed, 0.01,
					"a rat with " + accuracy + " accuracy against " + evasion + " evasion");
		}

		/** Doubling accuracy has to help, and no roll may ever become a certainty by itself. */
		@Test
		public void moreAccuracyMeansMoreHits() {
			Rat attacker = new Rat();
			Rat defender = new Rat();

			Random.pushGenerator(0xBEEFL);
			int plain = 0;
			int boosted = 0;
			for (int i = 0; i < TRIALS; i++) {
				if (Char.hit(attacker, defender, 1f, false)) plain++;
				if (Char.hit(attacker, defender, 2f, false)) boosted++;
			}
			Random.popGenerator();

			assertTrue(boosted > plain, "doubling accuracy did not land more hits: "
					+ boosted + " vs " + plain + " out of " + TRIALS);
			assertTrue(plain > 0 && plain < TRIALS,
					"an ordinary attack should sometimes hit and sometimes miss, got " + plain
							+ " hits out of " + TRIALS);
		}
	}

	@Test
	@DisplayName("the hero starts with a 200-point health pool")
	public void heroStartsWith200Health() {
		assertEquals(Hero.STARTING_HP, Dungeon.hero.HT);
		assertEquals(Hero.STARTING_HP, Dungeon.hero.HP);
	}

	@Test
	@DisplayName("hostile hits on the hero are slightly weaker")
	public void hostileDamageAgainstTheHeroIsReduced() {
		Rat rat = new Rat();
		assertEquals(Math.round(10 * BeginnerAid.ENEMY_DAMAGE),
				BeginnerAid.scaleEnemyDamage(10, rat));
		assertEquals(10, BeginnerAid.scaleEnemyDamage(10, Dungeon.hero));
		assertEquals(BeginnerAid.ENEMY_ACCURACY, BeginnerAid.enemyAccuracyFactor(rat, Dungeon.hero));
		assertEquals(1f, BeginnerAid.enemyAccuracyFactor(rat, new Rat()));
	}

	@Test
	@DisplayName("falling into a chasm costs about an eighth of current HP, not half")
	public void chasmFallIsABruiseNotAHalfBar() {
		assertEquals(25, Chasm.fallInstantDamage(200, 200));
		assertEquals(12, Chasm.fallInstantDamage(100, 200));
		assertEquals(5, Chasm.fallInstantDamage(20, 200));
		assertEquals(2, Chasm.fallBleedAmount(200, 200));
		assertTrue(Chasm.fallBleedAmount(50, 200) >= Chasm.fallBleedAmount(200, 200));
	}

	// ------------------------------------------------------------- experience

	@Test
	@DisplayName("the experience curve rises and is reachable")
	public void experienceCurveRises() {
		int previous = 0;
		for (int lvl = 1; lvl <= Hero.MAX_LEVEL; lvl++) {
			int needed = Hero.maxExp(lvl);
			assertTrue(needed > previous, "level " + lvl + " needs " + needed
					+ " experience, no more than level " + (lvl - 1) + " needed");
			previous = needed;
		}
	}
}
