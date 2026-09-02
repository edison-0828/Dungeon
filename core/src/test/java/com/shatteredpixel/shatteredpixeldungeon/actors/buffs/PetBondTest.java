/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PetAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetBondTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xB0D1L);
	}

	@Test
	@DisplayName("each appearance grants a different owner skill")
	public void appearancePicksTheSkill() {
		assertEquals(PetAlly.Skill.EXPERIENCE, PetAlly.Appearance.RAT.skill);
		assertEquals(PetAlly.Skill.HEALTH, PetAlly.Appearance.ALBINO.skill);
		assertEquals(PetAlly.Skill.ACCURACY, PetAlly.Appearance.SNAKE.skill);
		assertEquals(PetAlly.Skill.ARMOR, PetAlly.Appearance.CRAB.skill);
		assertEquals(PetAlly.Skill.EVASION, PetAlly.Appearance.BAT.skill);
		assertEquals(PetAlly.Skill.REGEN, PetAlly.Appearance.SLIME.skill);
		assertEquals(PetAlly.Skill.ATTACK, PetAlly.Appearance.BEE.skill);
		assertEquals(PetAlly.Skill.STRENGTH, PetAlly.Appearance.GNOLL.skill);
		assertEquals(PetAlly.Skill.SPEED, PetAlly.Appearance.SWARM.skill);
		assertEquals(PetAlly.Skill.CRIT, PetAlly.Appearance.SHEEP.skill);
	}

	@Test
	@DisplayName("quality scales the common 8% rate up to legendary")
	public void qualityScalesThePercent() {
		assertEquals(8, PetBond.percentBonus(PetAlly.Quality.COMMON));
		assertEquals(9, PetBond.percentBonus(PetAlly.Quality.UNCOMMON));
		assertEquals(10, PetBond.percentBonus(PetAlly.Quality.RARE));
		assertEquals(12, PetBond.percentBonus(PetAlly.Quality.LEGENDARY));
		assertEquals(1, PetBond.flatBonus(PetAlly.Quality.COMMON));
		assertEquals(2, PetBond.flatBonus(PetAlly.Quality.LEGENDARY));
	}

	@Test
	@DisplayName("a rat companion boosts experience, more at higher quality")
	public void ratBoostsExperience() {
		bind(PetAlly.Quality.COMMON, PetAlly.Appearance.RAT);
		assertEquals(1.08f, PetBond.expMultiplier(), 0.0001f);
		bind(PetAlly.Quality.LEGENDARY, PetAlly.Appearance.RAT);
		assertEquals(1.12f, PetBond.expMultiplier(), 0.0001f);
		assertEquals(1f, PetBond.attackMultiplier(), 0.0001f);
	}

	@Test
	@DisplayName("a gnoll companion adds strength, a bee adds attack")
	public void gnollAndBeeOwnerBonuses() {
		Hero hero = Dungeon.hero;
		int baseStr = hero.STR;
		bind(PetAlly.Quality.COMMON, PetAlly.Appearance.GNOLL);
		assertEquals(1, PetBond.strengthBonus());
		assertEquals(baseStr + 1, hero.STR());

		bind(PetAlly.Quality.LEGENDARY, PetAlly.Appearance.BEE);
		assertEquals(0, PetBond.strengthBonus());
		assertEquals(baseStr, hero.STR());
		assertEquals(1.12f, PetBond.attackMultiplier(), 0.0001f);
	}

	@Test
	@DisplayName("an albino companion raises max HP")
	public void albinoRaisesHealth() {
		int before = Dungeon.hero.HT;
		bind(PetAlly.Quality.COMMON, PetAlly.Appearance.ALBINO);
		assertTrue(Dungeon.hero.HT > before);
		assertEquals(Math.round(before * 1.08f), Dungeon.hero.HT);
	}

	@Test
	@DisplayName("the bond pauses while the companion is recovering")
	public void cooldownPausesTheBond() {
		bind(PetAlly.Quality.RARE, PetAlly.Appearance.RAT);
		assertEquals(1f + PetBond.BASE_PERCENT * PetAlly.Quality.RARE.statMul, PetBond.expMultiplier(), 0.0001f);

		Buff.prolong(Dungeon.hero, PetWhistle.ReviveCooldown.class, 10f);
		PetBond.refresh(Dungeon.hero);
		assertEquals(1f, PetBond.expMultiplier(), 0.0001f);
		assertEquals(0, PetBond.strengthBonus());
	}

	@Test
	@DisplayName("the owner bond grows as the hero levels up")
	public void bondGrowsWithHeroLevel() {
		bind(PetAlly.Quality.COMMON, PetAlly.Appearance.RAT);
		assertEquals(1.08f, PetBond.expMultiplier(), 0.0001f);
		assertEquals(8, PetBond.percentBonus(PetAlly.Quality.COMMON));

		Dungeon.hero.lvl = 11;
		assertEquals(1.40f, PetBond.levelScale(), 0.0001f);
		assertEquals(11, PetBond.percentBonus(PetAlly.Quality.COMMON));
		assertEquals(1f + PetBond.BASE_PERCENT * 1.40f, PetBond.expMultiplier(), 0.0001f);

		bind(PetAlly.Quality.COMMON, PetAlly.Appearance.GNOLL);
		Dungeon.hero.lvl = 14;
		assertEquals(2, PetBond.flatBonus(PetAlly.Quality.COMMON));
		assertEquals(2, PetBond.strengthBonus());
	}

	@Test
	@DisplayName("a sheep companion raises crit chance, and only while it is the bound pet")
	public void sheepBoostsCrit() {
		bind(PetAlly.Quality.COMMON, PetAlly.Appearance.CRAB);
		int baseline = Dungeon.hero.combatStats().critChance();
		assertEquals(1f, PetBond.critMultiplier(), 0.0001f);

		//legendary is 8% * 1.5 = 12%, and crit is measured in basis points
		bind(PetAlly.Quality.LEGENDARY, PetAlly.Appearance.SHEEP);
		assertEquals(1.12f, PetBond.critMultiplier(), 0.0001f);
		assertEquals(baseline + 1200, Dungeon.hero.combatStats().critChance());
	}

	private static void bind(PetAlly.Quality quality, PetAlly.Appearance appearance) {
		PetWhistle whistle = Dungeon.hero.belongings.getItem(PetWhistle.class);
		if (whistle == null) {
			whistle = new PetWhistle();
			assertTrue(whistle.collect());
		}
		whistle.bind(quality, appearance);
	}
}
