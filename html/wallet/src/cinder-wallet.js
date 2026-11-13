import { CherryMiniApp, getBridgeError, isInsideCherry } from '@cherrydotfun/miniapp-sdk';
import { WalletReadyState } from '@solana/wallet-adapter-base';
import { Connection, PublicKey } from '@solana/web3.js';
import { withTransactionLabel } from '@cherrydotfun/miniapp-sdk/solana';
import { APP_ID, readPackMemos, rpcUrls } from './chain.js';
import { WalletResult, changedWallet, formatError, isUserReject } from './errors.js';
import { identityFromCherry, signedInText } from './identity.js';
import { buildTransaction, enoughBalance, labelFor } from './transfer.js';

const cherry = isInsideCherry()
	? new CherryMiniApp({ trustedOrigins: ['https://cherry.run'] })
	: null;
let readyPromise = null;
let browserWallet = null;

function config() {
	return window.cinderWalletConfig || {};
}

function ready() {
	if (!readyPromise) {
		readyPromise = (async () => {
			if (cherry) await cherry.init();
			return true;
		})();
	}
	return readyPromise;
}

function paintSignedIn(text) {
	if (!text || typeof document === 'undefined') return;
	const parent = document.body || document.documentElement;
	if (!parent) return;
	let node = document.getElementById('cherry-signed-in');
	if (!node) {
		node = document.createElement('div');
		node.id = 'cherry-signed-in';
		node.setAttribute('role', 'status');
		node.style.position = 'fixed';
		node.style.top = '0';
		node.style.left = '0';
		node.style.right = '0';
		node.style.zIndex = '50';
		node.style.boxSizing = 'border-box';
		node.style.padding = 'max(8px, env(safe-area-inset-top)) 12px 8px';
		node.style.color = '#f4efe4';
		node.style.background = 'rgba(0, 0, 0, 0.82)';
		node.style.font = '14px/1.4 sans-serif';
		node.style.width = '100%';
		node.style.maxWidth = '100vw';
		node.style.overflowWrap = 'anywhere';
		node.style.textAlign = 'center';
		node.style.pointerEvents = 'none';
		parent.appendChild(node);
	}
	node.textContent = text;
}

function showCherryUser() {
	if (!cherry) return;
	const text = signedInText(identityFromCherry(cherry));
	if (text) paintSignedIn(text);
}

async function showCherryProfile() {
	showCherryUser();
	if (signedInText(identityFromCherry(cherry))) return;
	if (!cherry.users || typeof cherry.users.me !== 'function') return;
	try {
		const profile = await cherry.users.me();
		const current = identityFromCherry(cherry);
		const wallet = profile && profile.wallets && profile.wallets[0] && profile.wallets[0].address;
		paintSignedIn(signedInText({
			displayName: current.displayName || (profile && profile.displayName) || '',
			publicKey: current.publicKey || wallet || ''
		}));
	} catch (error) {
		showCherryUser();
	}
}

if (cherry) {
	ready().then(() => {
		showCherryProfile();
		if (typeof cherry.onContextChange === 'function') {
			cherry.onContextChange(showCherryUser);
		}
	}).catch(() => {});
}

function insideCherry() {
	return Promise.resolve(isInsideCherry());
}

function assertOnline() {
	if (typeof navigator !== 'undefined' && navigator.onLine === false) {
		throw new WalletResult('OFFLINE');
	}
}

function recipientAddress() {
	const recipient = String(config().recipient || '').trim();
	try {
		return new PublicKey(recipient).toBase58();
	} catch (error) {
		throw new WalletResult('NOT_CONFIGURED');
	}
}

function parseLamports(value) {
	const text = String(value == null ? '' : value).trim();
	if (!/^\d+$/.test(text)) throw new WalletResult('FAILED', 'bad amount');
	const amount = BigInt(text);
	if (amount <= 0n || amount > BigInt(Number.MAX_SAFE_INTEGER)) {
		throw new WalletResult('FAILED', 'bad amount');
	}
	return Number(amount);
}

function findBrowserWallet() {
	const candidates = [
		window.phantom && window.phantom.solana,
		window.solflare,
		window.backpack,
		window.okxwallet && window.okxwallet.solana,
		window.solana
	];
	for (const provider of candidates) {
		if (provider && typeof provider.connect === 'function') {
			return { provider, readyState: WalletReadyState.Installed };
		}
	}
	return { provider: null, readyState: WalletReadyState.NotDetected };
}

async function browserPublicKey(connect) {
	const found = findBrowserWallet();
	if (!found.provider || found.readyState !== WalletReadyState.Installed) {
		throw new WalletResult('NO_WALLET');
	}
	browserWallet = found.provider;
	if (!browserWallet.publicKey && connect) {
		const result = await browserWallet.connect();
		const key = browserWallet.publicKey || (result && result.publicKey);
		if (!key) throw new WalletResult('NO_WALLET');
	}
	if (!browserWallet.publicKey) throw new WalletResult('NO_WALLET');
	return browserWallet.publicKey.toString();
}

async function cherryPublicKey() {
	try {
		await ready();
	} catch (error) {
		throw new WalletResult('FAILED', error && error.message ? error.message : 'Cherry did not answer');
	}
	const key = cherry.paymentWallet || cherry.wallet.publicKey;
	if (!key) throw new WalletResult('FAILED', 'WALLET_NOT_CONNECTED');
	return key;
}

async function currentPayer() {
	assertOnline();
	if (isInsideCherry()) return cherryPublicKey();
	return browserPublicKey(true);
}

async function connect() {
	return currentPayer();
}

async function disconnect() {
	if (!isInsideCherry() && browserWallet && typeof browserWallet.disconnect === 'function') {
		try {
			await browserWallet.disconnect();
		} catch (error) {
			if (isUserReject(error)) throw new WalletResult('CANCELLED');
		}
	}
	browserWallet = null;
}

async function ownedMemos(owner) {
	const recipient = recipientAddress();
	try {
		return await readPackMemos(owner, recipient, config().rpcUrl || '');
	} catch (error) {
		throw new WalletResult('UNREAD');
	}
}

async function packRecord(owner) {
	assertOnline();
	recipientAddress();
	const memos = await ownedMemos(owner);
	return memos.join(',');
}

async function probe(url, payer) {
	const connection = new Connection(url, 'confirmed');
	const balance = await connection.getBalance(new PublicKey(payer), 'confirmed');
	const latest = await connection.getLatestBlockhash('confirmed');
	return { connection, balance, blockhash: latest.blockhash };
}

async function openConnection(payer, configuredRpc) {
	let last = null;
	for (const url of rpcUrls(configuredRpc)) {
		try {
			return await probe(url, payer);
		} catch (error) {
			last = error;
		}
	}
	throw last || new WalletResult('FAILED', 'RPC failed');
}

function signatureOf(value) {
	if (typeof value === 'string') return value;
	if (value && typeof value.signature === 'string') return value.signature;
	return '';
}

async function sendCherry(tx, label) {
	return withTransactionLabel(label, () => cherry.wallet.signAndSendTransaction(tx, { label }));
}

async function sendBrowser(connection, tx, label) {
	return withTransactionLabel(label, async () => {
		const provider = browserWallet;
		if (!provider) throw new WalletResult('NO_WALLET');
		if (typeof provider.signTransaction === 'function') {
			const signed = await provider.signTransaction(tx);
			return connection.sendRawTransaction(signed.serialize(), { preflightCommitment: 'confirmed' });
		}
		if (typeof provider.signAndSendTransaction === 'function') {
			return signatureOf(await provider.signAndSendTransaction(tx));
		}
		throw new WalletResult('FAILED', 'wallet cannot sign');
	});
}

async function waitConfirmed(connection, signature) {
	for (let attempt = 0; attempt < 12; attempt++) {
		await new Promise((resolve) => setTimeout(resolve, 2000));
		try {
			const status = await connection.getSignatureStatuses([signature], { searchTransactionHistory: true });
			const value = status && status.value && status.value[0];
			if (!value) continue;
			if (value.err) throw new WalletResult('FAILED', 'transaction failed');
			if (value.confirmationStatus === 'confirmed' || value.confirmationStatus === 'finalized') return true;
		} catch (error) {
			if (error instanceof WalletResult) throw error;
		}
	}
	return false;
}

async function payOnce(payer, lamports, memo) {
	if (memo) {
		let owned;
		try {
			owned = await readPackMemos(payer, recipientAddress(), config().rpcUrl || '');
		} catch (error) {
			throw new WalletResult('FAILED', 'could not read purchases');
		}
		if (owned.includes(memo)) throw new WalletResult('ALREADY', memo);
	}
	const opened = await openConnection(payer, config().rpcUrl || '');
	if (!enoughBalance(opened.balance, lamports)) throw new WalletResult('SHORT');
	const tx = buildTransaction({
		payer,
		recipient: recipientAddress(),
		lamports,
		memo,
		blockhash: opened.blockhash,
		insideCherry: isInsideCherry(),
		appId: APP_ID
	});
	const label = labelFor(memo);
	let signature;
	try {
		signature = signatureOf(isInsideCherry()
			? await sendCherry(tx, label)
			: await sendBrowser(opened.connection, tx, label));
	} catch (error) {
		if (isUserReject(error)) throw new WalletResult('CANCELLED');
		throw error;
	}
	if (!signature) throw new WalletResult('FAILED', 'no signature');
	if (!await waitConfirmed(opened.connection, signature)) throw new WalletResult('UNCONFIRMED');
	return signature;
}

async function transfer(lamports, memo) {
	assertOnline();
	const amount = parseLamports(lamports);
	const memoText = memo && String(memo).trim() ? String(memo).trim() : '';
	recipientAddress();
	let payer = await currentPayer();
	try {
		return await payOnce(payer, amount, memoText);
	} catch (error) {
		const hinted = changedWallet(error);
		if (hinted == null) throw error;
		const bridge = getBridgeError(error);
		const code = (bridge && bridge.code) || error.code;
		if (code !== 'WALLET_CHANGED' && code !== 'NOT_A_SIGNER') throw error;
		const next = hinted || await currentPayer();
		if (!next || next === payer) throw error;
		payer = next;
		return payOnce(payer, amount, memoText);
	}
}

window.cinderWallet = {
	ready,
	insideCherry,
	connect,
	disconnect,
	transfer,
	packRecord,
	formatError
};
