/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.stats;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class EquipmentAffixes implements Bundlable {

	public enum Family {
		MELEE, ARMOR, MISSILE, WAND, RING
	}

	private static final String ROLLED = "rolled";
	private static final String RARITY = "rarity";
	private static final String UNIQUE_ID = "unique_id";
	private static final String STAT_PREFIX = "stat_";

	private static final CombatStat[] ELEMENTS = {
			CombatStat.FIRE_POWER, CombatStat.FROST_POWER, CombatStat.SHOCK_POWER,
			CombatStat.POISON_POWER, CombatStat.MAGIC_POWER};
	private static final CombatStat[] NON_MAGIC_ELEMENTS = {
			CombatStat.FIRE_POWER, CombatStat.FROST_POWER, CombatStat.SHOCK_POWER,
			CombatStat.POISON_POWER};

	// NORMAL, FINE, EXCELLENT, EPIC, LEGENDARY, MYTHIC
	private static final float[][] RARITY_WEIGHTS = {
			{55, 30, 12,  3,  0, 0},
			{40, 32, 20,  7,  1, 0},
			{28, 28, 28, 13,  3, 0},
			{18, 22, 30, 22,  7, 1},
			{10, 18, 30, 28, 11, 3}
	};

	private boolean rolled;
	private EquipmentRarity rarity = EquipmentRarity.NORMAL;
	private String uniqueId = "";
	private final EnumMap<CombatStat, Integer> values = new EnumMap<>(CombatStat.class);
	private static int rollsThisRun;

	public static void resetRun() {
		rollsThisRun = 0;
	}

	public boolean rolled() {
		return rolled;
	}

	public EquipmentRarity rarity() {
		return rarity;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public boolean hasUniqueEffect() {
		return uniqueId != null && !uniqueId.isEmpty();
	}

	public String uniqueId() {
		return uniqueId == null ? "" : uniqueId;
	}

	public void copyFrom(EquipmentAffixes source) {
		rolled = source.rolled;
		rarity = source.rarity;
		uniqueId = source.uniqueId();
		values.clear();
		values.putAll(source.values);
	}

	public void resetToNormal() {
		rolled = true;
		rarity = EquipmentRarity.NORMAL;
		uniqueId = "";
		values.clear();
	}

	public boolean sameAs(EquipmentAffixes other) {
		if (other == null) return false;
		return rolled == other.rolled
				&& rarity == other.rarity
				&& Objects.equals(uniqueId(), other.uniqueId())
				&& values.equals(other.values);
	}

	public void roll(boolean offensive, int tier, int floorSet) {
		roll(offensive ? Family.MELEE : Family.ARMOR, tier, floorSet);
	}

	public void roll(Family family, int tier, int floorSet) {
		if (rolled) return;
		rolled = true;
		tier = Math.max(1, Math.min(5, tier));
		floorSet = Math.max(0, Math.min(4, floorSet));
		rarity = rollRarity(floorSet);

		ArrayList<CombatStat> pool = poolFor(family);
		for (int i = 0; i < rarity.affixCount && !pool.isEmpty(); i++) {
			CombatStat stat = pool.remove(Random.Int(pool.size()));
			int rolledValue = Math.max(1, Math.round(rolledValue(stat, tier, floorSet) * rarity.valueMultiplier));
			values.put(stat, rolledValue);
		}
	}

	public void rollIsolated(Family family, int tier, int floorSet) {
		if (rolled) return;
		long seed = Dungeon.seed;
		seed = seed * 31 + Dungeon.depth;
		seed = seed * 31 + Dungeon.branch;
		seed = seed * 31 + (++rollsThisRun);
		Random.pushGenerator(seed);
			roll(family, tier, floorSet);
		Random.popGenerator();
	}

	public static EquipmentRarity rollRarity(int floorSet) {
		floorSet = Math.max(0, Math.min(4, floorSet));
		int index = Random.chances(RARITY_WEIGHTS[floorSet]);
		if (index < 0) index = 0;
		return EquipmentRarity.values()[index];
	}

	private ArrayList<CombatStat> poolFor(Family family) {
		ArrayList<CombatStat> pool = new ArrayList<>();
		switch (family) {
			case MELEE:
				pool.add(CombatStat.ATTACK_POWER);
				pool.add(CombatStat.ACCURACY);
				pool.add(CombatStat.CRIT_CHANCE);
				pool.add(CombatStat.CRIT_DAMAGE);
				addDistinctElements(pool, 2, ELEMENTS);
				break;
			case ARMOR:
				pool.add(CombatStat.MAX_HEALTH);
				pool.add(CombatStat.EVASION);
				pool.add(CombatStat.ACCURACY);
				pool.add(CombatStat.ATTACK_POWER);
				pool.add(CombatStat.CRIT_CHANCE);
				addDistinctElements(pool, 1, ELEMENTS);
				break;
			case MISSILE:
				pool.add(CombatStat.ATTACK_POWER);
				pool.add(CombatStat.ACCURACY);
				pool.add(CombatStat.CRIT_CHANCE);
				pool.add(CombatStat.CRIT_DAMAGE);
				addDistinctElements(pool, 1, ELEMENTS);
				break;
			case WAND:
				pool.add(CombatStat.MAGIC_POWER);
				pool.add(CombatStat.CRIT_CHANCE);
				pool.add(CombatStat.CRIT_DAMAGE);
				pool.add(CombatStat.MAX_HEALTH);
				addDistinctElements(pool, 1, NON_MAGIC_ELEMENTS);
				break;
			case RING:
				pool.add(CombatStat.MAX_HEALTH);
				pool.add(CombatStat.EVASION);
				pool.add(CombatStat.ACCURACY);
				addDistinctElements(pool, 1, ELEMENTS);
				break;
		}
		return pool;
	}

	private void addDistinctElements(ArrayList<CombatStat> pool, int count, CombatStat[] source) {
		ArrayList<CombatStat> remaining = new ArrayList<>();
		for (CombatStat stat : source) remaining.add(stat);
		for (int i = 0; i < count && !remaining.isEmpty(); i++) {
			pool.add(remaining.remove(Random.Int(remaining.size())));
		}
	}

	private int rolledValue(CombatStat stat, int tier, int floorSet) {
		switch (stat) {
			case ATTACK_POWER:
				return 1 + floorSet + tier / 2;
			case ACCURACY:
			case EVASION:
				return 300 + 100 * tier + 100 * floorSet;
			case CRIT_CHANCE:
				return 200 + 50 * tier + 50 * floorSet;
			case CRIT_DAMAGE:
				return 800 + 200 * tier + 100 * floorSet;
			case MAX_HEALTH:
				return 2 + 2 * tier + 2 * floorSet;
			case FIRE_POWER:
			case FROST_POWER:
			case SHOCK_POWER:
			case POISON_POWER:
			case MAGIC_POWER:
				return 400 + 100 * tier + 100 * floorSet;
			default:
				return 0;
		}
	}

	public int value(CombatStat stat, int itemLevel) {
		int value = values.containsKey(stat) ? values.get(stat) : 0;
		int positiveLevel = Math.max(0, itemLevel);
		if (value == 0 || positiveLevel == 0) return value;
		switch (stat) {
			case ATTACK_POWER:
				return value + positiveLevel;
			case MAX_HEALTH:
				return value + 2 * positiveLevel;
			case CRIT_DAMAGE:
				return value + 200 * positiveLevel;
			default:
				return value + 50 * positiveLevel;
		}
	}

	public void set(CombatStat stat, int value) {
		rolled = true;
		if (value == 0) values.remove(stat);
		else values.put(stat, value);
		rarity = EquipmentRarity.fromAffixCount(values.size());
	}

	public ItemSprite.Glowing glowing() {
		if (rarity.glowColor == 0) return null;
		return new ItemSprite.Glowing(rarity.glowColor);
	}

	public String info(int itemLevel) {
		if (values.isEmpty()) return "";
		StringBuilder result = new StringBuilder();
		result.append(Messages.get(EquipmentAffixes.class, "header",
				Messages.get(EquipmentAffixes.class, rarity.name().toLowerCase())));
		for (Map.Entry<CombatStat, Integer> entry : values.entrySet()) {
			int value = value(entry.getKey(), itemLevel);
			String formatted = entry.getKey().percent()
					? "+" + Messages.decimalFormat("#.##", value / 100f) + "%"
					: "+" + value;
			result.append("\n").append(Messages.get(EquipmentAffixes.class, "stat",
					Messages.get(CombatStat.class, entry.getKey().name().toLowerCase()), formatted));
		}
		return result.toString();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(ROLLED, rolled);
		bundle.put(RARITY, rarity);
		if (uniqueId != null && !uniqueId.isEmpty()) {
			bundle.put(UNIQUE_ID, uniqueId);
		}
		for (Map.Entry<CombatStat, Integer> entry : values.entrySet()) {
			bundle.put(STAT_PREFIX + entry.getKey().name(), entry.getValue());
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		rolled = bundle.getBoolean(ROLLED);
		if (bundle.contains(RARITY)) {
			rarity = EquipmentRarity.fromSavedName(bundle.getString(RARITY));
		} else {
			rarity = EquipmentRarity.NORMAL;
		}
		uniqueId = bundle.contains(UNIQUE_ID) ? bundle.getString(UNIQUE_ID) : "";
		values.clear();
		for (CombatStat stat : CombatStat.values()) {
			String key = STAT_PREFIX + stat.name();
			if (bundle.contains(key)) values.put(stat, bundle.getInt(key));
		}
	}
}
