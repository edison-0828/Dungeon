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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

final class TapAdBridge {

	private TapAdBridge() {}

	@JSFunctor
	public interface JsStringFn extends JSObject {
		void accept(String value);
	}

	@JSBody(script = "return !!(window.cinderAds && window.cinderAds.rewardedAvailable && window.cinderAds.rewardedAvailable());")
	static native boolean rewardedAvailable();

	@JSBody(script = "return !!(window.cinderAds && window.cinderAds.interstitialAvailable && window.cinderAds.interstitialAvailable());")
	static native boolean interstitialAvailable();

	@JSBody(params = {"onOk", "onErr"}, script =
			"if (!window.cinderAds || !window.cinderAds.showRewarded) { onErr('unavailable'); return; }" +
			"window.cinderAds.showRewarded(function (value) { onOk(value == null ? '' : String(value)); }, function (value) { onErr(value == null ? 'failed' : String(value)); });")
	static native void showRewarded(JsStringFn onOk, JsStringFn onErr);

	@JSBody(params = {"onDone"}, script =
			"if (!window.cinderAds || !window.cinderAds.showInterstitial) { onDone('skipped'); return; }" +
			"window.cinderAds.showInterstitial(function (value) { onDone(value == null ? '' : String(value)); });")
	static native void showInterstitial(JsStringFn onDone);
}
