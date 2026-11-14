function bytesOf(input) {
	if (!input) return new Uint8Array(0);
	if (input instanceof Uint8Array) return input;
	if (input instanceof ArrayBuffer) return new Uint8Array(input);
	if (input.buffer instanceof ArrayBuffer) {
		return new Uint8Array(input.buffer, input.byteOffset || 0, input.byteLength || 0);
	}
	return new Uint8Array(0);
}

function decodeUtf8(bytes) {
	let out = '';
	let i = 0;
	while (i < bytes.length) {
		const c = bytes[i++];
		let cp;
		if (c < 0x80) cp = c;
		else if ((c & 0xe0) === 0xc0 && i < bytes.length) cp = ((c & 0x1f) << 6) | (bytes[i++] & 0x3f);
		else if ((c & 0xf0) === 0xe0 && i + 1 < bytes.length) {
			cp = ((c & 0x0f) << 12) | ((bytes[i++] & 0x3f) << 6) | (bytes[i++] & 0x3f);
		} else if ((c & 0xf8) === 0xf0 && i + 2 < bytes.length) {
			cp = ((c & 0x07) << 18) | ((bytes[i++] & 0x3f) << 12) | ((bytes[i++] & 0x3f) << 6) | (bytes[i++] & 0x3f);
		} else cp = 0xfffd;
		if (cp > 0xffff) {
			cp -= 0x10000;
			out += String.fromCharCode(0xd800 + (cp >> 10), 0xdc00 + (cp & 0x3ff));
		} else {
			out += String.fromCharCode(cp);
		}
	}
	return out;
}

function decodeUtf16LE(bytes) {
	const codes = new Array(bytes.length >> 1);
	for (let i = 0; i + 1 < bytes.length; i += 2) codes[i >> 1] = bytes[i] | (bytes[i + 1] << 8);
	let out = '';
	for (let j = 0; j < codes.length; j += 8192) out += String.fromCharCode.apply(null, codes.slice(j, j + 8192));
	return out;
}

function WeChatTextDecoder(encoding) {
	this.encoding = String(encoding || 'utf-8').toLowerCase();
}

WeChatTextDecoder.prototype.decode = function (input) {
	const bytes = bytesOf(input);
	const encoding = this.encoding.replace(/[^a-z0-9]/g, '');
	if (encoding === 'utf16le' || encoding === 'utf16') return decodeUtf16LE(bytes);
	return decodeUtf8(bytes);
};

function WeChatTextEncoder() {}

WeChatTextEncoder.prototype.encode = function (input) {
	const str = input == null ? '' : String(input);
	const arr = [];
	for (let i = 0; i < str.length; i++) {
		let c = str.charCodeAt(i);
		if (c >= 0xd800 && c <= 0xdbff && i + 1 < str.length) {
			const next = str.charCodeAt(++i);
			c = 0x10000 + ((c - 0xd800) << 10) + (next - 0xdc00);
		}
		if (c < 0x80) arr.push(c);
		else if (c < 0x800) arr.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f));
		else if (c < 0x10000) arr.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
		else arr.push(0xf0 | (c >> 18), 0x80 | ((c >> 12) & 0x3f), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
	}
	return new Uint8Array(arr);
};

function WeChatBlob(parts, options) {
	const chunks = [];
	let total = 0;
	const list = parts || [];
	for (let i = 0; i < list.length; i++) {
		const part = list[i];
		let bytes;
		if (part instanceof Uint8Array) bytes = part;
		else if (part instanceof ArrayBuffer) bytes = new Uint8Array(part);
		else if (part && part.buffer instanceof ArrayBuffer) bytes = new Uint8Array(part.buffer, part.byteOffset || 0, part.byteLength || 0);
		else if (Array.isArray(part)) bytes = Uint8Array.from(part);
		else {
			const str = String(part);
			bytes = new Uint8Array(str.length);
			for (let j = 0; j < str.length; j++) bytes[j] = str.charCodeAt(j) & 0xff;
		}
		chunks.push(bytes);
		total += bytes.length;
	}
	const out = new Uint8Array(total);
	let offset = 0;
	for (let k = 0; k < chunks.length; k++) {
		out.set(chunks[k], offset);
		offset += chunks[k].length;
	}
	this._bytes = out;
	this.size = total;
	this.type = (options && options.type) || '';
}

let blobSeq = 0;

function WeChatURL() {}

WeChatURL.createObjectURL = function (blob) {
	const bytes = blob && blob._bytes ? blob._bytes : bytesOf(blob);
	const path = wx.env.USER_DATA_PATH + '/blob-' + (blobSeq++) + '.bin';
	const copy = bytes.byteOffset === 0 && bytes.byteLength === bytes.buffer.byteLength ? bytes : bytes.slice();
	wx.getFileSystemManager().writeFileSync(path, copy);
	return path;
};

WeChatURL.revokeObjectURL = function () {};

const root = typeof GameGlobal !== 'undefined' ? GameGlobal : this;

if (typeof root.TextDecoder !== 'function') root.TextDecoder = WeChatTextDecoder;
if (typeof root.TextEncoder !== 'function') root.TextEncoder = WeChatTextEncoder;
if (typeof root.Blob !== 'function') root.Blob = WeChatBlob;
if (typeof root.URL === 'undefined' || typeof root.URL.createObjectURL !== 'function') root.URL = WeChatURL;

try {
	if (typeof globalThis !== 'undefined') {
		if (typeof globalThis.TextDecoder !== 'function') globalThis.TextDecoder = root.TextDecoder;
		if (typeof globalThis.TextEncoder !== 'function') globalThis.TextEncoder = root.TextEncoder;
		if (typeof globalThis.Blob !== 'function') globalThis.Blob = root.Blob;
		if (typeof globalThis.URL === 'undefined' || typeof globalThis.URL.createObjectURL !== 'function') globalThis.URL = root.URL;
	}
} catch (err) {}
