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
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

		assertEquals(EquipmentRarity.EXCELLENT, restored.affixes().rarity());
		assertEquals(6, restored.affixValue(CombatStat.ATTACK_POWER));
		assertEquals(1_400, restored.affixValue(CombatStat.CRIT_DAMAGE));
	}

	@Test
	@DisplayName("six rarity tiers have increasing affix counts")
	public void sixTiersHaveExpectedAffixCounts() {
		assertEquals(0, EquipmentRarity.NORMAL.affixCount);
		assertEquals(1, EquipmentRarity.FINE.affixCount);
		assertEquals(2, EquipmentRarity.EXCELLENT.affixCount);
		assertEquals(3, EquipmentRarity.EPIC.affixCount);
		assertEquals(4, EquipmentRarity.LEGENDARY.affixCount);
		assertEquals(5, EquipmentRarity.MYTHIC.affixCount);
		assertEquals(EquipmentRarity.EXCELLENT, EquipmentRarity.fromAffixCount(2));
		assertEquals(EquipmentRarity.MYTHIC, EquipmentRarity.fromAffixCount(5));
		assertFalse(new EquipmentAffixes().hasUniqueEffect());
	}

	@Test
	@DisplayName("legacy RARE rarity maps to excellent on load")
	public void legacyRareMapsToExcellent() {
		EquipmentAffixes affixes = new EquipmentAffixes();
		Bundle bundle = new Bundle();
		bundle.put("rolled", true);
		bundle.put("rarity", "RARE");
		bundle.put("stat_ACCURACY", 500);
		bundle.put("stat_EVASION", 400);
		affixes.restoreFromBundle(bundle);

		assertEquals(EquipmentRarity.EXCELLENT, affixes.rarity());
		assertEquals(500, affixes.value(CombatStat.ACCURACY, 0));
		assertEquals(400, affixes.value(CombatStat.EVASION, 0));
	}

	@Test
	@DisplayName("early floors never roll legendary or mythic gear")
	public void earlyFloorsSkipTopRarities() {
		for (int i = 0; i < 200; i++) {
			EquipmentRarity rarity = EquipmentAffixes.rollRarity(0);
			assertTrue(rarity.ordinal() <= EquipmentRarity.EPIC.ordinal(), rarity.name());
		}
	}

	@Test
	@DisplayName("isolated affix rolls do not consume the caller's RNG")
	public void isolatedRollDoesNotConsumeCallerRng() {
		Random.pushGenerator(42L);
		new EquipmentAffixes().rollIsolated(EquipmentAffixes.Family.WAND, 3, 0);
		int afterIsolated = Random.Int(10_000);
		Random.popGenerator();

		Random.pushGenerator(42L);
		int untouched = Random.Int(10_000);
		Random.popGenerator();

		assertEquals(untouched, afterIsolated);
	}

	@Test
	@DisplayName("missiles, wands and rings roll affixes")
	public void missilesWandsAndRingsRollAffixes() {
		assertTrue(new ThrowingStone().random().affixes().rolled());
		assertTrue(new WandOfMagicMissile().random().affixes().rolled());
		assertTrue(new RingOfAccuracy().random().affixes().rolled());
	}

	@Test
	@DisplayName("different missile affixes do not stack together")
	public void missilesWithDifferentAffixesDoNotStack() {
		ThrowingStone first = new ThrowingStone();
		ThrowingStone second = new ThrowingStone();
		second.setID = first.setID;
		first.affixes().set(CombatStat.ACCURACY, 500);
		second.affixes().set(CombatStat.ATTACK_POWER, 3);

		assertFalse(first.isSimilar(second));

		ThrowingStone copy = new ThrowingStone();
		copy.setID = first.setID;
		copy.affixes().copyFrom(first.affixes());
		assertTrue(first.isSimilar(copy));
	}

	@Test
	@DisplayName("shop stock strips rolled rarity back to normal")
	public void shopStockIsNormalRarity() {
		Sword weapon = (Sword) new Sword().random();
		assertTrue(weapon.affixes().rolled());
		weapon.affixes().resetToNormal();
		assertEquals(EquipmentRarity.NORMAL, weapon.affixes().rarity());
		assertTrue(weapon.affixes().isEmpty());

		Dungeon.depth = 6;
		int identifyScrolls = 0;
		for (Item item : ShopStock.stock()) {
			if (item instanceof EquipableItem || item instanceof Wand) {
				assertEquals(EquipmentRarity.NORMAL, item.affixes().rarity(), item.getClass().getSimpleName());
				assertTrue(item.affixes().isEmpty(), item.getClass().getSimpleName());
			}
			if (item instanceof ScrollOfIdentify) {
				identifyScrolls += item.quantity();
			}
		}
		assertTrue(identifyScrolls >= 3, "shop should stock at least 3 identify scrolls, got " + identifyScrolls);
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

	@Test
	@DisplayName("affix score normalizes flat damage against percent bonuses")
	public void affixScoreNormalizesFlatAndPercentStats() {
		EquipmentAffixes affixes = new EquipmentAffixes();
		affixes.set(CombatStat.ATTACK_POWER, 4);
		affixes.set(CombatStat.ACCURACY, 500);
		assertEquals(900, EquipmentAffixScore.score(affixes, 0, null));
	}

	@Test
	@DisplayName("class affinity only lightly boosts the matching element")
	public void affinityLightlyBoostsMatchingElement() {
		EquipmentAffixes affixes = new EquipmentAffixes();
		affixes.set(CombatStat.FIRE_POWER, 1000);
		assertEquals(1150, EquipmentAffixScore.score(affixes, 0, CombatStat.FIRE_POWER));
		assertEquals(1000, EquipmentAffixScore.score(affixes, 0, CombatStat.POISON_POWER));
	}

	@Test
	@DisplayName("unidentified equipment hides affix score")
	public void unidentifiedItemsHideAffixScore() {
		Sword sword = new Sword();
		sword.affixes().set(CombatStat.ATTACK_POWER, 4);
		assertEquals("", EquipmentAffixScore.description(sword));
		assertFalse(sword.info().contains("Affix score"));

		sword.levelKnown = true;
		assertTrue(EquipmentAffixScore.description(sword).contains("Affix score: 400"));
		assertTrue(sword.info().contains("Affix score: 400"));
	}

	@Test
	@DisplayName("identified gear shows score relative to the equipped slot")
	public void scoreComparesAgainstEquippedSlot() {
		Sword drop = new Sword();
		drop.levelKnown = true;
		drop.affixes().set(CombatStat.ATTACK_POWER, 6);

		assertNotNull(EquipmentAffixScore.compareTarget(drop));
		String description = EquipmentAffixScore.description(drop);
		assertTrue(description.contains("Affix score: 600"));
		assertTrue(description.contains("+600 vs equipped"));
	}

	@Test
	@DisplayName("wand and ring descriptions include affix score")
	public void wandAndRingInfoIncludeAffixScore() {
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.levelKnown = true;
		wand.affixes().set(CombatStat.MAGIC_POWER, 800);
		assertTrue(EquipmentAffixScore.description(wand).contains("Affix score: 800"));
		assertTrue(wand.info().contains("Affix score: 800"));

		RingOfAccuracy ring = new RingOfAccuracy();
		ring.levelKnown = true;
		ring.affixes().set(CombatStat.ACCURACY, 400);
		assertTrue(EquipmentAffixScore.description(ring).contains("Affix score: 400"));
		assertTrue(ring.info().contains("Affix score: 400"));
	}

	private static class ShopStock extends ShopRoom {
		static ArrayList<Item> stock() {
			return generateItems();
		}
	}
}
