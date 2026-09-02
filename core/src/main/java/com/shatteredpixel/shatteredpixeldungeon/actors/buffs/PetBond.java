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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PetAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Owner-side companion bonus. Appearance picks the skill; quality scales the magnitude.
 * Recalling the pet keeps the bond; a fallen pet (revive cooldown) pauses it.
 */
public class PetBond extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	/** Common quality uses this rate; other tiers multiply by {@link PetAlly.Quality#statMul}. */
	public static final float BASE_PERCENT = 0.08f;

	private PetAlly.Quality quality = PetAlly.Quality.COMMON;
	private PetAlly.Appearance appearance = PetAlly.Appearance.RAT;

	public static void refresh(Hero hero) {
		refresh(hero, PetWhistle.get());
	}

	public static void refresh(Hero hero, PetWhistle whistle) {
		if (hero == null) {
			return;
		}
		boolean want = whistle != null && hero.buff(PetWhistle.ReviveCooldown.class) == null;
		PetBond bond = hero.buff(PetBond.class);
		if (!want) {
			if (bond != null) {
				bond.detach();
				hero.updateHT(false);
			}
			return;
		}
		bond = Buff.affect(hero, PetBond.class);
		bond.quality = whistle.quality();
		bond.appearance = whistle.appearance();
		hero.updateHT(true);
	}

	public static PetBond get() {
		Hero hero = Dungeon.hero;
		if (hero == null || hero.buff(PetWhistle.ReviveCooldown.class) != null) {
			return null;
		}
		PetBond bond = hero.buff(PetBond.class);
		if (bond == null) {
			return null;
		}
		PetWhistle whistle = PetWhistle.get();
		if (whistle != null) {
			bond.quality = whistle.quality();
			bond.appearance = whistle.appearance();
		}
		return bond;
	}

	public static float expMultiplier() {
		return multiplier(PetAlly.Skill.EXPERIENCE);
	}

	public static float htMultiplier() {
		return multiplier(PetAlly.Skill.HEALTH);
	}

	public static float accuracyMultiplier() {
		return multiplier(PetAlly.Skill.ACCURACY);
	}

	public static float evasionMultiplier() {
		return multiplier(PetAlly.Skill.EVASION);
	}

	public static float attackMultiplier() {
		return multiplier(PetAlly.Skill.ATTACK);
	}

	public static float speedMultiplier() {
		return multiplier(PetAlly.Skill.SPEED);
	}

	public static float regenMultiplier() {
		return multiplier(PetAlly.Skill.REGEN);
	}

	public static float critMultiplier() {
		return multiplier(PetAlly.Skill.CRIT);
	}

	public static int strengthBonus() {
		return flat(PetAlly.Skill.STRENGTH);
	}

	public static int armorBonus() {
		return flat(PetAlly.Skill.ARMOR);
	}

	public static String activeBonusText() {
		PetBond bond = get();
		if (bond == null) {
			return "";
		}
		return bonusText(bond.appearance, bond.quality);
	}

	public static String bonusText(PetAlly.Appearance appearance, PetAlly.Quality quality) {
		if (appearance == null || quality == null || appearance.skill == null) {
			return "";
		}
		int percent = percentBonus(quality);
		int flat = flatBonus(quality);
		return Messages.get(PetAlly.class, "bonus_" + appearance.skill.key(),
				appearance.skill == PetAlly.Skill.STRENGTH || appearance.skill == PetAlly.Skill.ARMOR
						? flat : percent);
	}

	public static int percentBonus(PetAlly.Quality quality) {
		if (quality == null) quality = PetAlly.Quality.COMMON;
		return Math.round(BASE_PERCENT * quality.statMul * 100f);
	}

	public static int flatBonus(PetAlly.Quality quality) {
		if (quality == null) quality = PetAlly.Quality.COMMON;
		return Math.max(1, Math.round(quality.statMul));
	}

	private static float multiplier(PetAlly.Skill skill) {
		PetBond bond = get();
		if (bond == null || bond.appearance == null || bond.appearance.skill != skill) {
			return 1f;
		}
		return 1f + BASE_PERCENT * bond.quality.statMul;
	}

	private static int flat(PetAlly.Skill skill) {
		PetBond bond = get();
		if (bond == null || bond.appearance == null || bond.appearance.skill != skill) {
			return 0;
		}
		return flatBonus(bond.quality);
	}

	@Override
	public boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		if (appearance == null || appearance.skill == null) {
			return BuffIndicator.BLESS;
		}
		switch (appearance.skill) {
			case EXPERIENCE: return BuffIndicator.UPGRADE;
			case HEALTH:     return BuffIndicator.HEART;
			case ACCURACY:   return BuffIndicator.BLESS;
			case ARMOR:      return BuffIndicator.ARMOR;
			case EVASION:    return BuffIndicator.MOMENTUM;
			case REGEN:      return BuffIndicator.HEALING;
			case ATTACK:     return BuffIndicator.WEAPON;
			case STRENGTH:   return BuffIndicator.RAGE;
			case SPEED:      return BuffIndicator.HASTE;
			case CRIT:       return BuffIndicator.COMBO;
			default:         return BuffIndicator.BLESS;
		}
	}

	@Override
	public void tintIcon(Image icon) {
		if (quality == null) return;
		int tint = quality.tint;
		icon.hardlight(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f);
	}

	@Override
	public String name() {
		if (appearance == null) {
			return super.name();
		}
		return Messages.get(this, "name", appearance.title());
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc",
				appearance == null ? "?" : appearance.title(),
				quality == null ? "?" : quality.title(),
				bonusText(appearance, quality),
				quality == null ? 100 : Math.round(quality.statMul * 100f));
	}

	private static final String QUALITY = "quality";
	private static final String APPEARANCE = "appearance";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (quality != null) bundle.put(QUALITY, quality.name());
		if (appearance != null) bundle.put(APPEARANCE, appearance.name());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(QUALITY)) {
			try {
				quality = PetAlly.Quality.valueOf(bundle.getString(QUALITY));
			} catch (Exception ignored) {
				quality = PetAlly.Quality.COMMON;
			}
		}
		if (bundle.contains(APPEARANCE)) {
			try {
				appearance = PetAlly.Appearance.valueOf(bundle.getString(APPEARANCE));
			} catch (Exception ignored) {
				appearance = PetAlly.Appearance.RAT;
			}
		}
	}
}
