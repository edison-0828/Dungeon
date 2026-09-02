/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public enum MobAttackType {
	PHYSICAL(0xC45A3A),
	FIRE    (0xFF5500),
	FROST   (0x66DDFF),
	SHOCK   (0xFFE14A),
	POISON  (0x4CAF50),
	ACID    (0xC6D32C),
	MAGIC   (0xB266FF);

	public final int tint;

	MobAttackType(int tint) {
		this.tint = tint;
	}

	public String title() {
		return Messages.get(this, name().toLowerCase());
	}
}
