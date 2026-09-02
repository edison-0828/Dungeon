/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeginnerAidTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xBEEFL);
	}

	@Test
	@DisplayName("the new player health pool adds room for mistakes without flattening progression")
	public void startingHealthIsModerate() {
		assertEquals(30, Hero.STARTING_HP);
		assertEquals(30, Dungeon.hero.HT);
	}

	@Test
	@DisplayName("combat easing only applies while the novice tracker is active")
	public void combatEasingIsGated() {
		Rat rat = new Rat();
		assertEquals(9, BeginnerAid.scaleEnemyDamage(10, rat));
		assertEquals(BeginnerAid.ENEMY_ACCURACY,
				BeginnerAid.enemyAccuracyFactor(rat, Dungeon.hero));

		Dungeon.hero.buff(BeginnerAid.Tracker.class).detach();
		assertEquals(10, BeginnerAid.scaleEnemyDamage(10, rat));
		assertEquals(1f, BeginnerAid.enemyAccuracyFactor(rat, Dungeon.hero));
	}

	@Test
	@DisplayName("the low-health rescue fires once and grants the promised barrier")
	public void safetyNetIsOneShot() {
		Dungeon.hero.HP = Dungeon.hero.HT / 4;
		BeginnerAid.trySafetyNet(Dungeon.hero);
		Barrier barrier = Dungeon.hero.buff(Barrier.class);
		assertEquals(BeginnerAid.SAFETY_SHIELD, barrier.shielding());

		barrier.decShield(BeginnerAid.SAFETY_SHIELD - 1);
		BeginnerAid.trySafetyNet(Dungeon.hero);
		assertEquals(1, barrier.shielding());
		assertTrue(Dungeon.hero.buff(BeginnerAid.Tracker.class).safetyNetUsed);
	}

	@Test
	@DisplayName("the first surprise bonus cannot be farmed repeatedly")
	public void surpriseRewardIsOneShot() {
		Rat rat = new Rat();
		assertEquals(15f, BeginnerAid.modifyHeroAttackDamage(Dungeon.hero, rat, 10f, true));
		assertEquals(10f, BeginnerAid.modifyHeroAttackDamage(Dungeon.hero, rat, 10f, true));
	}

	@Test
	@DisplayName("claiming a starter companion binds a whistle and clears the pending reward")
	public void starterChoiceIsExclusive() {
		assertTrue(BeginnerAid.starterRewardPending());
		BeginnerAid.PetOffer[] offers = BeginnerAid.starterPets();
		assertEquals(3, offers.length);
		BeginnerAid.PetOffer chosen = offers[0];

		BeginnerAid.claimStarterReward(0);

		assertFalse(BeginnerAid.starterRewardPending());
		PetWhistle whistle = Dungeon.hero.belongings.getItem(PetWhistle.class);
		assertTrue(whistle != null);
		assertEquals(chosen.quality, whistle.quality());
		assertEquals(chosen.appearance, whistle.appearance());
	}

	@Test
	@DisplayName("one-shot reward state survives a save bundle round trip")
	public void trackerPersists() {
		BeginnerAid.Tracker tracker = Dungeon.hero.buff(BeginnerAid.Tracker.class);
		tracker.starterRewardPending = false;
		tracker.safetyNetUsed = true;
		tracker.surpriseRewardUsed = true;
		tracker.doorwayRewardUsed = true;

		Bundle bundle = new Bundle();
		tracker.storeInBundle(bundle);
		BeginnerAid.Tracker restored = new BeginnerAid.Tracker();
		restored.restoreFromBundle(bundle);

		assertFalse(restored.starterRewardPending);
		assertTrue(restored.safetyNetUsed);
		assertTrue(restored.surpriseRewardUsed);
		assertTrue(restored.doorwayRewardUsed);
	}

	@Test
	@DisplayName("beating Goo ends every first-clear modifier immediately")
	public void firstClearEndsAid() {
		assertTrue(BeginnerAid.isActive());
		BeginnerAid.completeFirstClear();
		assertFalse(BeginnerAid.isActive());
	}

	@Test
	@DisplayName("unknown potion descriptions teach their real effect only during the novice run")
	public void novicePotionDescriptionRevealsEffect() {
		Potion potion = new PotionOfHaste();
		String effect = Messages.get(PotionOfHaste.class, "desc");
		assertFalse(potion.isKnown());
		assertTrue(potion.desc().contains(effect));

		BeginnerAid.completeFirstClear();
		assertEquals(Messages.get(Potion.class, "unknown_desc"), potion.desc());
	}
}
