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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public final class EquipmentAffixScore {

	public static final float AFFINITY_WEIGHT = 1.15f;

	private EquipmentAffixScore() {}

	public static boolean appliesTo(Item item) {
		return item instanceof Weapon
				|| item instanceof Armor
				|| item instanceof Wand
				|| item instanceof Ring;
	}

	public static int score(Item item) {
		if (item == null) return 0;
		return score(item.affixes(), item.buffedLvl(), affinityOf(Dungeon.hero));
	}

	public static int score(EquipmentAffixes affixes, int itemLevel, CombatStat affinity) {
		if (affixes == null) return 0;
		int total = 0;
		for (CombatStat stat : CombatStat.values()) {
			int value = affixes.value(stat, itemLevel);
			if (value <= 0) continue;
			float points = points(stat, value);
			if (affinity != null && stat == affinity) {
				points *= AFFINITY_WEIGHT;
			}
			total += Math.round(points);
		}
		return total;
	}

	public static String description(Item item) {
		if (item == null || !item.levelKnown || !appliesTo(item)) {
			return "";
		}
		String scoreLine = scoreLine(item);
		String list = item.affixes().info(item.buffedLvl());
		if (scoreLine.isEmpty()) return list;
		if (list.isEmpty()) return scoreLine;
		return scoreLine + "\n" + list;
	}

	public static Item compareTarget(Item item) {
		if (item == null || Dungeon.hero == null) return null;
		Belongings belongings = Dungeon.hero.belongings;
		if (belongings == null) return null;

		if (item instanceof Ring) {
			return weakerOther(item, ring(belongings.ring()), ring(belongings.misc()));
		}
		if (item instanceof Armor) {
			return other(item, belongings.armor());
		}
		if (item instanceof Wand) {
			KindOfWeapon weapon = belongings.weapon();
			if (weapon instanceof MagesStaff) {
				return other(item, ((MagesStaff) weapon).wand());
			}
			return null;
		}
		if (item instanceof MissileWeapon) {
			return weakerOther(item, missile(belongings.weapon()), missile(belongings.secondWep()));
		}
		if (item instanceof Weapon) {
			return weakerOther(item, melee(belongings.weapon()), melee(belongings.secondWep()));
		}
		return null;
	}

	private static String scoreLine(Item item) {
		int score = score(item);
		Item target = compareTarget(item);
		if (target == null) {
			return Messages.get(EquipmentAffixes.class, "score", score);
		}
		int delta = score - score(target);
		if (delta == 0) {
			return Messages.get(EquipmentAffixes.class, "score_same", score);
		}
		return Messages.get(EquipmentAffixes.class, "score_vs", score, signed(delta));
	}

	private static String signed(int delta) {
		return delta > 0 ? "+" + delta : Integer.toString(delta);
	}

	private static float points(CombatStat stat, int value) {
		switch (stat) {
			case ATTACK_POWER:
				return value * 100f;
			case MAX_HEALTH:
				return value * 50f;
			default:
				return value;
		}
	}

	private static CombatStat affinityOf(Hero hero) {
		if (hero == null || hero.heroClass == null) return null;
		return hero.heroClass.affinityStat();
	}

	private static Item other(Item self, Item candidate) {
		if (candidate == null || candidate == self || !candidate.levelKnown) {
			return null;
		}
		return candidate;
	}

	private static Item weakerOther(Item self, Item first, Item second) {
		Item a = other(self, first);
		Item b = other(self, second);
		if (a == null) return b;
		if (b == null) return a;
		return score(a) <= score(b) ? a : b;
	}

	private static Item ring(Item item) {
		return item instanceof Ring ? item : null;
	}

	private static Item missile(KindOfWeapon weapon) {
		return weapon instanceof MissileWeapon ? weapon : null;
	}

	private static Item melee(KindOfWeapon weapon) {
		if (weapon instanceof Weapon && !(weapon instanceof MissileWeapon)) {
			return weapon;
		}
		return null;
	}
}
