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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.watabou.utils.ColorMath;
import com.watabou.utils.GameMath;

import java.util.HashMap;

public final class MobAttackProfile {

	public static final float MIN_INTENSITY = 0.15f;
	public static final float MAX_INTENSITY = 1f;

	private static final HashMap<Class<? extends Mob>, Entry> TABLE = new HashMap<>();

	static {
		put(Rat.class,              MobAttackType.PHYSICAL, 1, 4);
		put(Snake.class,            MobAttackType.PHYSICAL, 1, 4);
		put(Gnoll.class,            MobAttackType.PHYSICAL, 1, 6);
		put(Swarm.class,            MobAttackType.PHYSICAL, 1, 4);
		put(Crab.class,             MobAttackType.PHYSICAL, 1, 7);
		put(Slime.class,            MobAttackType.PHYSICAL, 2, 5);
		put(FetidRat.class,         MobAttackType.ACID,     1, 4);
		put(GnollExile.class,       MobAttackType.PHYSICAL, 1, 10);
		put(CausticSlime.class,     MobAttackType.ACID,     2, 5);

		put(Skeleton.class,         MobAttackType.PHYSICAL, 2, 10);
		put(Thief.class,            MobAttackType.PHYSICAL, 1, 10);
		put(DM100.class,            MobAttackType.SHOCK,    2, 8);
		put(Guard.class,            MobAttackType.PHYSICAL, 4, 12);
		put(Necromancer.class,      MobAttackType.MAGIC,    1, 8);

		put(Bat.class,              MobAttackType.PHYSICAL, 5, 18);
		put(Brute.class,            MobAttackType.PHYSICAL, 5, 25);
		put(Shaman.class,           MobAttackType.MAGIC,    5, 10);
		put(Shaman.BlueShaman.class,MobAttackType.SHOCK,    5, 10);
		put(Spinner.class,          MobAttackType.POISON,   10, 20);
		put(DM200.class,            MobAttackType.PHYSICAL, 10, 25);
		put(DM201.class,            MobAttackType.PHYSICAL, 15, 25);

		put(Ghoul.class,            MobAttackType.PHYSICAL, 16, 22);
		put(Elemental.FireElemental.class,  MobAttackType.FIRE,  20, 25);
		put(Elemental.FrostElemental.class, MobAttackType.FROST, 20, 25);
		put(Elemental.ShockElemental.class, MobAttackType.SHOCK, 20, 25);
		put(Elemental.ChaosElemental.class, MobAttackType.MAGIC, 20, 25);
		put(Warlock.class,          MobAttackType.MAGIC,    12, 18);
		put(Monk.class,             MobAttackType.PHYSICAL, 12, 25);
		put(Senior.class,           MobAttackType.PHYSICAL, 16, 25);
		put(Golem.class,            MobAttackType.PHYSICAL, 25, 30);

		put(RipperDemon.class,      MobAttackType.PHYSICAL, 15, 25);
		put(Succubus.class,         MobAttackType.PHYSICAL, 25, 30);
		put(Eye.class,              MobAttackType.MAGIC,    20, 30);
		put(Scorpio.class,          MobAttackType.POISON,   30, 40);
		put(Acidic.class,           MobAttackType.ACID,     30, 40);

		put(YogFist.BurningFist.class, MobAttackType.FIRE,   18, 36);
		put(YogFist.RottingFist.class, MobAttackType.ACID,   18, 36);
		put(YogFist.BrightFist.class,  MobAttackType.MAGIC,  18, 36);
		put(YogFist.DarkFist.class,    MobAttackType.MAGIC,  18, 36);
		put(YogFist.class,             MobAttackType.PHYSICAL, 18, 36);

		put(CrystalWisp.class,      MobAttackType.MAGIC,    5, 10);
		put(Goo.class,              MobAttackType.ACID,     1, 8);
	}

	private MobAttackProfile() {}

	public static boolean tintsSprite(Mob mob) {
		return mob != null
				&& !(mob instanceof NPC)
				&& mob.alignment == Char.Alignment.ENEMY;
	}

	public static MobAttackType type(Mob mob) {
		Entry entry = entry(mob);
		return entry == null ? MobAttackType.PHYSICAL : entry.type;
	}

	public static float typicalDamage(Mob mob) {
		Entry entry = entry(mob);
		if (entry != null) {
			return (entry.min + entry.max) / 2f;
		}
		int ht = Math.max(1, mob.HT);
		return Math.max(1f, ht / 6f);
	}

	public static float intensity(Mob mob) {
		float expected = 2f + Math.max(1, Dungeon.depth);
		return GameMath.gate(MIN_INTENSITY, typicalDamage(mob) / expected, MAX_INTENSITY);
	}

	public static int tintColor(Mob mob) {
		return ColorMath.interpolate(0xFFFFFF, type(mob).tint, intensity(mob));
	}

	private static Entry entry(Mob mob) {
		if (mob == null) {
			return null;
		}
		Class<?> cls = mob.getClass();
		while (cls != null && Mob.class.isAssignableFrom(cls)) {
			Entry found = TABLE.get(cls);
			if (found != null) {
				return found;
			}
			cls = cls.getSuperclass();
		}
		return null;
	}

	private static void put(Class<? extends Mob> cls, MobAttackType type, int min, int max) {
		TABLE.put(cls, new Entry(type, min, max));
	}

	private static final class Entry {
		final MobAttackType type;
		final int min;
		final int max;

		Entry(MobAttackType type, int min, int max) {
			this.type = type;
			this.min = min;
			this.max = max;
		}
	}
}
