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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Bundle is the single serialization mechanism for every save file in the game, and the
 * game's entire backward-compatibility strategy rests on two of its behaviours:
 * absent keys must degrade to sane defaults, and renamed classes must be redirectable
 * through aliases. These tests pin down that contract.
 */
public class BundleTest {

	public enum Flavour {
		SWEET, SOUR, BITTER
	}

	public static class Leaf implements Bundlable {

		public int hp;
		public String label;

		public Leaf(){
		}

		public Leaf( int hp, String label ){
			this.hp = hp;
			this.label = label;
		}

		@Override
		public void storeInBundle( Bundle bundle ){
			bundle.put( "hp", hp );
			bundle.put( "label", label );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			hp = bundle.getInt( "hp" );
			label = bundle.getString( "label" );
		}
	}

	public static class Branch implements Bundlable {

		public Leaf child;
		public Flavour flavour;

		@Override
		public void storeInBundle( Bundle bundle ){
			bundle.put( "child", child );
			bundle.put( "flavour", flavour );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			child = (Leaf) bundle.get( "child" );
			flavour = bundle.getEnum( "flavour", Flavour.class );
		}
	}

	/** Non-static inner classes cannot be rebuilt by reflection and are documented as skipped. */
	public class Inner implements Bundlable {
		@Override
		public void storeInBundle( Bundle bundle ){
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
		}
	}

	private static Bundle reread( Bundle bundle ) throws IOException {
		return reread( bundle, true );
	}

	private static Bundle reread( Bundle bundle, boolean compressed ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( bundle, out, compressed ), "writing the bundle should succeed" );
		return Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
	}

	@Nested
	@DisplayName("backward compatibility contract")
	class BackwardCompatibility {

		@Test
		@DisplayName("absent keys degrade to defaults instead of throwing")
		public void absentKeysDegradeToDefaults(){
			//every restoreFromBundle in the game relies on this when loading an older save
			//that predates a newly added field
			Bundle bundle = new Bundle();

			assertFalse( bundle.contains( "missing" ) );
			assertEquals( 0, bundle.getInt( "missing" ) );
			assertEquals( 0L, bundle.getLong( "missing" ) );
			assertEquals( 0f, bundle.getFloat( "missing" ) );
			assertFalse( bundle.getBoolean( "missing" ) );
			assertEquals( "", bundle.getString( "missing" ) );
			assertNull( bundle.get( "missing" ) );
			assertTrue( bundle.getBundle( "missing" ).isNull() );
			assertTrue( bundle.getCollection( "missing" ).isEmpty() );
		}

		@Test
		@DisplayName("an absent enum falls back to the first constant")
		public void absentEnumFallsBackToFirstConstant(){
			assertSame( Flavour.SWEET, new Bundle().getEnum( "missing", Flavour.class ) );
		}

		@Test
		@DisplayName("an enum constant that no longer exists falls back to the first constant")
		public void removedEnumConstantFallsBack(){
			//this is what happens to a save referencing a talent or badge that was deleted
			Bundle bundle = new Bundle();
			bundle.put( "flavour", "UMAMI" );

			assertSame( Flavour.SWEET, bundle.getEnum( "flavour", Flavour.class ) );
		}

		@Test
		@DisplayName("a renamed class is redirected through its alias")
		public void aliasRedirectsRenamedClass() throws IOException {
			String legacyName = "com.watabou.utils.BundleTest$RenamedAwayLeaf";
			Bundle original = new Bundle();
			original.put( "item", new Leaf( 7, "kept" ) );

			//simulate a save written before the class was renamed
			String legacyJson = original.toString().replace( Leaf.class.getName(), legacyName );
			Bundle.addAlias( Leaf.class, legacyName );

			Bundle restored = Bundle.read(
					new ByteArrayInputStream( legacyJson.getBytes( StandardCharsets.UTF_8 ) ) );
			Leaf leaf = (Leaf) restored.get( "item" );

			assertNotNull( leaf, "the alias should have resolved the old class name" );
			assertEquals( 7, leaf.hp );
			assertEquals( "kept", leaf.label );
		}

		@Test
		@DisplayName("an unknown class yields null rather than aborting the whole load")
		public void unknownClassYieldsNull() throws IOException {
			//a stack trace on stderr is expected here; the point is that loading continues
			Bundle original = new Bundle();
			original.put( "item", new Leaf( 1, "gone" ) );
			original.put( "keeper", new Leaf( 2, "survives" ) );

			String json = original.toString()
					.replace( "\"" + Leaf.class.getName() + "\"", "\"com.watabou.utils.NoSuchClass\"" );
			Bundle restored = Bundle.read(
					new ByteArrayInputStream( json.getBytes( StandardCharsets.UTF_8 ) ) );

			//both entries referenced the same class name, so both drop out, but read() succeeded
			assertNull( restored.get( "item" ) );
			assertNull( restored.get( "keeper" ) );
		}
	}

	@Nested
	@DisplayName("round trips through a stream")
	class RoundTrips {

		@Test
		@DisplayName("primitives survive a compressed round trip")
		public void primitivesSurviveRoundTrip() throws IOException {
			Bundle bundle = new Bundle();
			bundle.put( "flag", true );
			bundle.put( "count", -42 );
			bundle.put( "seed", 5429503678975L );
			bundle.put( "ratio", 0.375f );
			bundle.put( "name", "Goo" );

			Bundle restored = reread( bundle );

			assertTrue( restored.getBoolean( "flag" ) );
			assertEquals( -42, restored.getInt( "count" ) );
			assertEquals( 5429503678975L, restored.getLong( "seed" ) );
			assertEquals( 0.375f, restored.getFloat( "ratio" ) );
			assertEquals( "Goo", restored.getString( "name" ) );
		}

		@Test
		@DisplayName("arrays survive a round trip, including empty ones")
		public void arraysSurviveRoundTrip() throws IOException {
			Bundle bundle = new Bundle();
			bundle.put( "ints", new int[]{ 3, 0, -7 } );
			bundle.put( "longs", new long[]{ Long.MAX_VALUE, 0 } );
			bundle.put( "floats", new float[]{ 1.5f, -0.25f } );
			bundle.put( "bools", new boolean[]{ true, false, true } );
			bundle.put( "strings", new String[]{ "a", "", "c" } );
			bundle.put( "empty", new int[0] );

			Bundle restored = reread( bundle );

			assertArrayEquals( new int[]{ 3, 0, -7 }, restored.getIntArray( "ints" ) );
			assertArrayEquals( new long[]{ Long.MAX_VALUE, 0 }, restored.getLongArray( "longs" ) );
			assertArrayEquals( new float[]{ 1.5f, -0.25f }, restored.getFloatArray( "floats" ) );
			assertArrayEquals( new boolean[]{ true, false, true }, restored.getBooleanArray( "bools" ) );
			assertArrayEquals( new String[]{ "a", "", "c" }, restored.getStringArray( "strings" ) );
			assertArrayEquals( new int[0], restored.getIntArray( "empty" ) );
		}

		@Test
		@DisplayName("nested bundlables and enums survive a round trip")
		public void nestedBundlablesSurviveRoundTrip() throws IOException {
			Branch branch = new Branch();
			branch.child = new Leaf( 12, "inner" );
			branch.flavour = Flavour.BITTER;

			Bundle bundle = new Bundle();
			bundle.put( "root", branch );

			Branch restored = (Branch) reread( bundle ).get( "root" );

			assertNotNull( restored );
			assertSame( Flavour.BITTER, restored.flavour );
			assertNotNull( restored.child );
			assertEquals( 12, restored.child.hp );
			assertEquals( "inner", restored.child.label );
		}

		@Test
		@DisplayName("collections keep their order")
		public void collectionsKeepOrder() throws IOException {
			List<Leaf> leaves = Arrays.asList(
					new Leaf( 1, "first" ),
					new Leaf( 2, "second" ),
					new Leaf( 3, "third" ) );

			Bundle bundle = new Bundle();
			bundle.put( "leaves", leaves );

			Collection<Bundlable> restored = reread( bundle ).getCollection( "leaves" );

			assertEquals( 3, restored.size() );
			int expectedHp = 1;
			for (Bundlable each : restored){
				Leaf leaf = assertInstanceOf( Leaf.class, each );
				assertEquals( expectedHp++, leaf.hp );
			}
		}

		@Test
		@DisplayName("class references survive a round trip")
		public void classReferencesSurviveRoundTrip() throws IOException {
			Bundle bundle = new Bundle();
			bundle.put( "type", Leaf.class );

			assertEquals( Leaf.class, reread( bundle ).getClass( "type" ) );
		}

		@Test
		@DisplayName("uncompressed saves are still readable, and compression is detected per file")
		public void compressionIsAutoDetected() throws IOException {
			Bundle bundle = new Bundle();
			bundle.put( "depth", 26 );

			assertEquals( 26, reread( bundle, true ).getInt( "depth" ) );
			assertEquals( 26, reread( bundle, false ).getInt( "depth" ) );
		}

		@Test
		@DisplayName("compressed and uncompressed payloads are actually different on the wire")
		public void compressionActuallyCompresses() throws IOException {
			Bundle bundle = new Bundle();
			bundle.put( "padding", new int[512] );

			ByteArrayOutputStream compressed = new ByteArrayOutputStream();
			ByteArrayOutputStream plain = new ByteArrayOutputStream();
			Bundle.write( bundle, compressed, true );
			Bundle.write( bundle, plain, false );

			byte[] gzipped = compressed.toByteArray();
			assertEquals( (byte) 0x1f, gzipped[0], "gzip magic byte 0" );
			assertEquals( (byte) 0x8b, gzipped[1], "gzip magic byte 1" );
			assertTrue( gzipped.length < plain.toByteArray().length );
		}
	}

	@Nested
	@DisplayName("defensive behaviour")
	class Defensive {

		@Test
		@DisplayName("malformed data raises IOException instead of a raw JSON error")
		public void malformedDataRaisesIOException(){
			byte[] garbage = "not json at all".getBytes( StandardCharsets.UTF_8 );

			//a stack trace on stderr is expected here
			try {
				Bundle.read( new ByteArrayInputStream( garbage ) );
				throw new AssertionError( "expected an IOException" );
			} catch (IOException expected){
				//callers such as GamesInProgress.check() only handle IOException
			}
		}

		@Test
		@DisplayName("null bundlables are dropped rather than stored")
		public void nullBundlablesAreDropped(){
			Bundle bundle = new Bundle();
			bundle.put( "nothing", (Bundlable) null );

			assertFalse( bundle.contains( "nothing" ) );
		}

		@Test
		@DisplayName("non-static inner classes are skipped when storing collections")
		public void innerClassesAreSkipped() throws IOException {
			ArrayList<Bundlable> mixed = new ArrayList<>();
			mixed.add( new Leaf( 5, "kept" ) );
			mixed.add( new Inner() );

			Bundle bundle = new Bundle();
			bundle.put( "mixed", mixed );

			Collection<Bundlable> restored = reread( bundle ).getCollection( "mixed" );

			assertEquals( 1, restored.size(), "the inner class must not be written out" );
			assertInstanceOf( Leaf.class, restored.iterator().next() );
		}

		@Test
		@DisplayName("removing a key clears it")
		public void removingAKeyClearsIt(){
			Bundle bundle = new Bundle();
			bundle.put( "temp", 1 );

			assertTrue( bundle.remove( "temp" ) );
			assertFalse( bundle.contains( "temp" ) );
		}
	}
}
