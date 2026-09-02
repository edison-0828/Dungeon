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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class EquipmentAffixes implements Bundlable {

	private static final String ROLLED = "rolled";
	private static final String RARITY = "rarity";
	private static final String STAT_PREFIX = "stat_";

	private boolean rolled;
	private EquipmentRarity rarity = EquipmentRarity.NORMAL;
	private final EnumMap<CombatStat, Integer> values = new EnumMap<>(CombatStat.class);

	public boolean rolled() {
		return rolled;
	}

	public EquipmentRarity rarity() {
		return rarity;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public void copyFrom(EquipmentAffixes source) {
		rolled = source.rolled;
		rarity = source.rarity;
		values.clear();
		values.putAll(source.values);
	}

	public void roll(boolean offensive, int tier, int floorSet) {
		if (rolled) return;
		rolled = true;
		tier = Math.max(1, Math.min(5, tier));
		floorSet = Math.max(0, Math.min(4, floorSet));

		float rarityRoll = Random.Float();
		if (rarityRoll < 0.05f) rarity = EquipmentRarity.EPIC;
		else if (rarityRoll < 0.20f) rarity = EquipmentRarity.RARE;
		else if (rarityRoll < 0.55f) rarity = EquipmentRarity.FINE;
		else rarity = EquipmentRarity.NORMAL;

		ArrayList<CombatStat> pool = new ArrayList<>();
		if (offensive) {
			pool.add(CombatStat.ATTACK_POWER);
			pool.add(CombatStat.ACCURACY);
			pool.add(CombatStat.CRIT_CHANCE);
			pool.add(CombatStat.CRIT_DAMAGE);
			CombatStat[] elements = {
					CombatStat.FIRE_POWER, CombatStat.FROST_POWER, CombatStat.SHOCK_POWER,
					CombatStat.POISON_POWER, CombatStat.MAGIC_POWER};
			int first = Random.Int(elements.length);
			int second = (first + 1 + Random.Int(elements.length - 1)) % elements.length;
			pool.add(elements[first]);
			pool.add(elements[second]);
		} else {
			pool.add(CombatStat.MAX_HEALTH);
			pool.add(CombatStat.EVASION);
			pool.add(CombatStat.ATTACK_POWER);
			pool.add(CombatStat.CRIT_CHANCE);
		}

		for (int i = 0; i < rarity.affixCount && !pool.isEmpty(); i++) {
			CombatStat stat = pool.remove(Random.Int(pool.size()));
			values.put(stat, rolledValue(stat, tier, floorSet));
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
		int count = values.size();
		rarity = count >= 3 ? EquipmentRarity.EPIC
				: count == 2 ? EquipmentRarity.RARE
				: count == 1 ? EquipmentRarity.FINE
				: EquipmentRarity.NORMAL;
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
		for (Map.Entry<CombatStat, Integer> entry : values.entrySet()) {
			bundle.put(STAT_PREFIX + entry.getKey().name(), entry.getValue());
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		rolled = bundle.getBoolean(ROLLED);
		rarity = bundle.getEnum(RARITY, EquipmentRarity.class);
		if (rarity == null) rarity = EquipmentRarity.NORMAL;
		values.clear();
		for (CombatStat stat : CombatStat.values()) {
			String key = STAT_PREFIX + stat.name();
			if (bundle.contains(key)) values.put(stat, bundle.getInt(key));
		}
	}
}
