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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
import com.watabou.utils.Random;

public class HeroCombatStats {

	public static final int BASE_CRIT_CHANCE = 500;
	public static final int BASE_CRIT_DAMAGE = 15_000;
	public static final int MAX_CRIT_CHANCE = 5_000;
	public static final int MAX_CRIT_DAMAGE = 25_000;
	public static final int MAX_ACCURACY_BONUS = 5_000;
	public static final int MAX_EVASION_BONUS = 4_000;

	private final Hero hero;

	public HeroCombatStats(Hero hero) {
		this.hero = hero;
	}

	private int equipmentValue(CombatStat stat) {
		int value = 0;
		KindOfWeapon weapon = hero.belongings.attackingWeapon();
		if (weapon instanceof Item) value += ((Item) weapon).affixValue(stat);
		if (hero.belongings.armor() != null) value += hero.belongings.armor().affixValue(stat);
		return value;
	}

	public int attackPower() {
		return Math.max(0, equipmentValue(CombatStat.ATTACK_POWER));
	}

	public int accuracyBonus() {
		return Math.min(MAX_ACCURACY_BONUS, Math.max(0,
				equipmentValue(CombatStat.ACCURACY) + bonusBps(PetBond.accuracyMultiplier())));
	}

	public float accuracyMultiplier() {
		return 1f + accuracyBonus() / 10_000f;
	}

	public int evasionBonus() {
		return Math.min(MAX_EVASION_BONUS, Math.max(0,
				equipmentValue(CombatStat.EVASION) + bonusBps(PetBond.evasionMultiplier())));
	}

	public float evasionMultiplier() {
		return 1f + evasionBonus() / 10_000f;
	}

	public int critChance() {
		return Math.min(MAX_CRIT_CHANCE,
				Math.max(0, BASE_CRIT_CHANCE + equipmentValue(CombatStat.CRIT_CHANCE)
						+ bonusBps(PetBond.critMultiplier())));
	}

	public int critDamage() {
		return Math.min(MAX_CRIT_DAMAGE,
				Math.max(10_000, BASE_CRIT_DAMAGE + equipmentValue(CombatStat.CRIT_DAMAGE)));
	}

	public float critDamageMultiplier() {
		return critDamage() / 10_000f;
	}

	public boolean rollCritical() {
		return Random.Int(10_000) < critChance();
	}

	public int maxHealthBonus() {
		return Math.max(0, equipmentValue(CombatStat.MAX_HEALTH));
	}

	public int minimumWeaponDamage() {
		KindOfWeapon weapon = hero.belongings.attackingWeapon();
		int weaponDmg = weapon == null ? 1 : weapon.min();
		return Math.round(weaponDmg * PetBond.attackMultiplier()) + attackPower();
	}

	public int maximumWeaponDamage() {
		KindOfWeapon weapon = hero.belongings.attackingWeapon();
		int weaponDmg = weapon == null ? 1 : weapon.max();
		return Math.round(weaponDmg * PetBond.attackMultiplier()) + attackPower();
	}

	public int armorMin() {
		Armor armor = hero.belongings.armor();
		return Math.max(0, (armor == null ? 0 : armor.DRMin()) + PetBond.armorBonus());
	}

	public int armorMax() {
		Armor armor = hero.belongings.armor();
		return Math.max(0, (armor == null ? 0 : armor.DRMax()) + PetBond.armorBonus());
	}

	private static int bonusBps(float multiplier) {
		return Math.round((multiplier - 1f) * 10_000f);
	}
}
