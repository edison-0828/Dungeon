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

package com.watabou.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every map in the game is 9x9 or larger and has a sealed, impassable outer ring, which
 * Level.buildFlagMaps() enforces unconditionally. These tests model that same shape,
 * because PathFinder depends on it (see {@link #findAssumesASealedBorder()}).
 */
public class PathFinderTest {

	private static final int WIDTH = 9;
	private static final int HEIGHT = 9;

	private boolean[] passable;

	private static int cell( int x, int y ){
		return x + y * WIDTH;
	}

	/** Mirrors Level.buildFlagMaps(): the outer ring of the map is never passable. */
	private void sealBorder(){
		for (int x = 0; x < WIDTH; x++){
			passable[cell( x, 0 )] = false;
			passable[cell( x, HEIGHT - 1 )] = false;
		}
		for (int y = 0; y < HEIGHT; y++){
			passable[cell( 0, y )] = false;
			passable[cell( WIDTH - 1, y )] = false;
		}
	}

	@BeforeEach
	public void setUp(){
		PathFinder.setMapSize( WIDTH, HEIGHT );
		passable = new boolean[WIDTH * HEIGHT];
		Arrays.fill( passable, true );
		sealBorder();
	}

	@Test
	@DisplayName("neighbour offsets are derived from map width")
	public void neighbourOffsetsMatchWidth(){
		assertArrayEquals( new int[]{ -WIDTH, -1, 1, WIDTH }, PathFinder.NEIGHBOURS4 );
		assertArrayEquals( new int[]{ -WIDTH - 1, -WIDTH, -WIDTH + 1, -1, 1, WIDTH - 1, WIDTH, WIDTH + 1 },
				PathFinder.NEIGHBOURS8 );
		//NEIGHBOURS9 is the only variant which includes the cell itself
		assertArrayEquals( new int[]{ -WIDTH - 1, -WIDTH, -WIDTH + 1, -1, 0, 1, WIDTH - 1, WIDTH, WIDTH + 1 },
				PathFinder.NEIGHBOURS9 );
	}

	@Test
	@DisplayName("movement is 8-directional, so diagonals cost the same as orthogonals")
	public void diagonalMovementCostsOneStep(){
		int from = cell( 1, 1 );
		int to = cell( 6, 6 );

		PathFinder.Path path = PathFinder.find( from, to, passable );

		assertNotNull( path );
		//chebyshev distance, not manhattan
		assertEquals( 5, path.size() );
		//the path excludes the origin and ends on the target
		assertEquals( to, (int) path.getLast() );
	}

	@Test
	@DisplayName("distance map holds chebyshev distances on an open field")
	public void distanceMapOnOpenField(){
		int origin = cell( 4, 4 );

		PathFinder.buildDistanceMap( origin, passable );

		assertEquals( 0, PathFinder.distance[origin] );
		assertEquals( 1, PathFinder.distance[cell( 5, 5 )] );
		assertEquals( 1, PathFinder.distance[cell( 4, 3 )] );
		assertEquals( 3, PathFinder.distance[cell( 1, 1 )] );
		assertEquals( 3, PathFinder.distance[cell( 7, 7 )] );
	}

	@Test
	@DisplayName("unreachable cells keep a maximal distance rather than 0")
	public void unreachableCellsStayAtMaxValue(){
		//seal off a full interior column, splitting the map in two
		for (int y = 1; y < HEIGHT - 1; y++){
			passable[cell( 4, y )] = false;
		}

		PathFinder.buildDistanceMap( cell( 1, 1 ), passable );

		assertEquals( 0, PathFinder.distance[cell( 1, 1 )] );
		assertEquals( Integer.MAX_VALUE, PathFinder.distance[cell( 7, 7 )] );
		//the sealed border must never be treated as reachable
		assertEquals( Integer.MAX_VALUE, PathFinder.distance[cell( 0, 0 )] );
	}

	@Test
	@DisplayName("no path through a sealed wall")
	public void sealedWallBlocksPathing(){
		for (int y = 1; y < HEIGHT - 1; y++){
			passable[cell( 4, y )] = false;
		}

		assertNull( PathFinder.find( cell( 1, 1 ), cell( 7, 7 ), passable ) );
		assertEquals( -1, PathFinder.getStep( cell( 1, 1 ), cell( 7, 7 ), passable ) );
	}

	@Test
	@DisplayName("a gap in the wall is found and routed through")
	public void pathRoutesThroughGap(){
		for (int y = 1; y < HEIGHT - 1; y++){
			passable[cell( 4, y )] = false;
		}
		int gap = cell( 4, 7 );
		passable[gap] = true;

		PathFinder.Path path = PathFinder.find( cell( 1, 1 ), cell( 7, 1 ), passable );

		assertNotNull( path );
		assertTrue( path.contains( gap ), "the only opening must be part of the route" );
		assertEquals( cell( 7, 1 ), (int) path.getLast() );
	}

	@Test
	@DisplayName("paths never wrap around row edges")
	public void pathingDoesNotWrapAcrossRows(){
		//leave only one interior row open; a wrapping bug would let the path leak into another row
		Arrays.fill( passable, false );
		for (int x = 1; x < WIDTH - 1; x++){
			passable[cell( x, 4 )] = true;
		}

		PathFinder.Path path = PathFinder.find( cell( 1, 4 ), cell( 7, 4 ), passable );

		assertNotNull( path );
		assertEquals( 6, path.size() );
		for (int step : path){
			assertEquals( 4, step / WIDTH, "step " + step + " left the open row" );
		}
	}

	@Test
	@DisplayName("a step toward an adjacent target lands on the target")
	public void stepTowardAdjacentTarget(){
		int from = cell( 3, 3 );
		int to = cell( 4, 3 );

		assertEquals( to, PathFinder.getStep( from, to, passable ) );
	}

	@Test
	@DisplayName("there is no path from a cell to itself")
	public void noPathToSelf(){
		int cell = cell( 3, 3 );

		assertNull( PathFinder.find( cell, cell, passable ) );
		assertEquals( -1, PathFinder.getStep( cell, cell, passable ) );
	}

	@Test
	@DisplayName("a limited distance map stops expanding past its limit")
	public void limitedDistanceMapRespectsLimit(){
		int origin = cell( 4, 4 );

		PathFinder.buildDistanceMap( origin, passable, 1 );

		assertEquals( 0, PathFinder.distance[origin] );
		assertEquals( 1, PathFinder.distance[cell( 5, 5 )] );
		assertEquals( Integer.MAX_VALUE, PathFinder.distance[cell( 1, 1 )] );
	}

	@Test
	@DisplayName("origins are unreachable but still pathable from, as when a mob stands in a wall")
	public void impassableOriginCanStillBePathedFrom(){
		//buildDistanceMap special-cases the origin, so a mob shoved into terrain can escape
		int from = cell( 1, 1 );
		passable[from] = false;

		assertEquals( cell( 2, 2 ), PathFinder.getStep( from, cell( 5, 5 ), passable ) );
	}

	@Test
	@DisplayName("documents that find() assumes a sealed border and does not bounds check")
	public void findAssumesASealedBorder(){
		//find() and getStep() read distance[cell + offset] with no bounds check, unlike
		//buildDistanceMap(). That is safe only because Level.buildFlagMaps() makes the outer
		//ring of every map impassable, so no actor ever stands on a border cell.
		//This test pins the constraint down: if bounds checking is ever added, update it
		//deliberately rather than by accident.
		boolean[] fullyOpen = new boolean[WIDTH * HEIGHT];
		Arrays.fill( fullyOpen, true );

		assertThrows( ArrayIndexOutOfBoundsException.class,
				() -> PathFinder.find( cell( 0, 0 ), cell( 5, 5 ), fullyOpen ) );
	}
}
