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

final class CinderWalletBridge {

	private CinderWalletBridge() {}

	@JSFunctor
	public interface JsStringFn extends JSObject {
		void accept(String value);
	}

	@JSBody(script = "var config = window.cinderWalletConfig; return config && config.recipient ? String(config.recipient) : '';")
	static native String recipient();

	@JSBody(script = "var config = window.cinderWalletConfig; return config && config.lamports ? String(config.lamports) : '';")
	static native String lamports();

	@JSBody(script = "return typeof navigator === 'undefined' || navigator.onLine !== false;")
	static native boolean online();

	@JSBody(params = {"onOk", "onErr"}, script = "cinderWalletCall('connect', [], onOk, onErr);")
	static native void connect(JsStringFn onOk, JsStringFn onErr);

	@JSBody(params = {"onOk", "onErr"}, script = "cinderWalletCall('disconnect', [], onOk, onErr);")
	static native void disconnect(JsStringFn onOk, JsStringFn onErr);

	@JSBody(params = {"lamports", "memo", "onOk", "onErr"}, script = "cinderWalletCall('transfer', [lamports, memo], onOk, onErr);")
	static native void transfer(String lamports, String memo, JsStringFn onOk, JsStringFn onErr);

	@JSBody(params = {"owner", "onOk", "onErr"}, script = "cinderWalletCall('packRecord', [owner], onOk, onErr);")
	static native void packRecord(String owner, JsStringFn onOk, JsStringFn onErr);

	@JSBody(script =
			"if (typeof window.cinderWalletCall === 'function') return;" +
			"window.cinderWalletCall = function (method, args, onOk, onErr) {" +
			"  var wallet = window.cinderWallet;" +
			"  if (!wallet || typeof wallet[method] !== 'function') { onErr('FAILED\\nmissing wallet'); return; }" +
			"  Promise.resolve(wallet[method].apply(wallet, args)).then(function (value) {" +
			"    onOk(value == null ? '' : String(value));" +
			"  }, function (error) {" +
			"    var packed = wallet.formatError ? wallet.formatError(error) : ('FAILED\\n' + error);" +
			"    onErr(packed == null ? 'FAILED\\n' : String(packed));" +
			"  });" +
			"};")
	static native void install();
}
