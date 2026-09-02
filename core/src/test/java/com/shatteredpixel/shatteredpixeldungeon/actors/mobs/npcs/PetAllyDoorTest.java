/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetAllyDoorTest {

	private Level level;
	private int door;
	private int corridor;
	private int room;

	@BeforeEach
	public void doorway() {
		HeadlessDungeon.startRun(0xD001L);
		Dungeon.depth = 1;
		level = Dungeon.newLevel();
		Dungeon.level = level;
		PathFinder.setMapSize(level.width(), level.height());
		Actor.clear();

		int[] pair = findDoorway();
		assertNotNull(pair, "depth 1 should have a door with empty tiles on both sides");
		corridor = pair[0];
		door = pair[1];
		room = pair[2];

		Dungeon.hero.pos = room;
		Actor.add(Dungeon.hero);

		if (level.heroFOV == null || level.heroFOV.length != level.length()) {
			level.heroFOV = new boolean[level.length()];
		}
	}

	@Test
	@DisplayName("a companion can path through an empty closed door")
	public void emptyDoorIsOnThePath() {
		PetAlly pet = placePet(corridor);

		com.watabou.utils.PathFinder.Path path = Dungeon.findPath(
				pet, room, level.passable, pet.fieldOfView, true);
		assertNotNull(path, "pathfinding should reach the far side of a closed door");
		assertTrue(path.contains(door), "the shortest path should step on the door");
	}

	@Test
	@DisplayName("a companion squeezed behind the hero in a doorway still enters the room")
	public void squeezesPastHeroInDoorway() {
		Dungeon.hero.pos = door;
		PetAlly pet = placePet(corridor);

		int step = pet.bypassToward(room);
		assertNotEquals(-1, step, "the companion should find a cell on the far side of the door");
		assertEquals(room, step);
	}

	private PetAlly placePet(int pos) {
		PetAlly pet = new PetAlly();
		pet.pos = pos;
		pet.fieldOfView = new boolean[level.length()];
		Arrays.fill(pet.fieldOfView, true);
		Actor.add(pet);
		level.mobs.add(pet);
		return pet;
	}

	private int[] findDoorway() {
		for (int cell = 0; cell < level.length(); cell++) {
			if (level.map[cell] != Terrain.DOOR) {
				continue;
			}
			for (int i : PathFinder.NEIGHBOURS4) {
				int a = cell + i;
				int b = cell - i;
				if (!level.insideMap(a) || !level.insideMap(b)) {
					continue;
				}
				if (level.passable[a] && level.passable[b]
						&& Actor.findChar(a) == null && Actor.findChar(b) == null) {
					return new int[]{a, cell, b};
				}
			}
		}
		return null;
	}
}
