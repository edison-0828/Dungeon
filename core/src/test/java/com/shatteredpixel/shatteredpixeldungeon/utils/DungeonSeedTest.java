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

package com.shatteredpixel.shatteredpixeldungeon.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Seed codes are printed, shared between players, and used to reproduce a run on another
 * device or another build. Every mapping in here is therefore a compatibility surface:
 * if it shifts, previously shared codes silently start generating different dungeons.
 */
public class DungeonSeedTest {

	@Test
	@DisplayName("the seed space is 26^9, matching a nine letter code")
	public void seedSpaceMatchesCodeWidth(){
		long expected = 1;
		for (int i = 0; i < 9; i++){
			expected *= 26;
		}

		assertEquals( expected, DungeonSeed.TOTAL_SEEDS );
	}

	@Test
	@DisplayName("the extremes of the seed space map to the extremes of the code space")
	public void extremeSeedsMapToExtremeCodes(){
		assertEquals( "AAA-AAA-AAA", DungeonSeed.convertToCode( 0 ) );
		assertEquals( "ZZZ-ZZZ-ZZZ", DungeonSeed.convertToCode( DungeonSeed.TOTAL_SEEDS - 1 ) );

		assertEquals( 0, DungeonSeed.convertFromCode( "AAA-AAA-AAA" ) );
		assertEquals( DungeonSeed.TOTAL_SEEDS - 1, DungeonSeed.convertFromCode( "ZZZ-ZZZ-ZZZ" ) );
	}

	@Test
	@DisplayName("codes are base-26 big-endian, so the last letter is the least significant")
	public void codesAreBigEndianBase26(){
		assertEquals( "AAA-AAA-AAB", DungeonSeed.convertToCode( 1 ) );
		assertEquals( "AAA-AAA-ABA", DungeonSeed.convertToCode( 26 ) );
		assertEquals( "BAA-AAA-AAA", DungeonSeed.convertToCode( pow26( 8 ) ) );
	}

	@Test
	@DisplayName("every seed survives a code round trip")
	public void seedsSurviveCodeRoundTrip(){
		long[] samples = {
				0,
				1,
				25,
				26,
				27,
				pow26( 4 ),
				pow26( 4 ) + 12345,
				5429503678975L,
				DungeonSeed.TOTAL_SEEDS - 1 };

		for (long seed : samples){
			String code = DungeonSeed.convertToCode( seed );
			assertEquals( seed, DungeonSeed.convertFromCode( code ), "round trip failed for " + code );
		}
	}

	@Test
	@DisplayName("seeds outside the space are rejected rather than wrapping")
	public void outOfRangeSeedsAreRejected(){
		assertThrows( IllegalArgumentException.class, () -> DungeonSeed.convertToCode( -1 ) );
		assertThrows( IllegalArgumentException.class,
				() -> DungeonSeed.convertToCode( DungeonSeed.TOTAL_SEEDS ) );
	}

	@Test
	@DisplayName("codes tolerate dashes, spaces and lowercase in the canonical layout")
	public void codesTolerateFormattingNoise(){
		long expected = DungeonSeed.convertFromCode( "ABC-DEF-GHI" );

		assertEquals( expected, DungeonSeed.convertFromCode( "ABCDEFGHI" ) );
		assertEquals( expected, DungeonSeed.convertFromCode( "ABC DEF GHI" ) );
		assertEquals( expected, DungeonSeed.convertFromCode( "abc-def-ghi" ) );
	}

	@Test
	@DisplayName("lowercase is only accepted in the dashed layout")
	public void lowercaseNeedsTheDashedLayout(){
		//convertFromCode only upper-cases input that already looks like @@@-@@@-@@@, so a bare
		//lowercase run is rejected. Documented here because it is surprising, not because it
		//is desirable.
		assertThrows( IllegalArgumentException.class,
				() -> DungeonSeed.convertFromCode( "abcdefghi" ) );
	}

	@Test
	@DisplayName("malformed codes are rejected")
	public void malformedCodesAreRejected(){
		assertThrows( IllegalArgumentException.class, () -> DungeonSeed.convertFromCode( "" ) );
		assertThrows( IllegalArgumentException.class, () -> DungeonSeed.convertFromCode( "ABC" ) );
		assertThrows( IllegalArgumentException.class, () -> DungeonSeed.convertFromCode( "ABCDEFGHIJ" ) );
		assertThrows( IllegalArgumentException.class, () -> DungeonSeed.convertFromCode( "ABC-DEF-GH1" ) );
		assertThrows( IllegalArgumentException.class, () -> DungeonSeed.convertFromCode( "ABC-DEF-GH!" ) );
	}

	@Test
	@DisplayName("empty text input reports no seed")
	public void emptyTextReportsNoSeed(){
		assertEquals( -1, DungeonSeed.convertFromText( "" ) );
	}

	@Test
	@DisplayName("text input prefers the code interpretation")
	public void textInputPrefersCodes(){
		assertEquals( 1, DungeonSeed.convertFromText( "AAA-AAA-AAB" ) );
	}

	@Test
	@DisplayName("numeric text input is used as a literal seed")
	public void numericTextIsUsedLiterally(){
		assertEquals( 12345, DungeonSeed.convertFromText( "12345" ) );
		//whitespace is stripped before parsing
		assertEquals( 12345, DungeonSeed.convertFromText( " 12 345 " ) );
	}

	@Test
	@DisplayName("arbitrary text hashes to a stable seed inside the seed space")
	public void arbitraryTextHashesStably(){
		String[] inputs = { "Evan", "shattered", "Pixel Dungeon", "地牢", "!!!", "a" };

		for (String input : inputs){
			long seed = DungeonSeed.convertFromText( input );

			assertEquals( expectedTextHash( input ), seed, "hash drifted for \"" + input + "\"" );
			assertTrue( seed >= 0 && seed < DungeonSeed.TOTAL_SEEDS,
					"\"" + input + "\" produced an unusable seed: " + seed );
			//the same text must always give the same dungeon
			assertEquals( seed, DungeonSeed.convertFromText( input ) );
		}
	}

	/** Mirrors the documented "long hashCode with overflow" mapping used for fun seeds. */
	private static long expectedTextHash( String input ){
		long total = 0;
		for (char c : input.toCharArray()){
			total = 31 * total + c;
		}
		if (total < 0) total += Long.MAX_VALUE;
		return total % DungeonSeed.TOTAL_SEEDS;
	}

	@Test
	@DisplayName("formatting normalises codes and passes other text through untouched")
	public void formatTextNormalisesOnlyCodes(){
		assertEquals( "AAA-AAA-AAB", DungeonSeed.formatText( "aaa-aaa-aab" ) );
		assertEquals( "AAA-AAA-AAB", DungeonSeed.formatText( "AAAAAAAAB" ) );
		assertEquals( "Pixel Dungeon", DungeonSeed.formatText( "Pixel Dungeon" ) );
	}

	@Test
	@DisplayName("random seeds are usable and avoid vowels so they rarely spell words")
	public void randomSeedsAvoidVowels(){
		for (int i = 0; i < 200; i++){
			long seed = DungeonSeed.randomSeed();

			assertTrue( seed >= 0 && seed < DungeonSeed.TOTAL_SEEDS, "unusable seed: " + seed );

			String code = DungeonSeed.convertToCode( seed );
			for (char vowel : new char[]{ 'A', 'E', 'I', 'O', 'U' }){
				assertTrue( code.indexOf( vowel ) < 0, code + " contains a vowel" );
			}
		}
	}

	private static long pow26( int exponent ){
		long result = 1;
		for (int i = 0; i < exponent; i++){
			result *= 26;
		}
		return result;
	}
}
