/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

/**
 * Brief hidden resistance after the hero recovers from hard crowd control.
 * Char.resist applies a 50% duration multiplier for each listed resistance.
 */
public class ControlResistance extends FlavourBuff {

	public static final float DURATION = 2f;

	{
		type = buffType.POSITIVE;
		resistances.add(Paralysis.class);
		resistances.add(Frost.class);
	}
}
