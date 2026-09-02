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
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
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

	private static void bindPet(PetAlly.Quality quality, PetAlly.Appearance appearance) {
		PetWhistle whistle = Dungeon.hero.belongings.getItem(PetWhistle.class);
		if (whistle == null) {
			whistle = new PetWhistle();
			assertTrue(whistle.collect());
		}
		whistle.bind(quality, appearance);
	}
}
