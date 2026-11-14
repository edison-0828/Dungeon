/* wechat-prelude */
try { window = GameGlobal; } catch (e) {}
try { document = GameGlobal.document; } catch (e) {}
try { localStorage = GameGlobal.localStorage; } catch (e) {}
try { sessionStorage = GameGlobal.sessionStorage; } catch (e) {}
try { indexedDB = GameGlobal.indexedDB; } catch (e) {}
try { navigator = GameGlobal.navigator; } catch (e) {}
try { XMLHttpRequest = GameGlobal.XMLHttpRequest; } catch (e) {}
try { Image = GameGlobal.Image; } catch (e) {}
try { Audio = GameGlobal.Audio; } catch (e) {}
try { AudioContext = GameGlobal.AudioContext; } catch (e) {}
try { webkitAudioContext = GameGlobal.webkitAudioContext; } catch (e) {}
try { devicePixelRatio = GameGlobal.devicePixelRatio; } catch (e) {}
try { requestAnimationFrame = GameGlobal.requestAnimationFrame; } catch (e) {}
try { cancelAnimationFrame = GameGlobal.cancelAnimationFrame; } catch (e) {}
try { performance = GameGlobal.performance; } catch (e) {}
try { WebAssembly = GameGlobal.WebAssembly; } catch (e) {}
try { atob = GameGlobal.atob; } catch (e) {}
try { btoa = GameGlobal.btoa; } catch (e) {}
try { if (window && GameGlobal.document) window.document = GameGlobal.document; } catch (e) {}
try { if (window && GameGlobal.localStorage) window.localStorage = GameGlobal.localStorage; } catch (e) {}
try { if (window && GameGlobal.indexedDB) window.indexedDB = GameGlobal.indexedDB; } catch (e) {}
try { if (window) window.WebAssembly = GameGlobal.WebAssembly; } catch (e) {}
(function () {
	var src = GameGlobal && GameGlobal.document;
	if (!src || typeof document === "undefined" || !document || document === src) return;
	if (typeof document.getElementById === "function") return;
	var names = ["getElementById", "createElement", "createElementNS", "querySelector", "querySelectorAll", "getElementsByTagName", "addEventListener", "removeEventListener", "dispatchEvent", "createTextNode", "elementFromPoint"];
	for (var i = 0; i < names.length; i++) {
		if (typeof src[names[i]] === "function") {
			try { document[names[i]] = src[names[i]].bind(src); } catch (e) {}
		}
	}
	try { document.body = src.body; } catch (e) {}
	try { document.head = src.head; } catch (e) {}
	try { document.documentElement = src.documentElement; } catch (e) {}
	try { document.visibilityState = "visible"; } catch (e) {}
	try { document.hidden = false; } catch (e) {}
	try { document.readyState = "complete"; } catch (e) {}
})();
try {
	if (typeof process !== "undefined" && process && process.versions && typeof process.versions.node === "string") {
		var __wechatVersions = {};
		for (var __wechatKey in process.versions) __wechatVersions[__wechatKey] = process.versions[__wechatKey];
		__wechatVersions.node = void 0;
		try {
			Object.defineProperty(process, "versions", { configurable: true, writable: true, value: __wechatVersions });
		} catch (e) {
			try { process.versions.node = void 0; } catch (e2) {}
		}
	}
} catch (e) {}
