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

public enum CombatStat {
	ATTACK_POWER(false), ACCURACY(true), CRIT_CHANCE(true),
	CRIT_DAMAGE(true), EVASION(true), MAX_HEALTH(false),
	FIRE_POWER(true), FROST_POWER(true), SHOCK_POWER(true),
	POISON_POWER(true), MAGIC_POWER(true);

	private final boolean percent;

	CombatStat(boolean percent) {
		this.percent = percent;
	}

	public boolean percent() {
		return percent;
	}
}
