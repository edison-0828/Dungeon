export function signedInText(identity) {
	const source = identity || {};
	const name = String(source.displayName || '').trim();
	const keys = [];
	for (const value of [source.publicKey, source.wallet]) {
		const text = String(value || '').trim();
		if (text && !keys.includes(text)) keys.push(text);
	}
	return [name, ...keys].filter(Boolean).join(' ');
}

export function identityFromCherry(app) {
	const user = app && app.user ? app.user : {};
	const claims = app && app.claims ? app.claims : {};
	const claimUser = claims.user || {};
	const displayName = user.displayName || claimUser.display_name || claimUser.displayName || '';
	const wallet = user.publicKey || claims.sub || (app && app.wallet && app.wallet.publicKey) || '';
	const publicKey = (app && app.paymentWallet) || wallet;
	return { displayName, publicKey, wallet };
}
