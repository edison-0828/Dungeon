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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.watabou.utils.Random;

import java.util.LinkedHashSet;

public class HeroCombatStats {

	public static final int BASE_CRIT_CHANCE = 500;
	public static final int BASE_CRIT_DAMAGE = 15_000;
	public static final int MAX_CRIT_CHANCE = 5_000;
	public static final int MAX_CRIT_DAMAGE = 25_000;
	public static final int MAX_ACCURACY_BONUS = 5_000;
	public static final int MAX_EVASION_BONUS = 4_000;
	public static final int MAX_ELEMENT_BONUS = 6_000;
	public static final int SPELL_POWER_PER_INT = 300;

	private final Hero hero;

	public HeroCombatStats(Hero hero) {
		this.hero = hero;
	}

	private int equipmentValue(CombatStat stat) {
		return equipmentValue(stat, null);
	}

	private int equipmentValue(CombatStat stat, Object src) {
		LinkedHashSet<Item> counted = new LinkedHashSet<>();
		addCounted(counted, hero.belongings.attackingWeapon());
		addCounted(counted, hero.belongings.armor());
		addCounted(counted, hero.belongings.ring());
		KindofMisc misc = hero.belongings.misc();
		if (misc instanceof Ring) {
			counted.add(misc);
		}
		KindOfWeapon attacking = hero.belongings.attackingWeapon();
		if (attacking instanceof MagesStaff) {
			addCounted(counted, ((MagesStaff) attacking).wand());
		}
		if (src instanceof Item) {
			counted.add((Item) src);
		}

		int value = 0;
		for (Item item : counted) {
			value += item.affixValue(stat);
		}
		return value;
	}

	private static void addCounted(LinkedHashSet<Item> counted, Object item) {
		if (item instanceof Item) {
			counted.add((Item) item);
		}
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

	public int spellPowerBonus() {
		return (hero.INT() - Hero.STARTING_INT) * SPELL_POWER_PER_INT;
	}

	public float spellPowerMultiplier() {
		return Math.max(0.70f, Math.min(2f, 1f + spellPowerBonus() / 10_000f));
	}

	public int elementalBonus(CombatStat stat) {
		return elementalBonus(stat, null);
	}

	private int elementalBonus(CombatStat stat, Object src) {
		if (stat == null) return 0;
		int bonus = equipmentValue(stat, src);
		if (hero.heroClass != null && hero.heroClass.affinityStat() == stat) {
			bonus += hero.heroClass.affinityBonusAt(hero.lvl);
		}
		return Math.min(MAX_ELEMENT_BONUS, Math.max(0, bonus));
	}

	public float elementalMultiplier(HeroDamageType type) {
		return elementalMultiplier(type, null);
	}

	private float elementalMultiplier(HeroDamageType type, Object src) {
		if (type == null) return 1f;
		CombatStat stat = type.combatStat();
		if (stat == null) return 1f;
		return 1f + elementalBonus(stat, src) / 10_000f;
	}

	public float outgoingMultiplier(Object src) {
		HeroDamageType type = HeroDamageType.of(src);
		float multiplier = elementalMultiplier(type, src);
		if (HeroDamageType.isHeroSpellSource(src)) {
			multiplier *= spellPowerMultiplier();
		}
		return multiplier;
	}

	public int modifyOutgoingDamage(int dmg, Object src) {
		if (dmg <= 0 || src == null || src instanceof HeroDamageType.Hit) return dmg;
		if (!HeroDamageType.isHeroOutgoing(src)) return dmg;
		if (!HeroDamageType.isHeroSpellSource(src) && HeroDamageType.of(src) == HeroDamageType.PHYSICAL) {
			return dmg;
		}
		return Math.max(1, Math.round(dmg * outgoingMultiplier(src)));
	}

	public float modifyOutgoingDamage(float dmg, Object src) {
		if (dmg <= 0 || src == null || src instanceof HeroDamageType.Hit) return dmg;
		if (!HeroDamageType.isHeroOutgoing(src)) return dmg;
		if (!HeroDamageType.isHeroSpellSource(src) && HeroDamageType.of(src) == HeroDamageType.PHYSICAL) {
			return dmg;
		}
		return dmg * outgoingMultiplier(src);
	}

	public void dealAffinityHit(Char enemy, int physicalDmg) {
		if (enemy == null || !enemy.isAlive() || physicalDmg <= 0) {
			return;
		}
		HeroDamageType type = HeroDamageType.ofAffinity(hero.heroClass);
		float extra = elementalMultiplier(type) - 1f;
		if (extra <= 0f) {
			return;
		}
		int bonus = Math.max(1, Math.round(physicalDmg * extra));
		enemy.damage(bonus, new HeroDamageType.Hit(type));
	}

	public DamageWand equippedDamageWand() {
		KindOfWeapon weapon = hero.belongings.weapon();
		if (weapon instanceof MagesStaff) {
			Wand staffWand = ((MagesStaff) weapon).wand();
			if (staffWand instanceof DamageWand) {
				return (DamageWand) staffWand;
			}
		}
		for (Item item : hero.belongings) {
			if (item instanceof DamageWand) {
				return (DamageWand) item;
			}
		}
		return null;
	}

	public int minimumSpellDamage() {
		DamageWand wand = equippedDamageWand();
		if (wand == null) return 0;
		return Math.max(1, Math.round(wand.min() * outgoingMultiplier(wand)));
	}

	public int maximumSpellDamage() {
		DamageWand wand = equippedDamageWand();
		if (wand == null) return 0;
		return Math.max(1, Math.round(wand.max() * outgoingMultiplier(wand)));
	}

	private static int bonusBps(float multiplier) {
		return Math.round((multiplier - 1f) * 10_000f);
	}
}
