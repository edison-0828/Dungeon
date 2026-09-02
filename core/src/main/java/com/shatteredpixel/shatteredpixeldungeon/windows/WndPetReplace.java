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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.PetWhistle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public class WndPetReplace extends WndOptions {

	private final PetWhistle current;
	private final PetWhistle candidate;

	public WndPetReplace(PetWhistle current, PetWhistle candidate) {
		super(Messages.get(WndPetReplace.class, "title"),
				Messages.get(WndPetReplace.class, "message",
						current.quality().title(), current.appearance().title(), current.quality().bonusPercent(),
						candidate.quality().title(), candidate.appearance().title(), candidate.quality().bonusPercent(),
						PetBond.bonusText(current.appearance(), current.quality()),
						PetBond.bonusText(candidate.appearance(), candidate.quality())),
				Messages.get(WndPetReplace.class, "replace"),
				Messages.get(WndPetReplace.class, "keep"));
		this.current = current;
		this.candidate = candidate;
	}

	@Override
	protected void onSelect(int index) {
		if (index == 0) {
			current.replaceWith(candidate);
			if (Dungeon.level != null) {
				for (Heap heap : Dungeon.level.heaps.valueList()) {
					if (heap.items.contains(candidate)) {
						heap.remove(candidate);
						break;
					}
				}
			}
		}
	}
}
