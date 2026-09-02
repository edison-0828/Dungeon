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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FrostImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Spell;
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public enum HeroDamageType {
	PHYSICAL,
	FIRE,
	FROST,
	SHOCK,
	POISON,
	MAGIC;

	public CombatStat combatStat() {
		switch (this) {
			case FIRE: return CombatStat.FIRE_POWER;
			case FROST: return CombatStat.FROST_POWER;
			case SHOCK: return CombatStat.SHOCK_POWER;
			case POISON: return CombatStat.POISON_POWER;
			case MAGIC: return CombatStat.MAGIC_POWER;
			default: return null;
		}
	}

	public String title() {
		return Messages.get(this, name().toLowerCase());
	}

	public static HeroDamageType of(Object src) {
		if (src == null) return PHYSICAL;

		if (src instanceof WandOfFireblast || src instanceof Blazing
				|| src instanceof Burning || src instanceof FireImbue) {
			return FIRE;
		}
		if (src instanceof WandOfFrost || src instanceof Chilling
				|| src instanceof FrostImbue) {
			return FROST;
		}
		if (src instanceof WandOfLightning || src instanceof Shocking) {
			return SHOCK;
		}
		if (src instanceof WandOfCorrosion || src instanceof Poison
				|| src instanceof Corrosion || src instanceof ToxicImbue) {
			return POISON;
		}
		if (src instanceof WandOfBlastWave || src instanceof WandOfLivingEarth) {
			return PHYSICAL;
		}
		if (src instanceof WandOfMagicMissile || src instanceof WandOfDisintegration
				|| src instanceof WandOfPrismaticLight || src instanceof WandOfTransfusion
				|| src instanceof DamageWand || src instanceof Wand
				|| src instanceof ClericSpell || src instanceof Spell) {
			return MAGIC;
		}
		return PHYSICAL;
	}

	public static boolean isHeroSpellSource(Object src) {
		return src instanceof Wand || src instanceof ClericSpell || src instanceof Spell;
	}
}
