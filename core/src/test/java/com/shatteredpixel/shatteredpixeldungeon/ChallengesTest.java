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

package com.shatteredpixel.shatteredpixeldungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Challenges are stored in save files and rankings as a single bit mask, and three parallel
 * declarations have to agree: the mask constants, the MASKS array, and the NAME_IDS used for
 * translation lookups. Adding a challenge and updating only some of them is a silent break.
 */
public class ChallengesTest {

	@Test
	@DisplayName("the mask list, the name list and the declared count agree")
	public void parallelDeclarationsAgree(){
		assertEquals( Challenges.MAX_CHALS, Challenges.MASKS.length,
				"MASKS is out of step with MAX_CHALS" );
		assertEquals( Challenges.MAX_CHALS, Challenges.NAME_IDS.length,
				"NAME_IDS is out of step with MAX_CHALS" );
	}

	@Test
	@DisplayName("every challenge occupies its own bit")
	public void everyChallengeHasItsOwnBit(){
		HashSet<Integer> seen = new HashSet<>();

		for (int mask : Challenges.MASKS){
			assertTrue( mask > 0, "a challenge mask must be positive, got " + mask );
			assertEquals( 1, Integer.bitCount( mask ),
					"challenge mask " + mask + " is not a single bit" );
			assertTrue( seen.add( mask ), "challenge mask " + mask + " is used twice" );
		}
	}

	@Test
	@DisplayName("MAX_VALUE covers exactly the declared challenges")
	public void maxValueCoversEveryChallenge(){
		int combined = 0;
		for (int mask : Challenges.MASKS){
			combined |= mask;
		}

		assertEquals( Challenges.MAX_VALUE, combined,
				"MAX_VALUE must be the union of every challenge bit" );
	}

	@Test
	@DisplayName("challenge names are present and unique")
	public void challengeNamesArePresentAndUnique(){
		HashSet<String> seen = new HashSet<>();

		for (String name : Challenges.NAME_IDS){
			assertTrue( name != null && !name.trim().isEmpty(), "a challenge name is blank" );
			assertTrue( seen.add( name ), "challenge name \"" + name + "\" is used twice" );
		}
	}

	@Test
	@DisplayName("counting active challenges matches the bits that are set")
	public void activeChallengeCountMatchesBits(){
		assertEquals( 0, Challenges.activeChallenges( 0 ) );
		assertEquals( Challenges.MAX_CHALS, Challenges.activeChallenges( Challenges.MAX_VALUE ) );

		for (int mask : Challenges.MASKS){
			assertEquals( 1, Challenges.activeChallenges( mask ) );
		}

		assertEquals( 2, Challenges.activeChallenges( Challenges.NO_FOOD | Challenges.DARKNESS ) );
	}

	@Test
	@DisplayName("bits outside the declared challenges are not counted")
	public void unknownBitsAreIgnored(){
		//old saves can carry retired challenge bits; they must not inflate the count or score
		int retiredBit = Challenges.MAX_VALUE + 1;

		assertEquals( 0, Challenges.activeChallenges( retiredBit ) );
		assertEquals( 1, Challenges.activeChallenges( retiredBit | Challenges.NO_ARMOR ) );
	}
}
