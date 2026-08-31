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

package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Generator.Category pairs an array of item classes with a parallel array of drop weights,
 * wired up by hand in a static block. Adding an item without extending the weight arrays is
 * the easiest way to break item generation, and it only shows up as a crash or a silently
 * unobtainable item deep into a run. These tests check the arrays line up.
 */
public class GeneratorCategoryTest {

	/**
	 * WEAPON and MISSILE hold no classes of their own; Generator.random() switches on them and
	 * delegates to randomWeapon() / randomMissile(), which pick one of the per-tier categories.
	 */
	private static boolean isDispatchOnly( Category category ){
		return category.classes.length == 0;
	}

	@Test
	@DisplayName("every category has classes and weights, or delegates to other categories")
	public void everyCategoryIsPopulated(){
		for (Category category : Category.values()){
			assertNotNull( category.classes, category + " has no item classes" );
			assertNotNull( category.probs, category + " has no drop weights" );

			if (isDispatchOnly( category )){
				//a delegating category must not carry stray weights either
				assertEquals( 0, category.probs.length,
						category + " has no classes but still declares weights" );
			}
		}
	}

	@Test
	@DisplayName("weight arrays are the same length as the class arrays")
	public void weightArraysMatchClassArrays(){
		for (Category category : Category.values()){
			assertEquals( category.classes.length, category.probs.length,
					category + " has " + category.classes.length + " classes but "
							+ category.probs.length + " weights" );
		}
	}

	@Test
	@DisplayName("deck templates are the same length as the class arrays")
	public void deckTemplatesMatchClassArrays(){
		for (Category category : Category.values()){
			if (category.defaultProbs != null){
				assertEquals( category.classes.length, category.defaultProbs.length,
						category + " defaultProbs is out of step with its classes" );
			}
			if (category.defaultProbs2 != null){
				assertEquals( category.classes.length, category.defaultProbs2.length,
						category + " defaultProbs2 is out of step with its classes" );
			}
			if (category.defaultProbsTotal != null){
				assertEquals( category.classes.length, category.defaultProbsTotal.length,
						category + " defaultProbsTotal is out of step with its classes" );
			}
		}
	}

	@Test
	@DisplayName("a category using a second deck also declares the first and the total")
	public void secondDeckImpliesTheOthers(){
		for (Category category : Category.values()){
			if (category.defaultProbs2 != null){
				assertNotNull( category.defaultProbs,
						category + " declares a second deck but no first deck" );
				assertNotNull( category.defaultProbsTotal,
						category + " declares a second deck but no combined deck" );
			}
		}
	}

	@Test
	@DisplayName("no category has negative weights")
	public void noNegativeWeights(){
		for (Category category : Category.values()){
			assertAllNonNegative( category, "probs", category.probs );
			assertAllNonNegative( category, "defaultProbs", category.defaultProbs );
			assertAllNonNegative( category, "defaultProbs2", category.defaultProbs2 );
			assertAllNonNegative( category, "defaultProbsTotal", category.defaultProbsTotal );
		}
	}

	@Test
	@DisplayName("every category can actually drop something on a fresh game")
	public void everyCategoryCanDropSomething(){
		//Random.chances() returns -1 for an all-zero deck, which would make the category
		//undroppable rather than merely rare
		for (Category category : Category.values()){
			if (isDispatchOnly( category )) continue;

			assertTrue( sum( category.probs ) > 0,
					category + " has no positive weight, so it can never drop" );
		}
	}

	@Test
	@DisplayName("a starting deck is dealt from its own template, not another category's")
	public void startingDeckComesFromItsOwnTemplate(){
		//Each `probs = defaultProbs.clone()` line is written out by hand per category, so it is
		//easy to paste the wrong category name and quietly deal a neighbour's deck. This assumes
		//no game has been started in this JVM, which would legitimately reshuffle the decks.
		for (Category category : Category.values()){
			if (category.defaultProbs == null) continue;

			boolean matchesFirstDeck = Arrays.equals( category.probs, category.defaultProbs );
			boolean matchesSecondDeck = category.defaultProbs2 != null
					&& Arrays.equals( category.probs, category.defaultProbs2 );

			assertTrue( matchesFirstDeck || matchesSecondDeck,
					category + " starts with " + Arrays.toString( category.probs )
							+ " but its template is " + Arrays.toString( category.defaultProbs ) );
		}
	}

	@Test
	@DisplayName("every listed class actually belongs to its category")
	public void listedClassesMatchTheCategorySuperclass(){
		for (Category category : Category.values()){
			assertNotNull( category.superClass, category + " has no superclass" );

			for (Class<?> cls : category.classes){
				assertNotNull( cls, category + " lists a null class" );
				assertTrue( category.superClass.isAssignableFrom( cls ),
						category + " lists " + cls.getSimpleName()
								+ ", which is not a " + category.superClass.getSimpleName() );
			}
		}
	}

	@Test
	@DisplayName("no category lists the same item twice")
	public void noDuplicateClassesWithinACategory(){
		for (Category category : Category.values()){
			for (int i = 0; i < category.classes.length; i++){
				for (int j = i + 1; j < category.classes.length; j++){
					assertTrue( category.classes[i] != category.classes[j],
							category + " lists " + category.classes[i].getSimpleName() + " twice" );
				}
			}
		}
	}

	@Test
	@DisplayName("a fresh deck starts as a copy of its template, not a shared reference")
	public void freshDeckIsACopyOfItsTemplate(){
		//probs is decremented as items are drawn, so sharing the array with defaultProbs
		//would permanently consume the template
		for (Category category : Category.values()){
			if (category.defaultProbs != null){
				assertTrue( category.probs != category.defaultProbs,
						category + " shares its live deck with its template" );
			}
		}
	}

	private static void assertAllNonNegative( Category category, String name, float[] weights ){
		if (weights == null) return;

		for (int i = 0; i < weights.length; i++){
			assertTrue( weights[i] >= 0,
					category + "." + name + "[" + i + "] is negative: " + weights[i] );
		}
	}

	private static float sum( float[] weights ){
		float total = 0;
		for (float weight : weights){
			total += weight;
		}
		return total;
	}
}
