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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

public class WndInfoBuff extends Window {

	private static final float GAP	= 2;

	private static final int WIDTH = 120;

	public WndInfoBuff(Buff buff){
		super();

		IconTitle titlebar = new IconTitle();

		Image buffIcon = new BuffIcon( buff, true );

		titlebar.icon( buffIcon );
		int titleColor = buff.type == Buff.buffType.NEGATIVE
				? severityColor(buff.severity)
				: Window.TITLE_COLOR;
		titlebar.label( Messages.titleCase(buff.name()), titleColor );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		float contentTop = titlebar.bottom() + 2*GAP;
		String summary = buff.summary();
		if (buff.type == Buff.buffType.NEGATIVE && !summary.isEmpty()) {
			RenderedTextBlock txtSummary = PixelScene.renderTextBlock(
					Messages.get(WndInfoBuff.class, "negative_summary",
							Messages.get(WndInfoBuff.class, buff.severity.name().toLowerCase()),
							summary), 6);
			txtSummary.hardlight(titleColor);
			txtSummary.maxWidth(WIDTH);
			txtSummary.setPos(titlebar.left(), contentTop);
			add(txtSummary);
			contentTop = txtSummary.bottom() + 2*GAP;
		}

		RenderedTextBlock txtInfo = PixelScene.renderTextBlock(buff.desc(), 6);
		txtInfo.maxWidth(WIDTH);
		txtInfo.setPos(titlebar.left(), contentTop);
		add( txtInfo );

		resize( WIDTH, (int)txtInfo.bottom() + 2 );
	}

	private static int severityColor(Buff.debuffSeverity severity){
		switch (severity){
			case CRITICAL:
				return CharSprite.NEGATIVE;
			case MAJOR:
				return CharSprite.WARNING;
			default:
				return 0xFFD34E;
		}
	}
}
