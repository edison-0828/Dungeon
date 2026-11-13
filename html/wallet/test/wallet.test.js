import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { Keypair, PublicKey } from '@solana/web3.js';
import { BridgeError } from '@cherrydotfun/miniapp-sdk';
import { APP_ID, MEMO_PROGRAM_ID, containsPackPayment, ownedPackMemos, packMemo, rpcUrls } from '../src/chain.js';
import { formatError, WalletResult } from '../src/errors.js';
import { buildTransaction, enoughBalance, labelFor } from '../src/transfer.js';

const owner = Keypair.generate().publicKey.toBase58();
const recipient = Keypair.generate().publicKey.toBase58();

describe('chain record', () => {
	it('keeps a devnet rpc on devnet', () => {
		assert.deepEqual(rpcUrls('https://api.devnet.solana.com'), [
			'https://api.devnet.solana.com'
		]);
	});

	it('reads a paid weapon memo and skips a failed transfer', async () => {
		const paid = 'paid-signature';
		const rpc = async (method, params) => {
			if (method === 'getSignaturesForAddress') {
				return [
					{ signature: 'failed', memo: 'cindervault:weapon', err: { InstructionError: [0, 'Custom'] } },
					{ signature: paid, memo: '[1] cindervault:weapon', err: null }
				];
			}
			assert.equal(params[0], paid);
			return {
				meta: { err: null, innerInstructions: [] },
				transaction: {
					message: {
						instructions: [{
							parsed: {
								type: 'transfer',
								info: { source: owner, destination: recipient, lamports: 50_000_000 }
							}
						}]
					}
				}
			};
		};
		assert.deepEqual(await ownedPackMemos(owner, recipient, rpc), ['cindervault:weapon']);
		assert.equal(packMemo('something else'), null);
		assert.equal(containsPackPayment([{
			parsed: { type: 'transfer', info: { source: owner, destination: recipient, lamports: 1 } }
		}], owner, recipient), false);
	});
});

describe('transfer', () => {
	it('adds a memo only when the purchase has one', () => {
		const payer = Keypair.generate().publicKey;
		const to = new PublicKey(recipient);
		const tip = buildTransaction({
			payer,
			recipient: to,
			lamports: 10_000_000,
			memo: '',
			blockhash: '11111111111111111111111111111111',
			insideCherry: false
		});
		assert.equal(tip.instructions.length, 1);
		assert.equal(labelFor(''), 'Tip the developer');

		const pack = buildTransaction({
			payer,
			recipient: to,
			lamports: 50_000_000,
			memo: 'cindervault:weapon',
			blockhash: '11111111111111111111111111111111',
			insideCherry: true,
			appId: APP_ID
		});
		assert.equal(pack.instructions.length, 2);
		assert.equal(pack.instructions[0].keys.length, 3);
		const reference = pack.instructions[0].keys[2];
		assert.equal(reference.isSigner, false);
		assert.equal(reference.isWritable, false);
		assert.equal(pack.instructions[1].programId.toBase58(), MEMO_PROGRAM_ID);
		assert.equal(new TextDecoder().decode(pack.instructions[1].data), 'cindervault:weapon');
		assert.equal(labelFor('cindervault:weapon'), 'Buy the weapon pack');
		assert.equal(enoughBalance(50_004_999, 50_000_000), false);
		assert.equal(enoughBalance(50_005_000, 50_000_000), true);
	});
});

describe('errors', () => {
	it('keeps already, cancel, and policy codes distinct', () => {
		assert.equal(formatError(new WalletResult('ALREADY', 'cindervault:weapon')), 'ALREADY\ncindervault:weapon');
		assert.equal(formatError(new WalletResult('SHORT')), 'SHORT\n');
		assert.equal(formatError({ code: 4001, message: 'User rejected the request.' }), 'CANCELLED\n');
		const denied = new BridgeError('over the limit', 'POLICY_DENIED', { code: 'TX_LIMIT' });
		assert.equal(formatError(denied), 'POLICY\nTX_LIMIT');
	});
});
