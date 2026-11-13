import { getBridgeError, isPolicyDenied } from '@cherrydotfun/miniapp-sdk';

export class WalletResult extends Error {
	constructor(code, detail = '') {
		super(detail || code);
		this.name = 'WalletResult';
		this.code = code;
		this.detail = detail;
	}
}

export function formatError(error) {
	if (error instanceof WalletResult) {
		return error.code + '\n' + (error.detail || '');
	}
	if (isUserReject(error)) return 'CANCELLED\n';
	if (isPolicyDenied(error)) {
		const bridge = getBridgeError(error);
		return 'POLICY\n' + (bridge && bridge.policyCode ? bridge.policyCode : '');
	}
	const bridge = getBridgeError(error);
	const message = (bridge && bridge.message) || (error && error.message) || '';
	return 'FAILED\n' + String(message).replace(/[\r\n]+/g, ' ').slice(0, 180);
}

export function isUserReject(error) {
	const bridge = getBridgeError(error);
	const code = (bridge && bridge.code) || (error && error.code);
	if (code === 'USER_REJECTED' || code === 4001 || code === 'ACTION_REJECTED') return true;
	const message = String((error && error.message) || '');
	return /user rejected the request/i.test(message);
}

export function changedWallet(error) {
	const bridge = getBridgeError(error);
	const code = (bridge && bridge.code) || (error && error.code);
	if (code !== 'WALLET_CHANGED' && code !== 'NOT_A_SIGNER') return null;
	const data = (bridge && bridge.data) || (error && error.data) || {};
	const hinted = data.publicKey || data.wallet || data.paymentWallet || data.address;
	return typeof hinted === 'string' ? hinted : '';
}
