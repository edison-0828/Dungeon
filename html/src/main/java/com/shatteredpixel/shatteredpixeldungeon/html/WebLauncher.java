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

package com.shatteredpixel.shatteredpixeldungeon.html;

import com.badlogic.gdx.Files;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.news.NewsImpl;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.UpdateImpl;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

public class WebLauncher {

	public static void main(String[] args) {
		Game.version = "1.0.4";
		Game.versionCode = 901;

		Updates.service = UpdateImpl.getUpdateService();
		if (NewsImpl.supportsNews()) {
			News.service = NewsImpl.getNewsService();
		}

		FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
		Bundle.setCompressByDefault(false);

		WebApplicationConfiguration config = new WebApplicationConfiguration();
		config.storagePrefix = "cindervault";
		config.width = 480;
		config.height = 800;
		new WebApplication(new ShatteredPixelDungeon(new WebPlatformSupport()), config);
	}
}
