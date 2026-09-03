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

public enum EquipmentRarity {
	NORMAL(0, 1.00f, 0),
	FINE(1, 1.00f, 0x99CCFF),
	EXCELLENT(2, 1.10f, 0x33CC66),
	EPIC(3, 1.20f, 0xCC66FF),
	LEGENDARY(4, 1.35f, 0xFF9933),
	MYTHIC(5, 1.50f, 0xFFD700);

	public final int affixCount;
	public final float valueMultiplier;
	public final int glowColor;

	EquipmentRarity(int affixCount, float valueMultiplier, int glowColor) {
		this.affixCount = affixCount;
		this.valueMultiplier = valueMultiplier;
		this.glowColor = glowColor;
	}

	public static EquipmentRarity fromSavedName(String name) {
		if (name == null || name.isEmpty()) {
			return NORMAL;
		}
		if ("RARE".equals(name)) {
			return EXCELLENT;
		}
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return NORMAL;
		}
	}

	public static EquipmentRarity fromAffixCount(int count) {
		if (count >= MYTHIC.affixCount) return MYTHIC;
		if (count >= LEGENDARY.affixCount) return LEGENDARY;
		if (count >= EPIC.affixCount) return EPIC;
		if (count >= EXCELLENT.affixCount) return EXCELLENT;
		if (count >= FINE.affixCount) return FINE;
		return NORMAL;
	}
}
