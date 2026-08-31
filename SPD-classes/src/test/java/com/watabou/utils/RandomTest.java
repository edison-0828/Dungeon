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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.Standard.class)
public class RandomTest {

	private static final long SEED = 8675309L;

	@BeforeEach
	@AfterEach
	public void resetGeneratorStack(){
		//Random holds a global generator stack, so leaking a pushed generator would
		//silently couple tests together
		Random.resetGenerators();
	}

	private static int[] nextInts( int count, int bound ){
		int[] result = new int[count];
		for (int i = 0; i < count; i++){
			result[i] = Random.Int( bound );
		}
		return result;
	}

	@Test
	@DisplayName("the same seed always produces the same sequence")
	public void seedsAreReproducible(){
		Random.pushGenerator( SEED );
		int[] first = nextInts( 16, 1000 );
		Random.popGenerator();

		Random.pushGenerator( SEED );
		int[] second = nextInts( 16, 1000 );
		Random.popGenerator();

		assertArrayEquals( first, second );
	}

	@Test
	@DisplayName("neighbouring seeds do not produce similar sequences")
	public void neighbouringSeedsDiverge(){
		//this is the whole point of scrambling the seed before use
		Random.pushGenerator( SEED );
		int[] first = nextInts( 16, 1000 );
		Random.popGenerator();

		Random.pushGenerator( SEED + 1 );
		int[] second = nextInts( 16, 1000 );
		Random.popGenerator();

		assertNotEquals( java.util.Arrays.toString( first ), java.util.Arrays.toString( second ) );
	}

	/**
	 * MX3 by Jon Maiga, reimplemented here independently of Random.scrambleSeed.
	 * Shattered Pixel Dungeon seed codes are shared between players and daily challenges are
	 * expected to match across devices and versions, so this mapping must never drift.
	 */
	private static long mx3( long seed ){
		seed ^= seed >>> 32;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 29;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 32;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 29;
		return seed;
	}

	@Test
	@DisplayName("seeds are scrambled with MX3 and nothing else")
	public void seedScramblingIsStable(){
		java.util.Random reference = new java.util.Random( mx3( SEED ) );
		int[] expected = new int[16];
		for (int i = 0; i < expected.length; i++){
			expected[i] = reference.nextInt( 1000 );
		}

		Random.pushGenerator( SEED );
		int[] actual = nextInts( 16, 1000 );
		Random.popGenerator();

		assertArrayEquals( expected, actual,
				"changing the seed scramble would invalidate every published seed code" );
	}

	@Test
	@DisplayName("popping a generator restores the previous stream")
	public void popRestoresPreviousStream(){
		Random.pushGenerator( SEED );
		int[] baseline = nextInts( 4, 1000 );
		Random.popGenerator();

		Random.pushGenerator( SEED );
		int[] beforeNesting = nextInts( 2, 1000 );
		//levelgen nests seeded generators; the outer stream must not be disturbed
		Random.pushGenerator( SEED * 31 );
		nextInts( 8, 1000 );
		Random.popGenerator();
		int[] afterNesting = nextInts( 2, 1000 );
		Random.popGenerator();

		assertEquals( baseline[0], beforeNesting[0] );
		assertEquals( baseline[1], beforeNesting[1] );
		assertEquals( baseline[2], afterNesting[0] );
		assertEquals( baseline[3], afterNesting[1] );
	}

	@Test
	@DisplayName("the unseeded base generator is reachable past a seeded one")
	public void baseGeneratorBypassesTheStack(){
		//UI and cosmetic randomness must not consume numbers from the seeded levelgen stream
		Random.pushGenerator( SEED );
		int[] seeded = nextInts( 4, 1000 );
		Random.popGenerator();

		Random.pushGenerator( SEED );
		for (int i = 0; i < 32; i++){
			Random.Int( 1000, false );
		}
		int[] stillSeeded = nextInts( 4, 1000 );
		Random.popGenerator();

		assertArrayEquals( seeded, stillSeeded );
	}

	@Test
	@DisplayName("popping the last generator is refused rather than corrupting the stack")
	public void lastGeneratorCannotBePopped(){
		//a stack trace on stderr is expected here
		Random.popGenerator();

		//the stack is still usable
		assertTrue( Random.Int( 10 ) >= 0 );
	}

	@Test
	@DisplayName("weighted choice never returns a zero-weight index")
	public void weightedChoiceSkipsZeroWeights(){
		float[] chances = new float[]{ 0, 5, 0, 3, 0 };

		for (int i = 0; i < 500; i++){
			int picked = Random.chances( chances );
			assertTrue( picked == 1 || picked == 3, "picked a zero-weight index: " + picked );
		}
	}

	@Test
	@DisplayName("weighted choice treats negative weights as zero")
	public void weightedChoiceTreatsNegativesAsZero(){
		float[] chances = new float[]{ -10, 1 };

		for (int i = 0; i < 200; i++){
			assertEquals( 1, Random.chances( chances ) );
		}
	}

	@Test
	@DisplayName("an exhausted weight deck reports -1 rather than picking anything")
	public void exhaustedDeckReportsNoChoice(){
		//Generator relies on this to know when to reshuffle a deck
		assertEquals( -1, Random.chances( new float[]{ 0, 0, 0 } ) );
		assertEquals( -1, Random.chances( new float[0] ) );
	}

	@Test
	@DisplayName("a weighted map with no weight yields null")
	public void emptyWeightMapYieldsNull(){
		HashMap<String, Float> chances = new HashMap<>();
		chances.put( "a", 0f );

		assertNull( Random.chances( chances ) );
	}

	@Test
	@DisplayName("a weighted map returns only weighted keys")
	public void weightedMapReturnsWeightedKeys(){
		HashMap<String, Float> chances = new HashMap<>();
		chances.put( "never", 0f );
		chances.put( "always", 4f );

		for (int i = 0; i < 200; i++){
			assertEquals( "always", Random.chances( chances ) );
		}
	}

	@Test
	@DisplayName("a non-positive bound yields zero instead of throwing")
	public void nonPositiveBoundYieldsZero(){
		assertEquals( 0, Random.Int( 0 ) );
		assertEquals( 0, Random.Int( -5 ) );
	}

	@Test
	@DisplayName("integer ranges are half-open, and IntRange is inclusive")
	public void integerRangeBounds(){
		for (int i = 0; i < 500; i++){
			int halfOpen = Random.Int( 5, 8 );
			assertTrue( halfOpen >= 5 && halfOpen < 8, "out of range: " + halfOpen );

			int inclusive = Random.IntRange( 5, 8 );
			assertTrue( inclusive >= 5 && inclusive <= 8, "out of range: " + inclusive );
		}

		//a single-value range must terminate rather than loop or overflow
		assertEquals( 3, Random.IntRange( 3, 3 ) );
	}

	@Test
	@DisplayName("floats stay inside a half-open unit interval")
	public void floatBounds(){
		for (int i = 0; i < 500; i++){
			float value = Random.Float();
			assertTrue( value >= 0f && value < 1f, "out of range: " + value );
		}
	}

	@Test
	@DisplayName("bounded longs are non-negative, as dungeon seed generation assumes")
	public void boundedLongsAreNonNegative(){
		//DungeonSeed.randomSeed() feeds this straight into convertToCode, which rejects negatives.
		//Long(max) folds negatives with `result += Long.MAX_VALUE`, so the single input
		//Long.MIN_VALUE still maps to -1; that is 1 in 2^64 and not worth reshaping the mapping
		//for, since changing it would renumber every existing seed.
		for (int i = 0; i < 2000; i++){
			long value = Random.Long( 5429503678976L );
			assertTrue( value >= 0, "negative seed: " + value );
			assertTrue( value < 5429503678976L, "seed out of range: " + value );
		}
	}

	@Test
	@DisplayName("shuffling is a permutation, not a rewrite")
	public void shuffleIsAPermutation(){
		int[] array = new int[64];
		for (int i = 0; i < array.length; i++){
			array[i] = i;
		}

		Random.shuffle( array );

		boolean[] seen = new boolean[array.length];
		for (int value : array){
			assertFalse( seen[value], "value appeared twice: " + value );
			seen[value] = true;
		}
	}

	@Test
	@DisplayName("shuffling two arrays together keeps them aligned")
	public void pairedShuffleStaysAligned(){
		Integer[] keys = new Integer[32];
		String[] values = new String[32];
		for (int i = 0; i < keys.length; i++){
			keys[i] = i;
			values[i] = "v" + i;
		}

		Random.shuffle( keys, values );

		for (int i = 0; i < keys.length; i++){
			assertEquals( "v" + keys[i], values[i] );
		}
	}
}
