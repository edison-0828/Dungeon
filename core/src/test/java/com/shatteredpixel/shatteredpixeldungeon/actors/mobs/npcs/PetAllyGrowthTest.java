/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetAllyGrowthTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0x7E71L);
	}

	@Test
	@DisplayName("companion HP and damage rise with each hero level")
	public void combatStatsScaleWithHeroLevel() {
		assertEquals(18, PetAlly.scaledHT(PetAlly.Quality.COMMON, 1));
		assertEquals(24, PetAlly.scaledHT(PetAlly.Quality.COMMON, 2));
		assertEquals(72, PetAlly.scaledHT(PetAlly.Quality.COMMON, 10));
		assertEquals(1, PetAlly.scaledMinDamage(PetAlly.Quality.COMMON, 1));
		assertEquals(5, PetAlly.scaledMaxDamage(PetAlly.Quality.COMMON, 1));
		assertEquals(6, PetAlly.scaledMinDamage(PetAlly.Quality.COMMON, 10));
		assertEquals(16, PetAlly.scaledMaxDamage(PetAlly.Quality.COMMON, 10));

		PetAlly pet = new PetAlly();
		pet.setIdentity(PetAlly.Quality.COMMON, PetAlly.Appearance.RAT);
		int htAtOne = pet.HT;
		int maxAtOne = pet.maxDamage();

		Dungeon.hero.lvl = 10;
		pet.updateStats();
		assertTrue(pet.HT > htAtOne);
		assertTrue(pet.maxDamage() > maxAtOne);
		assertEquals(PetAlly.scaledHT(PetAlly.Quality.COMMON, 10), pet.HT);
	}

	@Test
	@DisplayName("hero level-up immediately refreshes a summoned companion")
	public void levelUpRefreshesSpawnedPet() {
		PetWhistle whistle = bind(PetAlly.Quality.COMMON, PetAlly.Appearance.RAT);
		PetAlly pet = new PetAlly();
		pet.setIdentity(PetAlly.Quality.COMMON, PetAlly.Appearance.RAT);
		whistle.linkPet(pet);

		int htBefore = pet.HT;
		int hpBefore = pet.HP;
		assertEquals(1, Dungeon.hero.lvl);

		Dungeon.hero.earnExp(Dungeon.hero.maxExp(), PotionOfExperience.class);

		assertEquals(2, Dungeon.hero.lvl);
		assertEquals(PetAlly.scaledHT(PetAlly.Quality.COMMON, 2), pet.HT);
		assertTrue(pet.HT > htBefore);
		assertEquals(hpBefore + (pet.HT - htBefore), pet.HP);
	}

	private static PetWhistle bind(PetAlly.Quality quality, PetAlly.Appearance appearance) {
		PetWhistle whistle = Dungeon.hero.belongings.getItem(PetWhistle.class);
		if (whistle == null) {
			whistle = new PetWhistle();
			assertTrue(whistle.collect());
		}
		whistle.bind(quality, appearance);
		return whistle;
	}
}
