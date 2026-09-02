/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobAttackProfileTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xA77AC1L);
	}

	@Test
	@DisplayName("rats are physical and hit softer than crabs")
	public void ratIsWeakerPhysicalThanCrab() {
		Rat rat = new Rat();
		Crab crab = new Crab();

		assertEquals(MobAttackType.PHYSICAL, rat.attackType());
		assertEquals(MobAttackType.PHYSICAL, crab.attackType());
		assertTrue(MobAttackProfile.typicalDamage(rat) < MobAttackProfile.typicalDamage(crab));
		assertTrue(MobAttackProfile.intensity(rat) < MobAttackProfile.intensity(crab)
				|| MobAttackProfile.intensity(crab) == MobAttackProfile.MAX_INTENSITY);
	}

	@Test
	@DisplayName("fire elementals are tagged as fire")
	public void fireElementalIsFire() {
		assertEquals(MobAttackType.FIRE, new Elemental.FireElemental().attackType());
		assertEquals(MobAttackType.FROST, new Elemental.FrostElemental().attackType());
		assertEquals(MobAttackType.SHOCK, new Elemental.ShockElemental().attackType());
	}

	@Test
	@DisplayName("intensity stays inside the visible tint range")
	public void intensityIsClamped() {
		float intensity = MobAttackProfile.intensity(new Rat());
		assertTrue(intensity >= MobAttackProfile.MIN_INTENSITY);
		assertTrue(intensity <= MobAttackProfile.MAX_INTENSITY);
		assertTrue(MobAttackProfile.intensity(new Scorpio()) <= MobAttackProfile.MAX_INTENSITY);
	}

	@Test
	@DisplayName("unlisted mobs still resolve a physical tint")
	public void unlistedMobDoesNotCrash() {
		UnlistedMob mob = new UnlistedMob();
		assertEquals(MobAttackType.PHYSICAL, mob.attackType());
		assertTrue(MobAttackProfile.typicalDamage(mob) > 0);
		int tint = MobAttackProfile.tintColor(mob);
		assertTrue(tint >= 0);
	}

	@Test
	@DisplayName("NPCs and blue shamans follow the skip and type rules")
	public void npcSkipAndShamanTypes() {
		assertFalse(MobAttackProfile.tintsSprite(new Ghost()));
		assertTrue(MobAttackProfile.tintsSprite(new Rat()));
		assertEquals(MobAttackType.MAGIC, new Shaman.RedShaman().attackType());
		assertEquals(MobAttackType.SHOCK, new Shaman.BlueShaman().attackType());
		assertEquals(MobAttackType.ACID, new CausticSlime().attackType());
	}

	private static class UnlistedMob extends Mob {
		{
			HP = HT = 30;
			alignment = Char.Alignment.ENEMY;
		}
	}
}
