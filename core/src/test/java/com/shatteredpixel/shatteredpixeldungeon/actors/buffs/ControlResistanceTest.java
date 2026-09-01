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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ControlResistanceTest {

	@BeforeEach
	public void freshRun() {
		HeadlessDungeon.startRun(0xC07A01L);
	}

	@Test
	@DisplayName("recovering from paralysis briefly halves repeat hard-control duration")
	public void heroGetsRepeatControlResistance() {
		Paralysis paralysis = Buff.affect(Dungeon.hero, Paralysis.class, 4f);
		paralysis.detach();

		assertNotNull(Dungeon.hero.buff(ControlResistance.class));
		assertEquals(0.5f, Dungeon.hero.resist(Paralysis.class));
		assertEquals(0.5f, Dungeon.hero.resist(Frost.class));
	}

	@Test
	@DisplayName("hero-only protection does not weaken crowd control used against enemies")
	public void enemiesDoNotGetProtection() {
		Rat rat = new Rat();
		Paralysis paralysis = Buff.affect(rat, Paralysis.class, 4f);
		paralysis.detach();

		assertNull(rat.buff(ControlResistance.class));
		assertEquals(1f, rat.resist(Paralysis.class));
	}

	@Test
	@DisplayName("negative buffs expose a concise effect summary and severity")
	public void negativeBuffGuidanceIsAvailable() {
		Cripple cripple = new Cripple();
		Paralysis paralysis = new Paralysis();

		assertFalse(cripple.summary().isEmpty());
		assertEquals(Buff.debuffSeverity.MAJOR, cripple.severity);
		assertEquals(Buff.debuffSeverity.CRITICAL, paralysis.severity);
	}
}
