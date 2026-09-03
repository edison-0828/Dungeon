/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PetAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeroCombatStatsTest {

	private Sword weapon;
	private LeatherArmor armor;

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xC017B47L);
		weapon = new Sword();
		armor = new LeatherArmor();
		Dungeon.hero.belongings.weapon = weapon;
		Dungeon.hero.belongings.armor = armor;
	}

	@Test
	@DisplayName("weapon and armor affixes aggregate into hero combat stats")
	public void aggregatesEquipmentStats() {
		weapon.affixes().set(CombatStat.ATTACK_POWER, 4);
		weapon.affixes().set(CombatStat.ACCURACY, 600);
		weapon.affixes().set(CombatStat.CRIT_CHANCE, 300);
		weapon.affixes().set(CombatStat.CRIT_DAMAGE, 1_000);
		armor.affixes().set(CombatStat.MAX_HEALTH, 8);
		armor.affixes().set(CombatStat.EVASION, 500);

		HeroCombatStats stats = Dungeon.hero.combatStats();

		assertEquals(4, stats.attackPower());
		assertEquals(600, stats.accuracyBonus());
		assertEquals(1.06f, stats.accuracyMultiplier(), 0.0001f);
		assertEquals(800, stats.critChance());
		assertEquals(16_000, stats.critDamage());
		assertEquals(500, stats.evasionBonus());
		assertEquals(8, stats.maxHealthBonus());
	}

	@Test
	@DisplayName("percentage combat stats respect their global caps")
	public void percentageStatsAreCapped() {
		weapon.affixes().set(CombatStat.ACCURACY, 50_000);
		weapon.affixes().set(CombatStat.CRIT_CHANCE, 50_000);
		weapon.affixes().set(CombatStat.CRIT_DAMAGE, 50_000);
		armor.affixes().set(CombatStat.EVASION, 50_000);

		HeroCombatStats stats = Dungeon.hero.combatStats();

		assertEquals(HeroCombatStats.MAX_ACCURACY_BONUS, stats.accuracyBonus());
		assertEquals(HeroCombatStats.MAX_CRIT_CHANCE, stats.critChance());
		assertEquals(HeroCombatStats.MAX_CRIT_DAMAGE, stats.critDamage());
		assertEquals(HeroCombatStats.MAX_EVASION_BONUS, stats.evasionBonus());
	}

	@Test
	@DisplayName("maximum health affixes update the hero health cap")
	public void maxHealthUpdatesHero() {
		int baseHealth = Dungeon.hero.HT;
		armor.affixes().set(CombatStat.MAX_HEALTH, 8);

		Dungeon.hero.updateHT(false);

		assertEquals(baseHealth + 8, Dungeon.hero.HT);
	}

	@Test
	@DisplayName("pet bond bonuses appear in the hero combat sheet")
	public void petBondAppearsOnCombatSheet() {
		HeroCombatStats stats = Dungeon.hero.combatStats();
		int baseMin = stats.minimumWeaponDamage();
		int baseMax = stats.maximumWeaponDamage();
		int baseArmorMin = stats.armorMin();
		int baseArmorMax = stats.armorMax();

		bindPet(PetAlly.Quality.COMMON, PetAlly.Appearance.SNAKE);
		stats = Dungeon.hero.combatStats();
		assertEquals(800, stats.accuracyBonus());
		assertEquals(PetBond.bonusText(PetAlly.Appearance.SNAKE, PetAlly.Quality.COMMON),
				PetBond.activeBonusText());

		bindPet(PetAlly.Quality.COMMON, PetAlly.Appearance.BAT);
		stats = Dungeon.hero.combatStats();
		assertEquals(800, stats.evasionBonus());

		bindPet(PetAlly.Quality.COMMON, PetAlly.Appearance.BEE);
		stats = Dungeon.hero.combatStats();
		assertEquals(Math.round(baseMin * 1.08f), stats.minimumWeaponDamage());
		assertEquals(Math.round(baseMax * 1.08f), stats.maximumWeaponDamage());

		bindPet(PetAlly.Quality.COMMON, PetAlly.Appearance.CRAB);
		stats = Dungeon.hero.combatStats();
		assertEquals(baseArmorMin + 1, stats.armorMin());
		assertEquals(baseArmorMax + 1, stats.armorMax());
	}

	@Test
	@DisplayName("hero panel combat values are defined for every class")
	public void heroPanelValuesAreDefinedForEveryClass() {
		for (HeroClass heroClass : HeroClass.values()) {
			Dungeon.hero.heroClass = heroClass;
			Dungeon.hero.applyClassAttributes();
			HeroCombatStats stats = Dungeon.hero.combatStats();

			assertTrue(stats.minimumWeaponDamage() >= 1, heroClass.title());
			assertTrue(stats.maximumWeaponDamage() >= stats.minimumWeaponDamage(), heroClass.title());
			assertTrue(stats.armorMin() >= 0, heroClass.title());
			assertTrue(stats.armorMax() >= stats.armorMin(), heroClass.title());
			assertTrue(stats.critChance() > 0, heroClass.title());
			stats.spellPowerBonus();
			stats.elementalBonus(CombatStat.FIRE_POWER);
			stats.elementalBonus(CombatStat.FROST_POWER);
			stats.elementalBonus(CombatStat.SHOCK_POWER);
			stats.elementalBonus(CombatStat.POISON_POWER);
			stats.elementalBonus(CombatStat.MAGIC_POWER);
			PetBond.activeBonusText();
		}
	}

	@Test
	@DisplayName("ring affixes contribute to worn combat stats")
	public void ringAffixesContribute() {
		HeroCombatStats before = Dungeon.hero.combatStats();
		int baseAccuracy = before.accuracyBonus();
		int baseHealth = before.maxHealthBonus();

		RingOfAccuracy ring = new RingOfAccuracy();
		ring.affixes().set(CombatStat.ACCURACY, 400);
		ring.affixes().set(CombatStat.MAX_HEALTH, 6);
		Dungeon.hero.belongings.ring = ring;

		HeroCombatStats stats = Dungeon.hero.combatStats();
		assertEquals(baseAccuracy + 400, stats.accuracyBonus());
		assertEquals(baseHealth + 6, stats.maxHealthBonus());
	}

	@Test
	@DisplayName("wand affixes apply when that wand is the damage source")
	public void wandAffixesApplyOnZap() {
		WandOfMagicMissile wand = new WandOfMagicMissile();
		HeroCombatStats stats = Dungeon.hero.combatStats();
		int base = stats.modifyOutgoingDamage(10, wand);

		wand.affixes().set(CombatStat.MAGIC_POWER, 2_000);
		int boosted = Dungeon.hero.combatStats().modifyOutgoingDamage(10, wand);
		assertTrue(boosted > base);
	}

	private static void bindPet(PetAlly.Quality quality, PetAlly.Appearance appearance) {
		PetWhistle whistle = Dungeon.hero.belongings.getItem(PetWhistle.class);
		if (whistle == null) {
			whistle = new PetWhistle();
			assertTrue(whistle.collect());
		}
		whistle.bind(quality, appearance);
	}
}
