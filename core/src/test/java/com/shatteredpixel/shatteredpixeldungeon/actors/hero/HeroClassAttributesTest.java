/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeroClassAttributesTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xA771B07EL);
	}

	@Test
	@DisplayName("each class starts with distinct strength and intellect")
	public void classStartingStats() {
		assertEquals(HeroClass.PrimaryStat.STRENGTH, HeroClass.WARRIOR.primaryStat());
		assertEquals(12, HeroClass.WARRIOR.startingSTR());
		assertEquals(8, HeroClass.WARRIOR.startingINT());
		assertEquals(CombatStat.FIRE_POWER, HeroClass.WARRIOR.affinityStat());

		assertEquals(HeroClass.PrimaryStat.INTELLECT, HeroClass.MAGE.primaryStat());
		assertEquals(10, HeroClass.MAGE.startingSTR());
		assertEquals(12, HeroClass.MAGE.startingINT());
		assertEquals(CombatStat.MAGIC_POWER, HeroClass.MAGE.affinityStat());

		assertEquals(HeroClass.PrimaryStat.STRENGTH, HeroClass.ROGUE.primaryStat());
		assertEquals(10, HeroClass.ROGUE.startingSTR());
		assertEquals(10, HeroClass.ROGUE.startingINT());

		assertEquals(11, HeroClass.HUNTRESS.startingINT());
		assertEquals(11, HeroClass.DUELIST.startingSTR());
		assertEquals(HeroClass.PrimaryStat.INTELLECT, HeroClass.CLERIC.primaryStat());
		assertEquals(11, HeroClass.CLERIC.startingINT());

		assertEquals(12, Dungeon.hero.STR);
		assertEquals(8, Dungeon.hero.INT);
	}

	@Test
	@DisplayName("heroes start with a stack of identify scrolls")
	public void startsWithIdentifyScrolls() {
		ScrollOfIdentify scrolls = Dungeon.hero.belongings.getItem(ScrollOfIdentify.class);
		assertTrue(scrolls != null && scrolls.quantity() >= 3,
				"expected at least 3 identify scrolls, got "
						+ (scrolls == null ? 0 : scrolls.quantity()));
	}

	@Test
	@DisplayName("warriors gain strength every level and intellect every third")
	public void warriorLevelGrowth() {
		assertEquals(12, Dungeon.hero.STR);
		assertEquals(8, Dungeon.hero.INT);

		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);
		assertEquals(2, Dungeon.hero.lvl);
		assertEquals(13, Dungeon.hero.STR);
		assertEquals(8, Dungeon.hero.INT);

		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);
		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);
		assertEquals(4, Dungeon.hero.lvl);
		assertEquals(15, Dungeon.hero.STR);
		assertEquals(9, Dungeon.hero.INT);
	}

	@Test
	@DisplayName("mages gain intellect every level and strength every third")
	public void mageLevelGrowth() {
		Dungeon.hero.heroClass = HeroClass.MAGE;
		Dungeon.hero.applyClassAttributes();
		assertEquals(10, Dungeon.hero.STR);
		assertEquals(12, Dungeon.hero.INT);

		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);
		assertEquals(2, Dungeon.hero.lvl);
		assertEquals(10, Dungeon.hero.STR);
		assertEquals(13, Dungeon.hero.INT);
	}

	@Test
	@DisplayName("spell damage scales with intellect and class elemental affinity")
	public void spellDamageUsesIntellectAndAffinity() {
		WandOfMagicMissile missile = new WandOfMagicMissile();
		int warriorHit = Dungeon.hero.combatStats().modifyOutgoingDamage(10, missile);
		assertTrue(warriorHit < 10, "a warrior's low intellect should weaken magic missile");

		Dungeon.hero.heroClass = HeroClass.MAGE;
		Dungeon.hero.applyClassAttributes();
		int mageHit = Dungeon.hero.combatStats().modifyOutgoingDamage(10, missile);
		assertTrue(mageHit > warriorHit);
		assertTrue(mageHit > 10, "a mage should deal bonus magic damage");

		WandOfFireblast fire = new WandOfFireblast();
		Dungeon.hero.heroClass = HeroClass.WARRIOR;
		Dungeon.hero.applyClassAttributes();
		int warriorFire = Dungeon.hero.combatStats().modifyOutgoingDamage(10, fire);
		int warriorMagic = Dungeon.hero.combatStats().modifyOutgoingDamage(10, missile);
		assertTrue(warriorFire > warriorMagic, "warriors should deal more fire than untyped magic");
	}

	@Test
	@DisplayName("hero-caused fire and poison scale, environmental damage does not")
	public void heroElementalProcsScale() {
		Blazing blazing = new Blazing();
		int warriorBlaze = Dungeon.hero.combatStats().modifyOutgoingDamage(10, blazing);
		assertTrue(warriorBlaze > 10, "warrior fire affinity should boost blazing");

		Burning trapFire = new Burning();
		assertEquals(10, Dungeon.hero.combatStats().modifyOutgoingDamage(10, trapFire));

		Burning heroFire = new Burning();
		heroFire.markHeroSourced();
		assertTrue(Dungeon.hero.combatStats().modifyOutgoingDamage(10, heroFire) > 10);

		Dungeon.hero.heroClass = HeroClass.ROGUE;
		Dungeon.hero.applyClassAttributes();
		Poison trapPoison = new Poison();
		assertEquals(10, Dungeon.hero.combatStats().modifyOutgoingDamage(10, trapPoison));
		Poison dartPoison = new Poison();
		dartPoison.markHeroSourced();
		assertTrue(Dungeon.hero.combatStats().modifyOutgoingDamage(10, dartPoison) > 10);
	}

	@Test
	@DisplayName("mages gain intellect from the strength potion, warriors gain strength")
	public void potionRaisesPrimaryStat() {
		assertEquals(HeroClass.PrimaryStat.STRENGTH, Dungeon.hero.gainPrimaryStat());
		assertEquals(13, Dungeon.hero.STR);

		Dungeon.hero.heroClass = HeroClass.MAGE;
		Dungeon.hero.applyClassAttributes();
		assertEquals(HeroClass.PrimaryStat.INTELLECT, Dungeon.hero.gainPrimaryStat());
		assertEquals(13, Dungeon.hero.INT);
		assertEquals(10, Dungeon.hero.STR);
	}

	@Test
	@DisplayName("wand descriptions show damage after intellect and affinity")
	public void wandDescriptionUsesScaledDamage() {
		Dungeon.hero.heroClass = HeroClass.MAGE;
		Dungeon.hero.applyClassAttributes();
		WandOfMagicMissile missile = new WandOfMagicMissile();
		assertTrue(missile.displayedMax() > missile.max());
	}
}
