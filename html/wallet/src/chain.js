export const APP_ID = '4b472823-9404-4550-a58e-a8f985bae418';
export const PACK_MEMO = 'cindervault:weapon';
export const PACK_LAMPORTS = 50_000_000;
export const FEE_BUFFER = 5_000;
export const MEMO_PROGRAM_ID = 'MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr';

const MAINNET_RPCS = [
	'https://api.mainnet-beta.solana.com',
	'https://solana-rpc.publicnode.com'
];
const DEVNET_RPC = 'https://api.devnet.solana.com';

export function rpcUrls(configured) {
	const urls = [];
	const primary = (configured || '').trim();
	if (primary) urls.push(primary);
	if (/devnet/i.test(primary)) urls.push(DEVNET_RPC);
	else urls.push(...MAINNET_RPCS);
	return [...new Set(urls)];
}

export function packMemo(raw) {
	if (raw == null || raw === '') return null;
	return String(raw).includes(PACK_MEMO) ? PACK_MEMO : null;
}

export function containsPackPayment(instructions, owner, recipient) {
	if (!instructions) return false;
	for (const instruction of instructions) {
		const parsed = instruction && instruction.parsed;
		if (!parsed || parsed.type !== 'transfer') continue;
		const info = parsed.info || {};
		const lamports = Number(info.lamports);
		if (info.source === owner
				&& info.destination === recipient
				&& Number.isFinite(lamports)
				&& lamports >= PACK_LAMPORTS) {
			return true;
		}
	}
	return false;
}

export async function ownedPackMemos(owner, recipient, rpc) {
	const found = [];
	let before = null;
	for (let page = 0; page < 5; page++) {
		const config = { limit: 1000, commitment: 'confirmed' };
		if (before) config.before = before;
		const rows = await rpc('getSignaturesForAddress', [owner, config]);
		const list = Array.isArray(rows) ? rows : [];
		if (list.length === 0) return found;
		for (const row of list) {
			if (!row || row.err) continue;
			const memo = packMemo(row.memo);
			if (!memo || !row.signature) continue;
			if (await paidPack(rpc, row.signature, owner, recipient) && !found.includes(memo)) {
				found.push(memo);
			}
			if (found.includes(PACK_MEMO)) return found;
		}
		if (list.length < 1000) return found;
		before = list[list.length - 1] && list[list.length - 1].signature;
		if (!before) return found;
	}
	return found;
}

async function paidPack(rpc, signature, owner, recipient) {
	const tx = await rpc('getTransaction', [signature, {
		encoding: 'jsonParsed',
		commitment: 'confirmed',
		maxSupportedTransactionVersion: 0
	}]);
	if (!tx) return false;
	if (tx.meta && tx.meta.err) return false;
	const message = tx.transaction && tx.transaction.message;
	if (containsPackPayment(message && message.instructions, owner, recipient)) return true;
	const inner = (tx.meta && tx.meta.innerInstructions) || [];
	for (const group of inner) {
		if (containsPackPayment(group && group.instructions, owner, recipient)) return true;
	}
	return false;
}

export async function rpcCall(url, method, params) {
	const response = await fetch(url, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			Accept: 'application/json'
		},
		body: JSON.stringify({ jsonrpc: '2.0', id: 1, method, params })
	});
	if (!response.ok) throw new Error('RPC HTTP ' + response.status);
	const body = await response.json();
	if (body.error) {
		const message = body.error.message || 'RPC failed';
		throw new Error(message);
	}
	return body.result;
}

export async function readPackMemos(owner, recipient, configuredRpc) {
	let last = null;
	for (const url of rpcUrls(configuredRpc)) {
		try {
			return await ownedPackMemos(owner, recipient, (method, params) => rpcCall(url, method, params));
		} catch (error) {
			last = error;
		}
	}
	throw last || new Error('RPC failed');
}
