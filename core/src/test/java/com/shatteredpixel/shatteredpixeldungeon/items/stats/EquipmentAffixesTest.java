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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EquipmentAffixesTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xA771B0L);
	}

	@Test
	@DisplayName("legacy equipment remains valid without generated affixes")
	public void legacyEquipmentHasNoAffixes() {
		Sword original = new Sword();
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		Sword restored = new Sword();
		restored.restoreFromBundle(bundle);

		assertFalse(restored.affixes().rolled());
		assertTrue(restored.affixes().isEmpty());
	}

	@Test
	@DisplayName("affixes survive item saving and scale with item upgrades")
	public void affixesSaveAndScale() {
		Sword original = new Sword();
		original.affixes().set(CombatStat.ATTACK_POWER, 4);
		original.affixes().set(CombatStat.CRIT_DAMAGE, 1_000);
		original.upgrade(2);

		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);
		Sword restored = new Sword();
		restored.restoreFromBundle(bundle);

		assertEquals(EquipmentRarity.RARE, restored.affixes().rarity());
		assertEquals(6, restored.affixValue(CombatStat.ATTACK_POWER));
		assertEquals(1_400, restored.affixValue(CombatStat.CRIT_DAMAGE));
	}

	@Test
	@DisplayName("new random weapons and armor roll their affix data exactly once")
	public void generatedEquipmentRollsAffixes() {
		Sword weapon = (Sword) new Sword().random();
		Armor armor = (Armor) new LeatherArmor().random();

		assertTrue(weapon.affixes().rolled());
		assertTrue(armor.affixes().rolled());
	}

	@Test
	@DisplayName("class armor conversion preserves armor affixes")
	public void classArmorPreservesAffixes() {
		LeatherArmor armor = new LeatherArmor();
		armor.affixes().set(CombatStat.MAX_HEALTH, 8);

		ClassArmor classArmor = ClassArmor.upgrade(Dungeon.hero, armor);

		assertEquals(8, classArmor.affixValue(CombatStat.MAX_HEALTH));
	}

	@Test
	@DisplayName("virtual item copies preserve affixes without sharing mutable state")
	public void virtualItemCopiesAffixes() {
		Sword original = new Sword();
		original.affixes().set(CombatStat.ACCURACY, 500);

		Item copy = original.virtual();
		copy.affixes().set(CombatStat.ACCURACY, 900);

		assertEquals(500, original.affixValue(CombatStat.ACCURACY));
		assertEquals(900, copy.affixValue(CombatStat.ACCURACY));
	}
}
