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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PetAlly;
import com.watabou.noosa.TextureFilm;

public class PetSprite extends MobSprite {

	private PetAlly.Quality tintedAs = PetAlly.Quality.COMMON;

	public PetSprite() {
		super();
		setup(PetAlly.Appearance.RAT, PetAlly.Quality.COMMON);
	}

	@Override
	public void link(Char ch) {
		if (ch instanceof PetAlly) {
			applyIdentity((PetAlly) ch);
		}
		super.link(ch);
		applyTint();
	}

	public void applyIdentity(PetAlly pet) {
		setup(pet.appearance(), pet.quality());
	}

	private void setup(PetAlly.Appearance appearance, PetAlly.Quality quality) {
		switch (appearance) {
			case ALBINO:
				setupRat(16);
				break;
			case SNAKE:
				setupSnake();
				break;
			case CRAB:
				setupCrab();
				break;
			case BAT:
				setupBat();
				break;
			case SLIME:
				setupSlime();
				break;
			case BEE:
				setupBee();
				break;
			case GNOLL:
				setupGnoll();
				break;
			case SWARM:
				setupSwarm();
				break;
			case SHEEP:
				setupSheep();
				break;
			case RAT:
			default:
				setupRat(0);
				break;
		}
		tintedAs = quality == null ? PetAlly.Quality.COMMON : quality;
		applyTint();
		play(idle);
	}

	private void setupRat(int c) {
		texture(Assets.Sprites.RAT);
		TextureFilm frames = new TextureFilm(texture, 16, 15);
		idle = new Animation(2, true);
		idle.frames(frames, c+0, c+0, c+0, c+1);
		run = new Animation(10, true);
		run.frames(frames, c+6, c+7, c+8, c+9, c+10);
		attack = new Animation(15, false);
		attack.frames(frames, c+2, c+3, c+4, c+5, c+0);
		die = new Animation(10, false);
		die.frames(frames, c+11, c+12, c+13, c+14);
	}

	private void setupSnake() {
		texture(Assets.Sprites.SNAKE);
		TextureFilm frames = new TextureFilm(texture, 12, 11);
		idle = new Animation(10, true);
		idle.frames(frames, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 1, 1);
		run = new Animation(8, true);
		run.frames(frames, 4, 5, 6, 7);
		attack = new Animation(15, false);
		attack.frames(frames, 8, 9, 10, 9, 0);
		die = new Animation(10, false);
		die.frames(frames, 11, 12, 13);
	}

	private void setupCrab() {
		texture(Assets.Sprites.CRAB);
		TextureFilm frames = new TextureFilm(texture, 16, 16);
		idle = new Animation(5, true);
		idle.frames(frames, 0, 1, 0, 2);
		run = new Animation(15, true);
		run.frames(frames, 3, 4, 5, 6);
		attack = new Animation(12, false);
		attack.frames(frames, 7, 8, 9);
		die = new Animation(12, false);
		die.frames(frames, 10, 11, 12, 13);
	}

	private void setupBat() {
		texture(Assets.Sprites.BAT);
		TextureFilm frames = new TextureFilm(texture, 15, 15);
		idle = new Animation(8, true);
		idle.frames(frames, 0, 1);
		run = new Animation(12, true);
		run.frames(frames, 0, 1);
		attack = new Animation(12, false);
		attack.frames(frames, 2, 3, 0, 1);
		die = new Animation(12, false);
		die.frames(frames, 4, 5, 6);
	}

	private void setupSlime() {
		texture(Assets.Sprites.SLIME);
		TextureFilm frames = new TextureFilm(texture, 14, 12);
		idle = new Animation(3, true);
		idle.frames(frames, 0, 1, 1, 0);
		run = new Animation(10, true);
		run.frames(frames, 0, 2, 3, 3, 2, 0);
		attack = new Animation(15, false);
		attack.frames(frames, 2, 3, 4, 6, 5);
		die = new Animation(10, false);
		die.frames(frames, 0, 5, 6, 7);
	}

	private void setupBee() {
		texture(Assets.Sprites.BEE);
		TextureFilm frames = new TextureFilm(texture, 16, 16);
		idle = new Animation(12, true);
		idle.frames(frames, 0, 1, 1, 0, 2, 2);
		run = new Animation(15, true);
		run.frames(frames, 0, 1, 1, 0, 2, 2);
		attack = new Animation(20, false);
		attack.frames(frames, 3, 4, 5, 6);
		die = new Animation(20, false);
		die.frames(frames, 7, 8, 9, 10);
	}

	private void setupGnoll() {
		texture(Assets.Sprites.GNOLL);
		TextureFilm frames = new TextureFilm(texture, 12, 15);
		idle = new Animation(2, true);
		idle.frames(frames, 0, 0, 0, 1, 0, 0, 1, 1);
		run = new Animation(12, true);
		run.frames(frames, 4, 5, 6, 7);
		attack = new Animation(12, false);
		attack.frames(frames, 2, 3, 0);
		die = new Animation(12, false);
		die.frames(frames, 8, 9, 10);
	}

	private void setupSwarm() {
		texture(Assets.Sprites.SWARM);
		TextureFilm frames = new TextureFilm(texture, 16, 16);
		idle = new Animation(15, true);
		idle.frames(frames, 0, 1, 2, 3, 4, 5);
		run = new Animation(15, true);
		run.frames(frames, 0, 1, 2, 3, 4, 5);
		attack = new Animation(20, false);
		attack.frames(frames, 6, 7, 8, 9);
		die = new Animation(15, false);
		die.frames(frames, 10, 11, 12, 13, 14);
	}

	private void setupSheep() {
		texture(Assets.Sprites.SHEEP);
		TextureFilm frames = new TextureFilm(texture, 16, 15);
		//the sheep sheet only holds four poses; idle mostly sits still and grazes now and then
		idle = new Animation(6, true);
		idle.frames(frames, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 0);
		run = new Animation(10, true);
		run.frames(frames, 1, 2, 3, 2);
		attack = new Animation(15, false);
		attack.frames(frames, 3, 2, 1, 0);
		die = new Animation(15, false);
		die.frames(frames, 0);
	}

	@Override
	public void resetColor() {
		super.resetColor();
		// Visual.<init> calls this before PetSprite field initializers run.
		applyTint();
	}

	private void applyTint() {
		if (tintedAs != null) {
			hardlight(tintedAs.tint);
		}
	}
}
