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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PetSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class PetAlly extends DirectableAlly {

	public enum Quality {
		COMMON   (1.00f, 0xF0E6C8, null),
		UNCOMMON (1.15f, 0x7AE87A, new ItemSprite.Glowing(0x3DDC84)),
		RARE     (1.30f, 0x6EB8FF, new ItemSprite.Glowing(0x4FC3F7)),
		LEGENDARY(1.50f, 0xFFD24A, new ItemSprite.Glowing(0xFFD54F, 0.6f));

		public final float statMul;
		public final int tint;
		public final ItemSprite.Glowing glow;

		Quality(float statMul, int tint, ItemSprite.Glowing glow) {
			this.statMul = statMul;
			this.tint = tint;
			this.glow = glow;
		}

		public String key() {
			return name().toLowerCase();
		}

		public String title() {
			return Messages.get(PetAlly.class, key());
		}

		public int bonusPercent() {
			return Math.round(BASE_STAT_MUL * statMul * 100f) - 100;
		}

		private static final float[] CHANCES = {45, 30, 18, 7};

		public static Quality roll() {
			return values()[Random.chances(CHANCES)];
		}
	}

	public enum Skill {
		EXPERIENCE, HEALTH, ACCURACY, ARMOR, EVASION, REGEN, ATTACK, STRENGTH, SPEED, CRIT;

		public String key() {
			return name().toLowerCase();
		}

		public String title() {
			return Messages.get(PetAlly.class, "skill_" + key());
		}
	}

	public enum Appearance {
		RAT   (Skill.EXPERIENCE),
		ALBINO(Skill.HEALTH),
		SNAKE (Skill.ACCURACY),
		CRAB  (Skill.ARMOR),
		BAT   (Skill.EVASION),
		SLIME (Skill.REGEN),
		BEE   (Skill.ATTACK),
		GNOLL (Skill.STRENGTH),
		SWARM (Skill.SPEED),
		SHEEP (Skill.CRIT);

		public final Skill skill;

		Appearance(Skill skill) {
			this.skill = skill;
		}

		public String key() {
			return name().toLowerCase();
		}

		public String title() {
			return Messages.get(PetAlly.class, key());
		}

		public static Appearance roll() {
			return Random.element(values());
		}
	}

	/** Global pet HP/damage multiplier, applied before quality. */
	public static final float BASE_STAT_MUL = 1.20f;

	{
		spriteClass = PetSprite.class;

		attacksAutomatically = true;
		intelligentAlly = true;
		state = WANDERING;

		EXP = 0;
		maxLvl = -2;
	}

	private Quality quality = Quality.COMMON;
	private Appearance appearance = Appearance.RAT;
	private boolean dismissed = false;

	public PetAlly() {
		super();
		updateStats();
		HP = HT;
	}

	public Quality quality() {
		return quality;
	}

	public Appearance appearance() {
		return appearance;
	}

	public void setIdentity(Quality quality, Appearance appearance) {
		if (quality == null) quality = Quality.COMMON;
		if (appearance == null) appearance = Appearance.RAT;
		boolean changed = this.quality != quality || this.appearance != appearance;
		this.quality = quality;
		this.appearance = appearance;
		if (changed) {
			updateStats();
			if (sprite instanceof PetSprite) {
				((PetSprite) sprite).applyIdentity(this);
			}
		}
	}

	public void applyStoredHealth(int storedHP, int storedHT) {
		updateStats();
		if (storedHT <= 0 || storedHP <= 0) {
			HP = Math.max(1, HT / 2);
		} else {
			HP = Math.min(HT, storedHP + Math.max(0, HT - storedHT));
		}
	}

	public void updateStats() {
		int lvl = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		int newHT = scaled(12 + 3 * lvl);
		if (HT != newHT) {
			if (HT > 0 && HP > 0) {
				HP = Math.max(1, HP + (newHT - HT));
			}
			HT = newHT;
		}
		defenseSkill = scaled(lvl + 2);
	}

	private int scaled(int value) {
		return Math.max(1, Math.round(value * BASE_STAT_MUL * quality.statMul));
	}

	@Override
	protected boolean act() {
		updateStats();
		PetWhistle whistle = PetWhistle.get();
		if (whistle != null) {
			whistle.linkPet(this);
		}
		if (!isAlive()) {
			return true;
		}
		return super.act();
	}

	@Override
	public CharSprite sprite() {
		PetSprite s = new PetSprite();
		s.applyIdentity(this);
		return s;
	}

	@Override
	public int attackSkill(Char target) {
		int lvl = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		return scaled(lvl + 6);
	}

	@Override
	public int damageRoll() {
		int lvl = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		return Random.NormalIntRange(scaled(1 + lvl / 6), scaled(4 + lvl / 4));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1 + Dungeon.depth / 10 + quality.ordinal());
	}

	@Override
	public float speed() {
		float speed = super.speed();
		if (state == WANDERING
				&& defendingPos == -1
				&& Dungeon.hero != null
				&& Dungeon.level.distance(pos, Dungeon.hero.pos) > 1) {
			speed *= 2;
		}
		return speed;
	}

	@Override
	protected boolean getCloser(int target) {
		if (super.getCloser(target)) {
			return true;
		}
		int step = bypassToward(target);
		if (step != -1) {
			move(step);
			return true;
		}
		return false;
	}

	/**
	 * Ordinary pathfinding treats the hero as a wall. In a 1-tile doorway that leaves the
	 * companion stranded in the corridor, so pick a cell that still gets it through the door.
	 */
	int bypassToward(int target) {
		if (Dungeon.level == null || Dungeon.hero == null) {
			return -1;
		}

		int best = -1;
		int bestDist = Dungeon.level.distance(pos, target);

		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = pos + i;
			if (!canStep(cell)) {
				continue;
			}
			if (!isDoor(cell) && !isDoor(pos)) {
				continue;
			}
			int dist = Dungeon.level.distance(cell, target);
			if (dist < bestDist) {
				bestDist = dist;
				best = cell;
			}
		}
		if (best != -1) {
			return best;
		}

		int heroPos = Dungeon.hero.pos;
		if (!Dungeon.level.adjacent(pos, heroPos)) {
			return -1;
		}
		if (!isDoor(heroPos) && !isDoor(pos)) {
			return -1;
		}

		best = -1;
		bestDist = Dungeon.level.distance(pos, target);
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = heroPos + i;
			if (cell == pos || !canStep(cell)) {
				continue;
			}
			int dist = Dungeon.level.distance(cell, target);
			if (dist < bestDist) {
				bestDist = dist;
				best = cell;
			}
		}
		return best;
	}

	private boolean canStep(int cell) {
		if (!Dungeon.level.insideMap(cell)) {
			return false;
		}
		if (!Dungeon.level.passable[cell] && !(flying && Dungeon.level.avoid[cell])) {
			return false;
		}
		return Actor.findChar(cell) == null;
	}

	private static boolean isDoor(int cell) {
		int terrain = Dungeon.level.map[cell];
		return terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR;
	}

	@Override
	public void move(int step, boolean travelling) {
		if (sprite == null) {
			if (Dungeon.level != null && Dungeon.level.map[pos] == Terrain.OPEN_DOOR) {
				com.shatteredpixel.shatteredpixeldungeon.levels.features.Door.leave(pos);
			}
			pos = step;
			if (Dungeon.level != null) {
				Dungeon.level.occupyCell(this);
			}
			return;
		}
		super.move(step, travelling);
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		Item.updateQuickslot();
	}

	public void dismiss() {
		dismissed = true;
		if (sprite != null) {
			sprite.killAndErase();
		}
		destroy();
	}

	@Override
	public void die(Object cause) {
		if (!dismissed) {
			yell(Messages.get(this, "defeated"));
		}
		super.die(cause);
	}

	@Override
	public void destroy() {
		PetWhistle whistle = PetWhistle.get();
		if (whistle != null) {
			whistle.onPetRemoved(this, !dismissed);
		}
		super.destroy();
	}

	@Override
	public String name() {
		return Messages.get(this, "name_bound", quality.title(), appearance.title());
	}

	@Override
	public String description() {
		return Messages.get(this, "desc",
				quality.title(),
				appearance.title(),
				quality.bonusPercent(),
				PetBond.bonusText(appearance, quality));
	}

	private static final String QUALITY = "pet_quality";
	private static final String APPEARANCE = "pet_appearance";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(QUALITY, quality.name());
		bundle.put(APPEARANCE, appearance.name());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		if (bundle.contains(QUALITY)) {
			try {
				quality = Quality.valueOf(bundle.getString(QUALITY));
			} catch (Exception ignored) {
				quality = Quality.COMMON;
			}
		}
		if (bundle.contains(APPEARANCE)) {
			try {
				appearance = Appearance.valueOf(bundle.getString(APPEARANCE));
			} catch (Exception ignored) {
				appearance = Appearance.RAT;
			}
		}
		super.restoreFromBundle(bundle);
	}
}
