/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.BeginnerAid;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** A mandatory once-per-run choice among three random companions. */
public class WndBeginnerReward extends WndOptions {

	public WndBeginnerReward() {
		super(Messages.get(WndBeginnerReward.class, "title"),
				Messages.get(WndBeginnerReward.class, "message"),
				option(0), option(1), option(2));
	}

	private static String option(int index) {
		BeginnerAid.PetOffer[] offers = BeginnerAid.starterPets();
		if (index >= offers.length || offers[index] == null) {
			return Messages.get(WndBeginnerReward.class, "pet", "?");
		}
		BeginnerAid.PetOffer offer = offers[index];
		return Messages.get(WndBeginnerReward.class, "pet",
				offer.appearance.title(), offer.appearance.skill.title());
	}

	@Override
	protected void onSelect(int index) {
		BeginnerAid.claimStarterReward(index);
	}

	@Override
	public void onBackPressed() {
		//Choosing is part of starting the run; do not silently discard the reward.
	}
}
