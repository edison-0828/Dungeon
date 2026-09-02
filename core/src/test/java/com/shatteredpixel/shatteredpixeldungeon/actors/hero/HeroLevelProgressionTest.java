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

import com.shatteredpixel.shatteredpixeldungeon.BeginnerAid;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeroLevelProgressionTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0x57A3A6L);
		//Keep these tests focused on normal XP thresholds.
		Dungeon.hero.buff(BeginnerAid.Tracker.class).detach();
	}

	@Test
	@DisplayName("every hero level grants exactly one base strength")
	public void levelUpGrantsStrength() {
		int startingStrength = Dungeon.hero.STR;

		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);

		assertEquals(2, Dungeon.hero.lvl);
		assertEquals(startingStrength + 1, Dungeon.hero.STR);
	}

	@Test
	@DisplayName("a single XP award that crosses multiple levels grants strength per level")
	public void multiLevelGainGrantsStrengthPerLevel() {
		int startingStrength = Dungeon.hero.STR;
		int experience = Hero.maxExp(1) + Hero.maxExp(2) + Hero.maxExp(3);

		Dungeon.hero.earnExp(experience, PotionOfExperience.class);

		assertEquals(4, Dungeon.hero.lvl);
		assertEquals(startingStrength + 3, Dungeon.hero.STR);
	}

	@Test
	@DisplayName("experience gained at the level cap does not grant extra strength")
	public void levelCapDoesNotGrantStrength() {
		int experienceToCap = 0;
		for (int level = 1; level < Hero.MAX_LEVEL; level++) {
			experienceToCap += Hero.maxExp(level);
		}
		Dungeon.hero.earnExp(experienceToCap, PotionOfExperience.class);
		int strengthAtCap = Dungeon.hero.STR;

		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);

		assertEquals(Hero.MAX_LEVEL, Dungeon.hero.lvl);
		assertEquals(strengthAtCap, Dungeon.hero.STR);
	}
}
