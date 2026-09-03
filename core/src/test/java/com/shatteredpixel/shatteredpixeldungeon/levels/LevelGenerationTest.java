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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.test.HeadlessDungeon;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on procedurally generated floors.
 *
 * <p>A generator that occasionally produces an unwinnable floor is the worst class of bug this game
 * can have: it only shows up for one player on one seed, and by the time it is reported the run is
 * gone. These tests generate real floors and assert the properties a floor must have for a run to be
 * completable at all — the exit is reachable, nothing is entombed in solid rock, the tile flags agree
 * with the tiles, and the same seed always yields the same dungeon.
 */
public class LevelGenerationTest {

	//fixed so that a failure is always reproducible; the multi-seed sweep below covers the rest
	private static final long REFERENCE_SEED = 0x5EEDL;

	private static final int FIRST_DEPTH = 1;
	private static final int AMULET_DEPTH = 26;

	/** Every floor of one full run on {@link #REFERENCE_SEED}, generated once and shared. */
	private static Map<Integer, Level> referenceRun;

	@BeforeAll
	public static void generateReferenceRun() {
		referenceRun = descend(REFERENCE_SEED);
	}

	static IntStream allDepths() {
		return IntStream.rangeClosed(FIRST_DEPTH, AMULET_DEPTH);
	}

	private static Level reference(int depth) {
		Level level = referenceRun.get(depth);
		assertNotNull(level, "no level was generated for depth " + depth);
		return level;
	}

	/**
	 * Generates a whole run the way a descending player would, one floor after another, and keeps
	 * every floor.
	 *
	 * <p>The order matters. Levelgen reads and mutates {@link Dungeon.LimitedDrops}, so the potions
	 * of strength and scrolls of upgrade a floor is allowed to place depend on what earlier floors
	 * already placed. Generating a floor in isolation would exercise a state the game never reaches.
	 */
	private static Map<Integer, Level> descend(long seed) {
		HeadlessDungeon.startRun(seed);

		Map<Integer, Level> levels = new LinkedHashMap<>();
		for (int depth = FIRST_DEPTH; depth <= AMULET_DEPTH; depth++) {
			Dungeon.depth = depth;
			Dungeon.branch = 0;
			levels.put(depth, Dungeon.newLevel());
		}
		return levels;
	}

	// ---------------------------------------------------------------- terrain

	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void tileArraysAllMatchTheMapDimensions(int depth) {
		Level level = reference(depth);

		assertTrue(level.width() > 0, "depth " + depth + " has no width");
		assertTrue(level.height() > 0, "depth " + depth + " has no height");
		assertEquals(level.width() * level.height(), level.length(),
				"depth " + depth + " reports a length that is not width*height");

		int length = level.length();
		assertEquals(length, level.map.length, "depth " + depth + ": map");
		assertEquals(length, level.passable.length, "depth " + depth + ": passable");
		assertEquals(length, level.losBlocking.length, "depth " + depth + ": losBlocking");
		assertEquals(length, level.flamable.length, "depth " + depth + ": flamable");
		assertEquals(length, level.secret.length, "depth " + depth + ": secret");
		assertEquals(length, level.solid.length, "depth " + depth + ": solid");
		assertEquals(length, level.avoid.length, "depth " + depth + ": avoid");
		assertEquals(length, level.water.length, "depth " + depth + ": water");
		assertEquals(length, level.pit.length, "depth " + depth + ": pit");
		assertEquals(length, level.openSpace.length, "depth " + depth + ": openSpace");
	}

	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void everyTileIsATerrainTypeTheGameDefines(int depth) {
		Level level = reference(depth);

		for (int cell = 0; cell < level.length(); cell++) {
			int terrain = level.map[cell];
			assertTrue(terrain >= 0 && terrain < Terrain.flags.length,
					"depth " + depth + " cell " + cell + " holds terrain id " + terrain
							+ ", which is outside the flag table");
			//every terrain the game declares sets at least one flag, so a zero means an unused id
			//was painted onto the map - typically a constant that was renumbered but not repainted
			assertTrue(Terrain.flags[terrain] != 0,
					"depth " + depth + " cell " + cell + " holds terrain id " + terrain
							+ ", which has no flags and so is not a terrain the game defines");
		}
	}

	/**
	 * Levels are allowed to tighten the flags they derive from terrain, and several do: {@code
	 * SewerLevel} makes its region decorations flammable, {@code LastLevel} turns the chasm around
	 * the amulet into solid railing, and a {@code MagicalFireRoom}'s eternal fire makes its own floor
	 * impassable until doused. So the flags cannot be required to equal the terrain table.
	 *
	 * <p>What must hold is the direction of those adjustments. A level may take movement away, never
	 * grant it — if {@code passable} ever became true for a cell the terrain says is a wall, the hero
	 * would walk through masonry.
	 */
	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	@DisplayName("flag maps never grant movement the terrain forbids")
	public void flagMapsNeverContradictTheirTerrain(int depth) {
		Level level = reference(depth);

		for (int cell : interiorCells(level)) {
			int flags = Terrain.flags[level.map[cell]];
			String where = "depth " + depth + " cell " + cell + " (terrain " + level.map[cell] + ")";

			if (level.passable[cell]) {
				assertTrue((flags & Terrain.PASSABLE) != 0,
						where + " is walkable even though its terrain is not");
			}
			if ((flags & Terrain.SOLID) != 0) {
				assertTrue(level.solid[cell], where + " is solid terrain but not marked solid");
			}
			if (level.openSpace[cell]) {
				assertFalse(level.solid[cell], where + " is both solid and open space");
			}

			//nothing in the codebase adjusts these three after they are derived
			assertEquals((flags & Terrain.SECRET) != 0, level.secret[cell], where + ": secret");
			assertEquals((flags & Terrain.LIQUID) != 0, level.water[cell], where + ": water");
			assertEquals((flags & Terrain.PIT) != 0, level.pit[cell], where + ": pit");
		}
	}

	/**
	 * {@code PathFinder} walks the eight neighbours of a cell without checking bounds, which is only
	 * safe because no reachable cell is ever on the map edge. {@code Level.buildFlagMaps()} is what
	 * guarantees that, by force-sealing the outer ring after deriving flags from terrain. If a level
	 * ever shipped without that ring, pathfinding would read past the ends of its arrays.
	 */
	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void theOuterRingIsSealed(int depth) {
		Level level = reference(depth);

		for (int cell : borderCells(level)) {
			String where = "depth " + depth + " border cell " + cell;
			assertFalse(level.passable[cell], where + " is passable");
			assertFalse(level.avoid[cell], where + " is marked avoid");
			assertTrue(level.solid[cell], where + " is not solid");
			assertTrue(level.losBlocking[cell], where + " does not block line of sight");
			assertFalse(level.openSpace[cell], where + " is open space");
		}
	}

	// ------------------------------------------------------------- occupants

	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void everyMobStandsOnACellItCouldOccupy(int depth) {
		Level level = reference(depth);

		for (Mob mob : level.mobs) {
			String who = "depth " + depth + ": " + mob.getClass().getSimpleName();
			assertTrue(mob.pos >= 0 && mob.pos < level.length(),
					who + " spawned at cell " + mob.pos + ", outside a " + level.length() + " cell map");
			assertFalse(level.solid[mob.pos],
					who + " spawned at cell " + mob.pos + ", which is solid");
			assertTrue(level.passable[mob.pos],
					who + " spawned at cell " + mob.pos + ", which is not passable");
		}
	}

	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void noTwoMobsShareACell(int depth) {
		Level level = reference(depth);

		Map<Integer, String> occupants = new HashMap<>();
		for (Mob mob : level.mobs) {
			String previous = occupants.put(mob.pos, mob.getClass().getSimpleName());
			assertNull(previous, "depth " + depth + ": " + mob.getClass().getSimpleName()
					+ " and " + previous + " both spawned on cell " + mob.pos);
		}
	}

	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void everyItemLiesOnACellTheHeroCanPickItUpFrom(int depth) {
		Level level = reference(depth);

		for (int cell : level.heaps.keyArray()) {
			Heap heap = level.heaps.get(cell);
			String what = "depth " + depth + ": " + heap.type + " heap";
			assertTrue(cell >= 0 && cell < level.length(),
					what + " is at cell " + cell + ", outside a " + level.length() + " cell map");
			assertFalse(level.solid[cell],
					what + " is at cell " + cell + ", which is solid, so it can never be picked up");
			assertFalse(heap.items.isEmpty(), what + " at cell " + cell + " holds no items");
			assertEquals(cell, heap.pos,
					what + " is filed under cell " + cell + " but believes it is at " + heap.pos);
		}
	}

	@Test
	@DisplayName("each region places at least three identify scrolls on its regular floors")
	public void eachRegionDropsIdentifyScrolls() {
		for (int region = 0; region < 5; region++) {
			int count = 0;
			for (int floor = 1; floor <= 4; floor++) {
				count += countIdentifyScrolls(reference(region * 5 + floor));
			}
			assertTrue(count >= 3,
					"region " + (region + 1) + " only placed " + count + " identify scrolls");
		}
	}

	// ----------------------------------------------------------- connectivity

	@Test
	public void everyFloorHasTheTransitionsItsPositionInTheDungeonRequires() {
		for (int depth = FIRST_DEPTH; depth <= AMULET_DEPTH; depth++) {
			Level level = reference(depth);

			assertNotNull(entranceOf(level), "depth " + depth + " has no way back up");

			if (depth == FIRST_DEPTH) {
				assertNotNull(transitionOf(level, LevelTransition.Type.SURFACE),
						"depth 1 must lead back to the surface");
			}
			if (depth < AMULET_DEPTH) {
				assertNotNull(transitionOf(level, LevelTransition.Type.REGULAR_EXIT),
						"depth " + depth + " has no way down, so the run cannot continue");
			}

			for (LevelTransition transition : level.transitions) {
				int cell = transition.cell();
				assertTrue(cell >= 0 && cell < level.length(),
						"depth " + depth + ": " + transition.type + " sits at cell " + cell
								+ ", outside a " + level.length() + " cell map");
			}
		}
	}

	/**
	 * Boss floors are exempt. Their exits start walled in and are opened by the fight itself — Tengu's
	 * cell block rewrites its own map between arena phases, and the DM-300 and Yog arenas unseal only
	 * once the boss dies. Asking whether their exit is reachable at generation time is the wrong
	 * question; the twenty ordinary floors are where the generator actually improvises.
	 */
	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void theExitIsReachableFromTheEntrance(int depth) {
		Level level = reference(depth);

		LevelTransition exit = transitionOf(level, LevelTransition.Type.REGULAR_EXIT);
		if (exit == null) {
			//the amulet floor is the bottom of the dungeon and has nowhere further to go
			assertEquals(AMULET_DEPTH, depth, "depth " + depth + " unexpectedly has no exit");
			return;
		}
		if (Dungeon.bossLevel(depth)) return;

		int entrance = entranceOf(level).cell();
		assertTrue(canReach(level, entrance, exit.cell()),
				"depth " + depth + ": no route from the entrance (cell " + entrance
						+ ") to the exit (cell " + exit.cell() + "), so the floor cannot be completed");
	}

	/**
	 * Nothing may be sealed inside solid rock. Chasms count as crossable here even though nobody can
	 * walk over one, because {@code SecretChestChasmRoom} moats its chests on purpose — its own
	 * comment says the room "always requires 2 levitation potions". So an island across a chasm is a
	 * puzzle; an island inside masonry is a generator that lost a room.
	 */
	@ParameterizedTest(name = "depth {0}")
	@MethodSource("allDepths")
	public void nothingIsEntombedInSolidRock(int depth) {
		Level level = reference(depth);
		boolean[] reached = reachableFrom(level, entranceOf(level).cell(),
				terrain -> !isMasonry(terrain));

		for (int cell : level.heaps.keyArray()) {
			assertTrue(reached[cell], "depth " + depth + ": the " + level.heaps.get(cell).type
					+ " heap at cell " + cell + " is walled off from the entrance");
		}
		for (Mob mob : level.mobs) {
			assertTrue(reached[mob.pos], "depth " + depth + ": " + mob.getClass().getSimpleName()
					+ " at cell " + mob.pos + " is walled off from the entrance");
		}
	}

	// -------------------------------------------------------- quest branches

	/**
	 * The quest sub-floors hang off a branch transition rather than the main staircase, so nothing
	 * above would have generated them.
	 */
	@ParameterizedTest(name = "depth {0} branch 1")
	@ValueSource(ints = {11, 12, 13, 14, 16, 17, 18, 19})
	public void questBranchesGenerateAndAreTraversable(int depth) {
		HeadlessDungeon.startRun(REFERENCE_SEED);
		Dungeon.depth = depth;
		Dungeon.branch = 1;
		Level level = Dungeon.newLevel();

		assertEquals(level.width() * level.height(), level.length(),
				"depth " + depth + " branch 1 reports an inconsistent length");
		for (int cell : borderCells(level)) {
			assertTrue(level.solid[cell],
					"depth " + depth + " branch 1 border cell " + cell + " is not solid");
		}

		LevelTransition entrance = entranceOf(level);
		assertNotNull(entrance, "depth " + depth + " branch 1 has no way back out");
		boolean[] reached = reachableFrom(level, entrance.cell());
		for (int cell : level.heaps.keyArray()) {
			assertTrue(reached[cell], "depth " + depth + " branch 1: the heap at cell " + cell
					+ " is walled off from the entrance");
		}
	}

	// -------------------------------------------------------------- sweeping

	/**
	 * One seed proves the generator can work; it takes a spread of them to show it usually does.
	 */
	@ParameterizedTest(name = "seed {0}")
	@ValueSource(longs = {1L, 42L, 1_000_003L, -7L, Long.MAX_VALUE, 0L})
	public void aWholeRunIsWellFormedOnAnySeed(long seed) {
		for (Map.Entry<Integer, Level> floor : descend(seed).entrySet()) {
			int depth = floor.getKey();
			Level level = floor.getValue();
			String run = "seed " + seed + " depth " + depth;

			assertEquals(level.width() * level.height(), level.length(), run + ": inconsistent length");

			for (int cell : borderCells(level)) {
				assertTrue(level.solid[cell], run + ": border cell " + cell + " is not solid");
			}
			for (Mob mob : level.mobs) {
				assertTrue(level.passable[mob.pos], run + ": " + mob.getClass().getSimpleName()
						+ " spawned on an impassable cell " + mob.pos);
			}
			for (int cell : level.heaps.keyArray()) {
				assertFalse(level.solid[cell], run + ": heap entombed at cell " + cell);
			}

			LevelTransition entrance = entranceOf(level);
			assertNotNull(entrance, run + ": no way back up");

			LevelTransition exit = transitionOf(level, LevelTransition.Type.REGULAR_EXIT);
			if (exit == null) {
				assertEquals(AMULET_DEPTH, depth, run + ": unexpectedly has no exit");
			} else if (!Dungeon.bossLevel(depth)) {
				assertTrue(canReach(level, entrance.cell(), exit.cell()),
						run + ": the exit cannot be reached from the entrance");
			}
		}
	}

	// ----------------------------------------------------------- determinism

	@Nested
	@DisplayName("seeds")
	public class Seeds {

		/**
		 * Players share seeds and expect the same dungeon, and the daily challenge depends on it
		 * outright. Anything that reached for unseeded randomness while carving a floor — a {@code
		 * HashSet} iteration order, a {@code Math.random()} call — would show up here.
		 *
		 * <p>What gets compared is the floor plan: which cells are rock and where the staircases are.
		 * That is deliberately narrower than the whole tile map, because the contents of a floor are
		 * not reproducible from the seed alone and are not meant to be. Item placement depends on the
		 * player's own progress — a floor drops a guide page only while pages are missing — and a
		 * dropped item then edits the terrain under it, disarming a hidden trap or flattening high
		 * grass. So two players on the same seed get the same rooms and corridors, but not necessarily
		 * the same trap in the same corner. Asserting otherwise would produce a test that passes or
		 * fails depending on what ran before it.
		 */
		@Test
		public void theSameSeedProducesTheSameFloorPlan() {
			Map<Integer, Level> first = descend(777L);
			Map<Integer, Level> second = descend(777L);

			for (int depth = FIRST_DEPTH; depth <= AMULET_DEPTH; depth++) {
				assertArrayEquals(floorPlanOf(first.get(depth)), floorPlanOf(second.get(depth)),
						"depth " + depth + " was laid out differently on a second run of the same seed");
			}
		}

		@Test
		public void differentSeedsProduceDifferentDungeons() {
			Map<Integer, Level> first = descend(101L);
			Map<Integer, Level> second = descend(202L);

			//individual floors could coincide, but a whole run matching would mean the seed is ignored
			int identicalFloors = 0;
			for (int depth = FIRST_DEPTH; depth <= AMULET_DEPTH; depth++) {
				if (Arrays.equals(floorPlanOf(first.get(depth)), floorPlanOf(second.get(depth)))) {
					identicalFloors++;
				}
			}
			assertTrue(identicalFloors < AMULET_DEPTH,
					"two different seeds produced an identical dungeon, so the seed is not being used");
		}

		/**
		 * The rooms and corridors of a floor, as a wall-or-not map plus the cells the staircases sit
		 * on. Nothing here can be moved by an item landing on it.
		 */
		private int[] floorPlanOf(Level level) {
			int[] plan = new int[level.length() + 1 + level.transitions.size()];
			for (int cell = 0; cell < level.length(); cell++) {
				plan[cell] = isMasonry(level.map[cell]) ? 1 : 0;
			}
			int at = level.length();
			plan[at++] = level.width();
			for (LevelTransition transition : level.transitions) {
				plan[at++] = transition.cell();
			}
			return plan;
		}
	}

	// ---------------------------------------------------------------- helpers

	private static LevelTransition transitionOf(Level level, LevelTransition.Type type) {
		for (LevelTransition transition : level.transitions) {
			if (transition.type == type) return transition;
		}
		return null;
	}

	/**
	 * The way back out, which is a staircase on most floors, the surface on the first, and a branch
	 * transition on the quest sub-floors.
	 */
	private static LevelTransition entranceOf(Level level) {
		for (LevelTransition.Type type : new LevelTransition.Type[]{
				LevelTransition.Type.REGULAR_ENTRANCE,
				LevelTransition.Type.SURFACE,
				LevelTransition.Type.BRANCH_ENTRANCE,
				LevelTransition.Type.BRANCH_EXIT}) {
			LevelTransition transition = transitionOf(level, type);
			if (transition != null) return transition;
		}
		return null;
	}

	private static List<Integer> interiorCells(Level level) {
		List<Integer> cells = new ArrayList<>();
		for (int y = 1; y < level.height() - 1; y++) {
			for (int x = 1; x < level.width() - 1; x++) {
				cells.add(x + y * level.width());
			}
		}
		return cells;
	}

	private static List<Integer> borderCells(Level level) {
		Set<Integer> cells = new HashSet<>();
		int lastRow = level.length() - level.width();
		for (int x = 0; x < level.width(); x++) {
			cells.add(x);
			cells.add(lastRow + x);
		}
		for (int y = 0; y < level.height(); y++) {
			cells.add(y * level.width());
			cells.add(y * level.width() + level.width() - 1);
		}
		return new ArrayList<>(cells);
	}

	private static int countIdentifyScrolls(Level level) {
		int count = 0;
		for (Heap heap : level.heaps.valueList()) {
			for (Item item : heap.items) {
				if (item instanceof ScrollOfIdentify) {
					count += item.quantity();
				}
			}
		}
		return count;
	}

	/**
	 * Whether a determined hero could ever get through this tile, given the keys and tools the
	 * dungeon itself hands out.
	 *
	 * <p>This is deliberately more generous than {@link Level#passable}. Locked doors, barricades,
	 * bookshelves, secret doors, mine walls and statuary are all impassable in the flag maps, but they
	 * are obstacles the game hands the player a key, a pickaxe or a search command for — not
	 * structure. Only masonry stays blocked, plus chasms, since crossing one means falling to the next
	 * floor rather than walking anywhere.
	 *
	 * <p>Being generous is the point: it means a failure can only mean the floor is genuinely cut in
	 * two by solid rock, never that the test disagreed with the designer about a locked door.
	 */
	private static boolean isOpenable(int terrain) {
		return !isMasonry(terrain) && terrain != Terrain.CHASM;
	}

	/** Solid rock, the one obstacle the dungeon never hands the player a way through. */
	private static boolean isMasonry(int terrain) {
		return terrain == Terrain.WALL || terrain == Terrain.WALL_DECO;
	}

	private static boolean[] reachableFrom(Level level, int origin) {
		return reachableFrom(level, origin, LevelGenerationTest::isOpenable);
	}

	private static boolean[] reachableFrom(Level level, int origin, IntPredicate crossable) {
		boolean[] open = new boolean[level.length()];
		for (int cell = 0; cell < open.length; cell++) {
			open[cell] = crossable.test(level.map[cell]);
		}

		//PathFinder keeps the map dimensions in statics, and these levels vary in size
		PathFinder.setMapSize(level.width(), level.height());
		PathFinder.buildDistanceMap(origin, open);

		boolean[] reached = new boolean[level.length()];
		for (int cell = 0; cell < reached.length; cell++) {
			reached[cell] = PathFinder.distance[cell] != Integer.MAX_VALUE;
		}
		return reached;
	}

	private static boolean canReach(Level level, int from, int to) {
		return reachableFrom(level, from)[to];
	}
}
