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

final class KeyboardBridge {

	private KeyboardBridge() {}

	@JSFunctor
	public interface JsStringFn extends JSObject {
		void accept(String value);
	}

	@JSBody(params = {"initial", "multiline", "maxLength", "onChange", "onConfirm"}, script =
			"try {" +
			"if (!window.cinderTextEntry) {" +
			"  var api = window.cinderTextEntry = { open: false, onChange: null, onConfirm: null };" +
			"  if (typeof wx !== 'undefined' && wx && wx.onKeyboardInput) {" +
			"    wx.onKeyboardInput(function (res) {" +
			"      var value = res && res.value != null ? String(res.value) : '';" +
			"      if (api.onChange) api.onChange(value);" +
			"    });" +
			"    wx.onKeyboardConfirm(function (res) {" +
			"      api.open = false;" +
			"      var value = res && res.value != null ? String(res.value) : '';" +
			"      if (api.onConfirm) api.onConfirm(value);" +
			"    });" +
			"    wx.onKeyboardComplete(function (res) {" +
			"      api.open = false;" +
			"      var value = res && res.value != null ? String(res.value) : '';" +
			"      if (api.onChange) api.onChange(value);" +
			"    });" +
			"  }" +
			"}" +
			"var api = window.cinderTextEntry;" +
			"api.onChange = onChange;" +
			"api.onConfirm = onConfirm;" +
			"if (typeof wx !== 'undefined' && wx && typeof wx.showKeyboard === 'function') {" +
			"  if (api.open) return;" +
			"  api.open = true;" +
			"  wx.showKeyboard({" +
			"    defaultValue: initial || ''," +
			"    maxLength: maxLength > 0 ? maxLength : 500," +
			"    multiple: !!multiline," +
			"    confirmHold: false," +
			"    confirmType: 'done'," +
			"    fail: function () { api.open = false; }" +
			"  });" +
			"  return;" +
			"}" +
			"var node = document.getElementById('cinder-text-entry');" +
			"if (node) { try { node.focus(); } catch (e) {} return; }" +
			"node = document.createElement(multiline ? 'textarea' : 'input');" +
			"node.id = 'cinder-text-entry';" +
			"if (!multiline) node.type = 'text';" +
			"node.value = initial || '';" +
			"if (maxLength > 0) node.maxLength = maxLength;" +
			"node.setAttribute('style', 'position:fixed;left:0;right:0;bottom:0;width:100%;box-sizing:border-box;z-index:10000;font-size:16px;padding:8px;');" +
			"node.oninput = function () { if (api.onChange) api.onChange(node.value || ''); };" +
			"node.addEventListener('keydown', function (e) {" +
			"  if (!multiline && e.key === 'Enter') {" +
			"    e.preventDefault();" +
			"    api.open = false;" +
			"    if (api.onConfirm) api.onConfirm(node.value || '');" +
			"  }" +
			"});" +
			"if (document.body) document.body.appendChild(node);" +
			"api.open = true;" +
			"try { node.focus(); } catch (e) {}" +
			"} catch (e) {}")
	static native void show(String initial, boolean multiline, int maxLength, JsStringFn onChange, JsStringFn onConfirm);

	@JSBody(script =
			"try {" +
			"var api = window.cinderTextEntry;" +
			"if (!api) return;" +
			"var wasOpen = api.open;" +
			"api.open = false;" +
			"api.onChange = null;" +
			"api.onConfirm = null;" +
			"if (wasOpen && typeof wx !== 'undefined' && wx && typeof wx.hideKeyboard === 'function') {" +
			"  wx.hideKeyboard({});" +
			"}" +
			"var node = document.getElementById('cinder-text-entry');" +
			"if (node && node.parentNode) node.parentNode.removeChild(node);" +
			"} catch (e) {}")
	static native void hide();
}
