require('./text-codec.js');
const assetBase = require('../asset-base.js');
const ORIGIN = 'http://cinder.local/game/';

const systemInfo = wx.getSystemInfoSync();
const pixelRatio = systemInfo.pixelRatio || 1;
const windowWidth = systemInfo.windowWidth || systemInfo.screenWidth || 480;
const windowHeight = systemInfo.windowHeight || systemInfo.screenHeight || 800;
const platform = String(systemInfo.platform || systemInfo.system || '').toLowerCase();
const mobileUa = platform.indexOf('ios') >= 0 || platform.indexOf('iphone') >= 0
	? 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger'
	: 'Mozilla/5.0 (Linux; Android 12; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36 MicroMessenger';

function cssStyle() {
	const style = {};
	style.setProperty = function (name, value) {
		style[name] = value == null ? '' : String(value);
	};
	style.getPropertyValue = function (name) {
		return style[name] || '';
	};
	return style;
}

function invoke(listener, event) {
	if (typeof listener === 'function') listener.call(event && event.target, event);
	else if (listener && typeof listener.handleEvent === 'function') listener.handleEvent(event);
}

function installEventTarget(target) {
	const listeners = {};
	target.addEventListener = function (type, fn) {
		if (!fn) return;
		(listeners[type] || (listeners[type] = [])).push(fn);
	};
	target.removeEventListener = function (type, fn) {
		const list = listeners[type];
		if (!list) return;
		const index = list.indexOf(fn);
		if (index >= 0) list.splice(index, 1);
	};
	target.dispatchEvent = function (event) {
		if (!event.target) event.target = target;
		event.currentTarget = target;
		const prop = target['on' + event.type];
		if (typeof prop === 'function') {
			try {
				prop.call(target, event);
			} catch (err) {
				console.error(err);
			}
		}
		const list = (listeners[event.type] || []).slice();
		for (let i = 0; i < list.length; i++) {
			try {
				invoke(list[i], event);
			} catch (err) {
				console.error(err);
			}
		}
		return true;
	};
	return target;
}

function resolveUrl(url) {
	if (!url) return '';
	const value = String(url);
	if (value.indexOf('data:') === 0 || value.indexOf('wxfile:') === 0 || value.indexOf('http://') === 0 || value.indexOf('https://') === 0) {
		return value;
	}
	if (value.indexOf('//') === 0) return 'http:' + value;
	if (value.charAt(0) === '/') return 'http://cinder.local' + value;
	return ORIGIN + value;
}

function toNetworkUrl(url) {
	const absolute = resolveUrl(url);
	const assetPrefix = ORIGIN + 'assets/';
	const scriptPrefix = ORIGIN + 'scripts/';
	if (absolute.indexOf(assetPrefix) === 0) {
		return assetBase + 'assets/' + absolute.substring(assetPrefix.length);
	}
	if (absolute.indexOf(scriptPrefix) === 0) {
		return { script: absolute.substring(scriptPrefix.length).split('?')[0] };
	}
	return absolute;
}

const screen = wx.createCanvas();
installEventTarget(screen);
if (!screen.style) screen.style = cssStyle();
if (typeof screen.style.setProperty !== 'function') screen.style.setProperty = function (name, value) {
	screen.style[name] = value == null ? '' : String(value);
};
screen.id = 'canvas';
screen.tagName = 'CANVAS';
try {
	if (!screen.clientWidth) {
		Object.defineProperty(screen, 'clientWidth', { configurable: true, get: function () { return windowWidth; } });
		Object.defineProperty(screen, 'clientHeight', { configurable: true, get: function () { return windowHeight; } });
	}
} catch (err) {
}
screen.getBoundingClientRect = function () {
	return {
		left: 0,
		top: 0,
		right: windowWidth,
		bottom: windowHeight,
		width: windowWidth,
		height: windowHeight,
		x: 0,
		y: 0
	};
};
if (typeof screen.getContext === 'function') {
	const rawGetContext = screen.getContext.bind(screen);
	screen.getContext = function (type, attrs) {
		let context = rawGetContext(type, attrs);
		if (!context && attrs) context = rawGetContext(type);
		return context;
	};
}

function createOffscreenCanvas() {
	const canvas = wx.createCanvas();
	installEventTarget(canvas);
	if (!canvas.style) canvas.style = cssStyle();
	canvas.tagName = 'CANVAS';
	canvas.getBoundingClientRect = screen.getBoundingClientRect;
	return canvas;
}

function hookImage(img) {
	const listeners = { load: [], error: [] };
	let userOnload = null;
	let userOnerror = null;
	img.addEventListener = function (type, fn) {
		if (listeners[type]) listeners[type].push(fn);
	};
	img.removeEventListener = function (type, fn) {
		const list = listeners[type];
		if (!list) return;
		const index = list.indexOf(fn);
		if (index >= 0) list.splice(index, 1);
	};
	function emit(type, nativeEvent) {
		const event = nativeEvent || { type: type, target: img };
		event.type = type;
		event.target = img;
		const list = listeners[type].slice();
		for (let i = 0; i < list.length; i++) invoke(list[i], event);
		const user = type === 'load' ? userOnload : userOnerror;
		if (typeof user === 'function') user.call(img, event);
	}
	function loadDispatcher(event) { emit('load', event); }
	function errorDispatcher(event) { emit('error', event); }
	try {
		Object.defineProperty(img, 'onload', {
			configurable: true,
			get: function () { return loadDispatcher; },
			set: function (fn) { if (fn !== loadDispatcher) userOnload = fn; }
		});
		Object.defineProperty(img, 'onerror', {
			configurable: true,
			get: function () { return errorDispatcher; },
			set: function (fn) { if (fn !== errorDispatcher) userOnerror = fn; }
		});
	} catch (err) {
		img.onload = loadDispatcher;
		img.onerror = errorDispatcher;
	}
	const srcDescriptor = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(img), 'src')
		|| Object.getOwnPropertyDescriptor(img, 'src');
	let srcValue = '';
	try {
		Object.defineProperty(img, 'src', {
			configurable: true,
			get: function () { return srcValue; },
			set: function (value) {
				srcValue = resolveUrl(value);
				const mapped = toNetworkUrl(srcValue);
				const native = typeof mapped === 'string' ? mapped : srcValue;
				if (srcDescriptor && srcDescriptor.set) srcDescriptor.set.call(img, native);
				else img.setAttribute && img.setAttribute('src', native);
			}
		});
	} catch (err) {
	}
	return img;
}

function createImage() {
	return hookImage(wx.createImage());
}

function createScript() {
	const script = installEventTarget({
		tagName: 'SCRIPT',
		async: true,
		_src: '',
		_fired: false
	});
	Object.defineProperty(script, 'src', {
		configurable: true,
		get: function () { return script._src; },
		set: function (value) {
			script._src = resolveUrl(value);
			scheduleScript(script);
		}
	});
	script.setAttribute = function (name, value) {
		if (name === 'src') script.src = value;
		else script[name] = value;
	};
	return script;
}

function scheduleScript(script) {
	if (script._fired || !script._src) return;
	script._fired = true;
	setTimeout(function () {
		const mapped = toNetworkUrl(script._src);
		if (mapped && mapped.script) {
			script.dispatchEvent({ type: 'load', target: script });
			return;
		}
		console.error('[wechat] refused remote script', script._src);
		script.dispatchEvent({ type: 'error', target: script });
	}, 0);
}

function createAudio(src) {
	const audio = installEventTarget({
		tagName: 'AUDIO',
		preload: 'auto',
		readyState: 0,
		paused: true,
		currentTime: 0,
		volume: 1,
		loop: false,
		src: src || ''
	});
	audio.canPlayType = function () { return 'probably'; };
	audio.play = function () {
		audio.paused = false;
		return Promise.resolve();
	};
	audio.pause = function () { audio.paused = true; };
	audio.load = function () {};
	audio.setAttribute = function (name, value) { audio[name] = value; };
	return audio;
}
createAudio.canPlayType = function () { return 'probably'; };

function createStub(tag) {
	const node = installEventTarget({
		tagName: String(tag || 'DIV').toUpperCase(),
		style: cssStyle(),
		children: [],
		childNodes: []
	});
	node.appendChild = function (child) {
		node.children.push(child);
		node.childNodes.push(child);
		if (child && child.tagName === 'SCRIPT') scheduleScript(child);
		return child;
	};
	node.removeChild = function (child) { return child; };
	node.setAttribute = function (name, value) { node[name] = value; };
	node.getAttribute = function (name) { return node[name]; };
	node.insertBefore = function (child) { return node.appendChild(child); };
	node.focus = function () {};
	node.blur = function () {};
	return node;
}

const documentElement = createStub('html');
const body = createStub('body');
documentElement.clientWidth = windowWidth;
documentElement.clientHeight = windowHeight;
body.clientWidth = windowWidth;
body.clientHeight = windowHeight;

const documentTarget = installEventTarget({
	documentElement: documentElement,
	body: body,
	head: createStub('head'),
	readyState: 'complete',
	visibilityState: 'visible',
	hidden: false,
	title: 'Cinder Vault',
	cookie: '',
	location: null,
	currentScript: { src: ORIGIN + 'scripts/gdx.wasm.js' }
});
documentTarget.nodeType = 9;
documentTarget.documentElement.appendChild(body);
documentTarget.getElementById = function (id) {
	if (id === 'canvas') return screen;
	return null;
};
documentTarget.querySelector = function (selector) {
	if (selector === '#canvas' || selector === 'canvas') return screen;
	return null;
};
documentTarget.querySelectorAll = function () { return []; };
documentTarget.getElementsByTagName = function (tag) {
	if (String(tag).toLowerCase() === 'canvas') return [screen];
	if (String(tag).toLowerCase() === 'body') return [body];
	return [];
};
documentTarget.createElement = function (tag) {
	const name = String(tag || '').toLowerCase();
	if (name === 'canvas') return createOffscreenCanvas();
	if (name === 'img' || name === 'image') return createImage();
	if (name === 'script') return createScript();
	if (name === 'audio') return createAudio();
	return createStub(name);
};
documentTarget.createElementNS = function (ns, tag) { return documentTarget.createElement(tag); };
documentTarget.createTextNode = function (text) { return { textContent: text }; };
documentTarget.elementFromPoint = function () { return screen; };

const location = {
	href: ORIGIN + 'index.html',
	protocol: 'http:',
	host: 'cinder.local',
	hostname: 'cinder.local',
	port: '',
	pathname: '/game/index.html',
	search: '',
	hash: '',
	origin: 'http://cinder.local',
	assign: function () {},
	replace: function () {},
	reload: function () {},
	toString: function () { return ORIGIN + 'index.html'; }
};
documentTarget.location = location;

const storageMemory = {};
const storage = {
	getItem: function (key) {
		const name = String(key);
		if (Object.prototype.hasOwnProperty.call(storageMemory, name)) return storageMemory[name];
		try {
			const value = wx.getStorageSync(name);
			if (value === '' || value === undefined || value === null) return null;
			return String(value);
		} catch (err) {
			return null;
		}
	},
	setItem: function (key, value) {
		const name = String(key);
		const text = String(value);
		storageMemory[name] = text;
		try {
			wx.setStorageSync(name, text);
		} catch (err) {
			console.error('[wechat] storage write failed', name, err);
		}
	},
	removeItem: function (key) {
		const name = String(key);
		delete storageMemory[name];
		try {
			wx.removeStorageSync(name);
		} catch (err) {
		}
	},
	clear: function () {
		Object.keys(storageMemory).forEach(function (key) { delete storageMemory[key]; });
		try {
			wx.clearStorageSync();
		} catch (err) {
		}
	},
	key: function (index) {
		try {
			const info = wx.getStorageInfoSync();
			return info.keys[index] || null;
		} catch (err) {
			return Object.keys(storageMemory)[index] || null;
		}
	},
	get length() {
		try {
			return wx.getStorageInfoSync().keys.length;
		} catch (err) {
			return Object.keys(storageMemory).length;
		}
	}
};

function installIndexedDB() {
	const fs = wx.getFileSystemManager();
	const root = wx.env.USER_DATA_PATH + '/idb';
	const databases = {};

	function safe(name) {
		return String(name).replace(/[^a-zA-Z0-9._-]/g, '_');
	}
	function ensureDir(path) {
		try {
			fs.mkdirSync(path, true);
		} catch (err) {
		}
	}
	function bytesOf(contents) {
		if (!contents) return null;
		if (contents instanceof ArrayBuffer) return new Uint8Array(contents);
		if (ArrayBuffer.isView(contents)) {
			return new Uint8Array(contents.buffer, contents.byteOffset, contents.byteLength);
		}
		return null;
	}
	function persist(db, store, key, value) {
		const dir = root + '/' + safe(db.name) + '/' + safe(store);
		ensureDir(dir);
		const file = dir + '/' + encodeURIComponent(String(key)) + '.json';
		const bytes = value ? bytesOf(value.contents) : null;
		const payload = {
			type: value && value.type != null ? value.type : 0,
			date: value && value.date ? Number(value.date) || 0 : 0,
			contents: bytes ? wx.arrayBufferToBase64(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)) : null
		};
		fs.writeFileSync(file, JSON.stringify(payload), 'utf8');
	}
	function loadStore(db, store) {
		const map = new Map();
		const dir = root + '/' + safe(db.name) + '/' + safe(store);
		let names = [];
		try {
			names = fs.readdirSync(dir) || [];
		} catch (err) {
			db.stores[store] = map;
			return map;
		}
		names.forEach(function (name) {
			if (name.slice(-5) !== '.json') return;
			try {
				const saved = JSON.parse(fs.readFileSync(dir + '/' + name, 'utf8'));
				const record = { type: saved.type, date: new Date(saved.date || 0), timestamp: new Date(saved.date || 0) };
				if (saved.contents) record.contents = new Int8Array(wx.base64ToArrayBuffer(saved.contents));
				map.set(decodeURIComponent(name.slice(0, -5)), record);
			} catch (err) {
				console.error('[wechat] idb read failed', name, err);
			}
		});
		db.stores[store] = map;
		return map;
	}
	function database(name) {
		if (databases[name]) return databases[name];
		const db = { name: name, version: 0, stores: {} };
		try {
			const meta = JSON.parse(fs.readFileSync(root + '/' + safe(name) + '/meta.json', 'utf8'));
			db.version = meta.version || 0;
		} catch (err) {
		}
		databases[name] = db;
		return db;
	}
	function saveMeta(db) {
		const dir = root + '/' + safe(db.name);
		ensureDir(dir);
		fs.writeFileSync(dir + '/meta.json', JSON.stringify({ version: db.version }), 'utf8');
	}
	function storeOf(db, name) {
		if (!db.stores[name]) return loadStore(db, name);
		return db.stores[name];
	}
	function request() {
		return installEventTarget({ readyState: 'pending', result: null, error: null });
	}
	function succeed(req, result, tx) {
		setTimeout(function () {
			req.readyState = 'done';
			req.result = result;
			req.dispatchEvent({ type: 'success', target: req });
			if (tx && tx._finish && result == null) tx._finish();
		}, 0);
	}

	return {
		open: function (name, version) {
			const req = request();
			const db = database(name);
			setTimeout(function () {
				const api = {
					name: name,
					version: version,
					objectStoreNames: [],
					createObjectStore: function (storeName) {
						storeOf(db, storeName);
						return {};
					},
					transaction: function (storeNames, mode) {
						const tx = installEventTarget({ mode: mode || 'readonly', error: null });
						tx._finished = false;
						tx._finish = function () {
							if (tx._finished) return;
							tx._finished = true;
							setTimeout(function () {
								tx.dispatchEvent({ type: 'complete', target: tx });
							}, 0);
						};
						tx.objectStore = function (storeName) {
							const map = storeOf(db, storeName);
							return {
								put: function (value, key) {
									const copy = {
										type: value ? value.type : 0,
										date: value ? value.date : null,
										timestamp: value ? (value.timestamp || value.date) : null
									};
									const bytes = value ? bytesOf(value.contents) : null;
									if (bytes) copy.contents = new Int8Array(bytes);
									map.set(String(key), copy);
									try {
										persist(db, storeName, key, copy);
									} catch (err) {
										console.error('[wechat] idb write failed', key, err);
									}
									const putReq = request();
									succeed(putReq, key, null);
									setTimeout(tx._finish, 0);
									return putReq;
								},
								delete: function (key) {
									map.delete(String(key));
									try {
										fs.unlinkSync(root + '/' + safe(db.name) + '/' + safe(storeName) + '/' + encodeURIComponent(String(key)) + '.json');
									} catch (err) {
									}
									const deleteReq = request();
									succeed(deleteReq, undefined, null);
									setTimeout(tx._finish, 0);
									return deleteReq;
								},
								openCursor: function () {
									const cursorReq = request();
									const keys = Array.from(map.keys());
									let index = 0;
									const step = function () {
										if (index >= keys.length) {
											cursorReq.result = null;
											cursorReq.dispatchEvent({ type: 'success', target: cursorReq });
											tx._finish();
											return;
										}
										const key = keys[index];
										cursorReq.result = {
											key: key,
											primaryKey: key,
											value: map.get(key),
											continue: function () {
												index += 1;
												setTimeout(step, 0);
											}
										};
										cursorReq.dispatchEvent({ type: 'success', target: cursorReq });
									};
									setTimeout(step, 0);
									return cursorReq;
								}
							};
						};
						return tx;
					}
				};
				req.result = api;
				if ((version || 1) > db.version) {
					req.transaction = api.transaction('FILE_DATA', 'versionchange');
					req.dispatchEvent({ type: 'upgradeneeded', target: req, oldVersion: db.version, newVersion: version });
					db.version = version || 1;
					try {
						saveMeta(db);
					} catch (err) {
						console.error(err);
					}
				}
				req.dispatchEvent({ type: 'success', target: req });
			}, 0);
			return req;
		}
	};
}

function XMLHttpRequest() {
	this.readyState = 0;
	this.status = 0;
	this.statusText = '';
	this.response = null;
	this.responseText = '';
	this.responseType = '';
	this.timeout = 0;
	this.withCredentials = false;
	this._headers = {};
	this._url = '';
	this._async = true;
	this._task = null;
	installEventTarget(this);
}
XMLHttpRequest.UNSENT = 0;
XMLHttpRequest.OPENED = 1;
XMLHttpRequest.HEADERS_RECEIVED = 2;
XMLHttpRequest.LOADING = 3;
XMLHttpRequest.DONE = 4;
XMLHttpRequest.prototype.open = function (method, url, async) {
	this._method = method || 'GET';
	this._url = resolveUrl(url);
	this._async = async !== false;
	this.readyState = 1;
	this.dispatchEvent({ type: 'readystatechange', target: this });
};
XMLHttpRequest.prototype.setRequestHeader = function (name, value) {
	this._headers[name] = value;
};
XMLHttpRequest.prototype.getResponseHeader = function () { return null; };
XMLHttpRequest.prototype.getAllResponseHeaders = function () { return ''; };
XMLHttpRequest.prototype.overrideMimeType = function () {};
XMLHttpRequest.prototype.abort = function () {
	if (this._task && this._task.abort) this._task.abort();
	this.dispatchEvent({ type: 'abort', target: this });
};
XMLHttpRequest.prototype.send = function () {
	const xhr = this;
	const mapped = toNetworkUrl(xhr._url);
	if (mapped && mapped.script) {
		xhr.readyState = 4;
		xhr.status = 200;
		xhr.response = new ArrayBuffer(0);
		setTimeout(function () {
			xhr.dispatchEvent({ type: 'readystatechange', target: xhr });
			xhr.dispatchEvent({ type: 'load', target: xhr });
		}, 0);
		return;
	}
	const url = typeof mapped === 'string' ? mapped : xhr._url;
	xhr._task = wx.request({
		url: url,
		method: xhr._method || 'GET',
		header: xhr._headers,
		responseType: xhr.responseType === 'arraybuffer' ? 'arraybuffer' : 'text',
		timeout: xhr.timeout || 60000,
		success: function (res) {
			xhr.readyState = 4;
			xhr.status = res.statusCode || 0;
			xhr.statusText = String(xhr.status);
			if (xhr.responseType === 'arraybuffer') {
				if (res.data instanceof ArrayBuffer) xhr.response = res.data;
				else if (ArrayBuffer.isView(res.data)) xhr.response = res.data.buffer.slice(res.data.byteOffset, res.data.byteOffset + res.data.byteLength);
				else xhr.response = new ArrayBuffer(0);
				xhr.responseText = '';
			} else {
				xhr.responseText = typeof res.data === 'string' ? res.data : '';
				xhr.response = xhr.responseText;
			}
			xhr.dispatchEvent({ type: 'readystatechange', target: xhr });
			if (xhr.status >= 200 && xhr.status < 300) xhr.dispatchEvent({ type: 'load', target: xhr });
			else xhr.dispatchEvent({ type: 'error', target: xhr });
			xhr.dispatchEvent({ type: 'loadend', target: xhr });
		},
		fail: function (err) {
			console.error('[wechat] asset request failed', url, err && err.errMsg);
			xhr.readyState = 4;
			xhr.status = 0;
			xhr.response = xhr.responseType === 'arraybuffer' ? new ArrayBuffer(0) : '';
			xhr.dispatchEvent({ type: 'readystatechange', target: xhr });
			xhr.dispatchEvent({ type: 'error', target: xhr });
			xhr.dispatchEvent({ type: 'loadend', target: xhr });
		}
	});
};

function installWebAssembly() {
	const original = typeof WebAssembly === 'object' && WebAssembly ? WebAssembly : null;
	const wxWasm = typeof WXWebAssembly !== 'undefined' ? WXWebAssembly : null;
	function toArrayBuffer(buffer) {
		if (buffer instanceof ArrayBuffer) return buffer;
		if (ArrayBuffer.isView(buffer)) return buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength);
		if (buffer && buffer.data) return toArrayBuffer(buffer.data);
		if (buffer && typeof buffer.length === 'number') {
			const bytes = new Uint8Array(buffer.length);
			for (let i = 0; i < buffer.length; i++) bytes[i] = buffer[i] & 255;
			return bytes.buffer;
		}
		return new ArrayBuffer(0);
	}
	function normalize(result) {
		if (result && result.instance) return result;
		return { instance: result, module: null };
	}
	function instantiate(buffer, imports) {
		if (wxWasm && typeof wxWasm.instantiate === 'function') {
			return wxWasm.instantiate('engine/scripts/gdx.wasm', imports).then(normalize).catch(function (err) {
				console.error('[wechat] package wasm failed, writing a local copy', err);
				const path = wx.env.USER_DATA_PATH + '/teavm-gdx.wasm';
				wx.getFileSystemManager().writeFileSync(path, toArrayBuffer(buffer));
				return wxWasm.instantiate(path, imports).then(normalize);
			});
		}
		if (original && typeof original.instantiate === 'function') return original.instantiate(toArrayBuffer(buffer), imports);
		return Promise.reject(new Error('WebAssembly unavailable'));
	}
	GameGlobal.WebAssembly = {
		instantiate: instantiate,
		compile: function (buffer) {
			return instantiate(buffer, {}).then(function (result) { return result.module; });
		},
		RuntimeError: (original && original.RuntimeError) || Error,
		Module: original && original.Module,
		Instance: original && original.Instance,
		Memory: original && original.Memory,
		Table: original && original.Table,
		validate: original && original.validate
	};
	try {
		WebAssembly = GameGlobal.WebAssembly;
	} catch (err) {
	}
}

function WeChatAudioContext() {
	if (typeof wx.createWebAudioContext !== 'function') throw new Error('WebAudio unavailable');
	const ctx = wx.createWebAudioContext();
	if (ctx && typeof ctx.decodeAudioData === 'function') {
		const decode = ctx.decodeAudioData.bind(ctx);
		ctx.decodeAudioData = function (data, success, error) {
			if (typeof success === 'function') return decode(data, success, error || function () {});
			return new Promise(function (resolve, reject) {
				let settled = false;
				const result = decode(data, function (buffer) {
					settled = true;
					resolve(buffer);
				}, function (err) {
					settled = true;
					reject(err);
				});
				if (result && typeof result.then === 'function' && !settled) result.then(resolve, reject);
			});
		};
	}
	return ctx;
}

if (typeof atob !== 'function') {
	GameGlobal.atob = function (input) {
		const text = wx.base64ToArrayBuffer(input);
		const bytes = new Uint8Array(text);
		let out = '';
		for (let i = 0; i < bytes.length; i++) out += String.fromCharCode(bytes[i]);
		return out;
	};
}
if (typeof btoa !== 'function') {
	GameGlobal.btoa = function (input) {
		const bytes = new Uint8Array(input.length);
		for (let i = 0; i < input.length; i++) bytes[i] = input.charCodeAt(i) & 0xff;
		return wx.arrayBufferToBase64(bytes.buffer);
	};
}

installWebAssembly();

const navigatorObject = {
	userAgent: mobileUa,
	appVersion: mobileUa,
	platform: platform.indexOf('ios') >= 0 ? 'iPhone' : 'Linux armv8l',
	language: systemInfo.language || 'zh-CN',
	languages: [systemInfo.language || 'zh-CN'],
	onLine: true,
	vendor: '',
	maxTouchPoints: 5,
	product: 'Gecko'
};

const windowTarget = installEventTarget(GameGlobal);
windowTarget.window = windowTarget;
windowTarget.self = windowTarget;
windowTarget.global = windowTarget;
windowTarget.top = windowTarget;
windowTarget.parent = windowTarget;
windowTarget.globalThis = windowTarget;
windowTarget.document = documentTarget;
windowTarget.location = location;
windowTarget.navigator = navigatorObject;
windowTarget.localStorage = storage;
windowTarget.sessionStorage = storage;
windowTarget.indexedDB = installIndexedDB();
windowTarget.canvas = screen;
windowTarget.Image = createImage;
windowTarget.Audio = createAudio;
windowTarget.XMLHttpRequest = XMLHttpRequest;
windowTarget.AudioContext = WeChatAudioContext;
windowTarget.webkitAudioContext = WeChatAudioContext;
windowTarget.devicePixelRatio = pixelRatio;
windowTarget.innerWidth = windowWidth;
windowTarget.innerHeight = windowHeight;
windowTarget.outerWidth = windowWidth;
windowTarget.outerHeight = windowHeight;
windowTarget.screen = {
	width: systemInfo.screenWidth || windowWidth,
	height: systemInfo.screenHeight || windowHeight,
	availWidth: windowWidth,
	availHeight: windowHeight
};
windowTarget.performance = GameGlobal.performance || { now: function () { return Date.now(); } };
windowTarget.console = console;
windowTarget.setTimeout = setTimeout;
windowTarget.clearTimeout = clearTimeout;
windowTarget.setInterval = setInterval;
windowTarget.clearInterval = clearInterval;
windowTarget.requestAnimationFrame = function (callback) {
	if (screen.requestAnimationFrame) return screen.requestAnimationFrame(callback);
	return setTimeout(function () { callback(Date.now()); }, 16);
};
windowTarget.cancelAnimationFrame = function (id) {
	if (screen.cancelAnimationFrame) return screen.cancelAnimationFrame(id);
	clearTimeout(id);
};
windowTarget.getComputedStyle = function () {
	return { getPropertyValue: function () { return ''; } };
};
windowTarget.scrollTo = function () {};
windowTarget.focus = function () {};
windowTarget.addEventListener = windowTarget.addEventListener;
windowTarget.WebAssembly = GameGlobal.WebAssembly;

function pointerFrom(touch) {
	const x = touch.clientX != null ? touch.clientX : (touch.x || 0);
	const y = touch.clientY != null ? touch.clientY : (touch.y || 0);
	return {
		identifier: touch.identifier || 0,
		clientX: x,
		clientY: y,
		pageX: touch.pageX != null ? touch.pageX : x,
		pageY: touch.pageY != null ? touch.pageY : y,
		screenX: x,
		screenY: y,
		offsetX: x,
		offsetY: y,
		target: screen
	};
}

function emitPointer(type, res) {
	const touches = (res.touches || []).map(pointerFrom);
	const changed = (res.changedTouches || res.touches || []).map(pointerFrom);
	const primary = changed[0] || touches[0] || pointerFrom({});
	const event = {
		type: type,
		target: screen,
		currentTarget: screen,
		clientX: primary.clientX,
		clientY: primary.clientY,
		pageX: primary.pageX,
		pageY: primary.pageY,
		offsetX: primary.offsetX,
		offsetY: primary.offsetY,
		screenX: primary.screenX,
		screenY: primary.screenY,
		button: 0,
		buttons: type === 'touchend' || type === 'mouseup' || type === 'pointerup' ? 0 : 1,
		pointerId: primary.identifier,
		pointerType: 'touch',
		which: 1,
		touches: touches,
		targetTouches: touches,
		changedTouches: changed,
		preventDefault: function () {},
		stopPropagation: function () {}
	};
	screen.dispatchEvent(event);
	documentTarget.dispatchEvent(event);
	windowTarget.dispatchEvent(event);
}

wx.onTouchStart(function (res) {
	emitPointer('touchstart', res);
	emitPointer('pointerdown', res);
	emitPointer('mousedown', res);
});
wx.onTouchMove(function (res) {
	emitPointer('touchmove', res);
	emitPointer('pointermove', res);
	emitPointer('mousemove', res);
});
wx.onTouchEnd(function (res) {
	emitPointer('touchend', res);
	emitPointer('pointerup', res);
	emitPointer('mouseup', res);
});
wx.onTouchCancel(function (res) {
	emitPointer('touchcancel', res);
	emitPointer('pointerup', res);
	emitPointer('mouseup', res);
});

if (typeof wx.onWindowResize === 'function') {
	wx.onWindowResize(function () {
		windowTarget.dispatchEvent({ type: 'resize', target: windowTarget });
	});
}

function copyDocumentApi(doc) {
	if (!doc || doc === documentTarget) return;
	const names = ['getElementById', 'createElement', 'createElementNS', 'querySelector', 'querySelectorAll', 'getElementsByTagName', 'addEventListener', 'removeEventListener', 'dispatchEvent', 'createTextNode', 'elementFromPoint'];
	for (let i = 0; i < names.length; i++) {
		if (typeof documentTarget[names[i]] === 'function' && typeof doc[names[i]] !== 'function') {
			try {
				doc[names[i]] = documentTarget[names[i]].bind(documentTarget);
			} catch (err) {
			}
		}
	}
	try { doc.body = documentTarget.body; } catch (err) {}
	try { doc.head = documentTarget.head; } catch (err) {}
	try { doc.documentElement = documentTarget.documentElement; } catch (err) {}
	try { doc.visibilityState = 'visible'; } catch (err) {}
	try { doc.hidden = false; } catch (err) {}
	try { doc.readyState = 'complete'; } catch (err) {}
	try { doc.location = location; } catch (err) {}
}

function adopt(win) {
	if (!win) return;
	try { win.document = documentTarget; } catch (err) {}
	try { copyDocumentApi(win.document); } catch (err) {}
	try { win.innerWidth = windowWidth; } catch (err) {}
	try { win.innerHeight = windowHeight; } catch (err) {}
	try { win.devicePixelRatio = pixelRatio; } catch (err) {}
	try { win.localStorage = storage; } catch (err) {}
	try { win.sessionStorage = storage; } catch (err) {}
	try { win.indexedDB = windowTarget.indexedDB; } catch (err) {}
	try { win.navigator = navigatorObject; } catch (err) {}
	try { win.requestAnimationFrame = windowTarget.requestAnimationFrame; } catch (err) {}
	try { win.cancelAnimationFrame = windowTarget.cancelAnimationFrame; } catch (err) {}
	try { win.XMLHttpRequest = XMLHttpRequest; } catch (err) {}
	try { win.AudioContext = WeChatAudioContext; } catch (err) {}
	try { win.webkitAudioContext = WeChatAudioContext; } catch (err) {}
	try { win.WebAssembly = GameGlobal.WebAssembly; } catch (err) {}
	try { if (typeof win.atob !== 'function') win.atob = GameGlobal.atob; } catch (err) {}
	try { if (typeof win.btoa !== 'function') win.btoa = GameGlobal.btoa; } catch (err) {}
	try { if (typeof win.TextDecoder !== 'function') win.TextDecoder = GameGlobal.TextDecoder; } catch (err) {}
	try { if (typeof win.TextEncoder !== 'function') win.TextEncoder = GameGlobal.TextEncoder; } catch (err) {}
	try { if (typeof win.Blob !== 'function') win.Blob = GameGlobal.Blob; } catch (err) {}
	try { if (!win.URL || typeof win.URL.createObjectURL !== 'function') win.URL = GameGlobal.URL; } catch (err) {}
}

adopt(GameGlobal);
try { adopt(window); } catch (err) {}
try { if (GameGlobal.window && GameGlobal.window !== GameGlobal) adopt(GameGlobal.window); } catch (err) {}

const cinderWindow = new Proxy({}, {
	get: function (target, prop, receiver) {
		if (prop === 'document') return documentTarget;
		if (prop === 'window' || prop === 'self' || prop === 'top' || prop === 'parent' || prop === 'globalThis') return receiver;
		if (prop === 'localStorage' || prop === 'sessionStorage') return storage;
		if (prop === 'indexedDB') return GameGlobal.indexedDB;
		if (prop === 'navigator') return navigatorObject;
		if (prop === 'XMLHttpRequest') return XMLHttpRequest;
		if (prop === 'Image') return createImage;
		if (prop === 'Audio') return createAudio;
		if (prop === 'AudioContext' || prop === 'webkitAudioContext') return WeChatAudioContext;
		if (prop === 'WebAssembly') return GameGlobal.WebAssembly;
		if (prop === 'innerWidth' || prop === 'outerWidth') return windowWidth;
		if (prop === 'innerHeight' || prop === 'outerHeight') return windowHeight;
		if (prop === 'devicePixelRatio') return pixelRatio;
		if (prop === 'location') return location;
		if (prop === 'performance') return GameGlobal.performance;
		if (prop === 'atob') return GameGlobal.atob;
		if (prop === 'btoa') return GameGlobal.btoa;
		if (prop === 'TextDecoder') return GameGlobal.TextDecoder;
		if (prop === 'TextEncoder') return GameGlobal.TextEncoder;
		if (prop === 'Blob') return GameGlobal.Blob;
		if (prop === 'URL') return GameGlobal.URL;
		if (prop === 'console') return console;
		if (prop === 'setTimeout') return setTimeout;
		if (prop === 'clearTimeout') return clearTimeout;
		if (prop === 'setInterval') return setInterval;
		if (prop === 'clearInterval') return clearInterval;
		if (prop === 'requestAnimationFrame') return GameGlobal.requestAnimationFrame;
		if (prop === 'cancelAnimationFrame') return GameGlobal.cancelAnimationFrame;
		if (prop === 'addEventListener') return function (type, fn) { return windowTarget.addEventListener(type, fn); };
		if (prop === 'removeEventListener') return function (type, fn) { return windowTarget.removeEventListener(type, fn); };
		if (prop === 'dispatchEvent') return function (event) { return windowTarget.dispatchEvent(event); };
		if (Object.prototype.hasOwnProperty.call(target, prop)) return target[prop];
		const host = GameGlobal.window && GameGlobal.window !== receiver ? GameGlobal.window : GameGlobal;
		const value = host[prop];
		return typeof value === 'function' ? value.bind(host) : value;
	},
	set: function (target, prop, value) {
		target[prop] = value;
		return true;
	},
	has: function () { return true; }
});
GameGlobal.__cinderWindow = cinderWindow;
GameGlobal.__cinderDocument = documentTarget;

console.log('[wechat] adapter ready, assets from ' + assetBase);
