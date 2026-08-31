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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** A mandatory once-per-run choice. The pending flag is only cleared after a selection. */
public class WndBeginnerReward extends WndOptions {

	public WndBeginnerReward() {
		super(Messages.get(WndBeginnerReward.class, "title"),
				Messages.get(WndBeginnerReward.class, "message"),
				Messages.get(WndBeginnerReward.class, "weapon",
						Messages.titleCase(BeginnerAid.starterWeapon(Dungeon.hero).name())),
				Messages.get(WndBeginnerReward.class, "armor"),
				Messages.get(WndBeginnerReward.class, "supply",
						Messages.titleCase(supply().name())));
	}

	private static Item supply() {
		return BeginnerAid.starterSupply(Dungeon.hero);
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
