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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.HashSet;

/**
 * Extra teaching and a light first-clear safety net, active only until the player has beaten Goo
 * at least once. Challenge runs are treated as knowing the game already.
 *
 * <p>Hints fire at the moment they are useful and at most once per run, so they do not replace the
 * journal and they do not nag a player who already acted on them.
 */
public final class BeginnerAid {

	private BeginnerAid() {}

	private static final HashSet<String> shown = new HashSet<>();

	/** Slightly softer enemy hits so early mistakes are survivable. */
	public static final float ENEMY_DAMAGE = 0.85f;
	/** Enemy accuracy roll vs the hero. */
	public static final float ENEMY_ACCURACY = 0.75f;

	public static void resetForRun() {
		shown.clear();
	}

	public static int scaleEnemyDamage(int dmg, Object src) {
		if (dmg <= 0 || !isHostileDamage(src)) return dmg;
		return Math.max(1, Math.round(dmg * ENEMY_DAMAGE));
	}

	public static boolean isHostileDamage(Object src) {
		if (src instanceof Char) {
			return ((Char) src).alignment == Char.Alignment.ENEMY;
		}
		if (src == null) return false;
		Class<?> enclosing = src.getClass().getEnclosingClass();
		while (enclosing != null) {
			if (Mob.class.isAssignableFrom(enclosing)) return true;
			enclosing = enclosing.getEnclosingClass();
		}
		return false;
	}

	public static float enemyAccuracyFactor(Char attacker, Char defender) {
		if (defender instanceof Hero
				&& attacker != null
				&& attacker.alignment == Char.Alignment.ENEMY) {
			return ENEMY_ACCURACY;
		}
		return 1f;
	}

	public static boolean isNovice() {
		if (Dungeon.challenges != 0) return false;
		try {
			Badges.loadGlobal();
			return !Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_1);
		} catch (Exception e) {
			return false;
		}
	}

	public static void hint(String id) {
		if (!isNovice() || !shown.add(id)) return;
		GLog.p(Messages.get(BeginnerAid.class, id));
	}

	public static void onItemCollected(Item item) {
		if (item != null && !item.isIdentified() && (item instanceof Weapon || item instanceof Armor)) {
			hint("unidentified");
		}
	}
}
