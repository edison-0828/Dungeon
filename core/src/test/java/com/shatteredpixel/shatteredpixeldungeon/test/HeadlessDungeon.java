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

package com.shatteredpixel.shatteredpixeldungeon.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.BeginnerAid;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/**
 * Boots the smallest slice of the game that dungeon logic needs in order to run from a plain JUnit
 * test, with no window, no OpenGL context and no player data in reach.
 *
 * <p>Three separate things have to be in place before a single level will generate:
 * <ul>
 *     <li>{@code Gdx.files}, because {@link Messages} resolves its string bundles through it and
 *         {@link FileUtils} routes every save file through it.</li>
 *     <li>{@code Gdx.app}, because settings are backed by libGDX preferences.</li>
 *     <li>The global statics on {@link Dungeon}, which levelgen reads directly.</li>
 * </ul>
 * The headless libGDX backend supplies the first two. It deliberately leaves {@code Gdx.gl} and
 * {@code Gdx.audio} null, so anything that draws or plays a sound is still out of bounds here — that
 * is a feature, not a gap, since it keeps tests honest about touching presentation code.
 */
public final class HeadlessDungeon {

	private HeadlessDungeon() {}

	private static boolean booted;
	private static Path sandbox;

	/**
	 * Stands up libGDX once per test JVM. Repeated calls are ignored, which matters because the game
	 * keeps its state in statics: a second application would orphan the bundles and preferences that
	 * the already-loaded classes captured.
	 */
	public static synchronized void boot() {
		if (booted) return;

		try {
			sandbox = Files.createTempDirectory("spd-headless-test");
		} catch (IOException e) {
			throw new UncheckedIOException("could not create a sandbox for the test run", e);
		}
		sandbox.toFile().deleteOnExit();

		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		//the listener below draws nothing, so the loop only needs to tick often enough to stay responsive
		config.updatesPerSecond = 1;
		//headless preferences are always resolved against the user's home dir, so this has to stay a
		//relative name - and it must not be libGDX's default ".prefs/", which is where a real install
		//of the game keeps the player's actual settings
		config.preferencesDirectory = ".shattered-pixel-dungeon-test-prefs" + File.separator;
		new HeadlessApplication(new ApplicationAdapter() {}, config);

		//every save, bones file and settings write lands inside the sandbox, never in a real save slot
		FileUtils.setDefaultFileProperties(com.badlogic.gdx.Files.FileType.Absolute,
				sandbox.resolve("saves").toString() + File.separator);

		//a launcher normally fills these in from the jar manifest, and DeviceCompat.isDebug() reads
		//Game.version without a null check, so leaving them unset breaks anything that consults it
		Game.version = System.getProperty("Specification-Version", "0.0.0");
		Game.versionCode = Integer.parseInt(System.getProperty("Implementation-Version", "0"));

		//pin the language so assertions do not depend on whatever the developer last played in
		Messages.setup(Languages.ENGLISH);

		booted = true;
	}

	/**
	 * Puts the globals into the same shape {@link Dungeon#init()} would, for a fresh run on the given
	 * seed.
	 *
	 * <p>This mirrors {@code Dungeon.init()} rather than calling it, for two reasons: that method
	 * pulls the seed and the challenge set out of user settings, and it resets on-screen widgets
	 * ({@code QuickSlotButton}, {@code Toolbar}) whose classes drag in textures we have no context
	 * for. Everything here that levelgen actually reads is kept in the same order as the original so
	 * the two stay comparable when {@code Dungeon.init()} changes.
	 */
	public static void startRun(long seed, int challenges) {
		boot();
		BeginnerAid.resetForRun();

		Dungeon.initialVersion = Dungeon.version = Game.versionCode;
		Dungeon.challenges = challenges;
		Dungeon.mobsToChampion = 1;

		//Journal progress belongs to the player, not the run, so Dungeon.init() has no reason to touch
		//it - but levelgen reads it. A floor only drops a guide page while pages are still missing, and
		//an item landing on a hidden trap disarms it, so a seed reproduces the same floor only for a
		//player at the same point in the guide. Clearing it here is what lets one test's dungeon be
		//compared against another's.
		for (Document document : Document.values()) {
			for (String page : document.pageNames().toArray(new String[0])) {
				document.deletePage(page);
			}
		}

		Actor.clear();
		Actor.resetNextID();

		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.seed = seed;

		//offset seed slightly to avoid output patterns, as the game does
		Random.pushGenerator(seed + 1);

			Scroll.initLabels();
			Potion.initColors();
			Ring.initGems();

			SpecialRoom.initForRun();
			SecretRoom.initForRun();

			Generator.fullReset();

		Random.resetGenerators();

		Statistics.reset();
		Notes.reset();
		Dungeon.quickslot.reset();

		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.generatedLevels.clear();

		Dungeon.gold = 0;
		Dungeon.energy = 0;
		Dungeon.droppedItems = new SparseArray<>();

		Dungeon.LimitedDrops.reset();
		Dungeon.chapters = new HashSet<>();

		Ghost.Quest.reset();
		Wandmaker.Quest.reset();
		Blacksmith.Quest.reset();
		Imp.Quest.reset();

		Dungeon.hero = new Hero();
		Dungeon.hero.live();

		Badges.reset();

		HeroClass.WARRIOR.initHero(Dungeon.hero);

		Dungeon.level = null;
	}

	public static void startRun(long seed) {
		startRun(seed, 0);
	}
}
