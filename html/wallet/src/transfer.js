import { PublicKey, SystemProgram, Transaction, TransactionInstruction } from '@solana/web3.js';
import { withAppReference } from '@cherrydotfun/miniapp-sdk/solana';
import { APP_ID, FEE_BUFFER, MEMO_PROGRAM_ID, PACK_MEMO } from './chain.js';

const MEMO_PROGRAM = new PublicKey(MEMO_PROGRAM_ID);

export function labelFor(memo) {
	if (memo && String(memo).includes(PACK_MEMO)) return 'Buy the weapon pack';
	return 'Tip the developer';
}

export function buildTransaction({ payer, recipient, lamports, memo, blockhash, insideCherry, appId }) {
	const fromPubkey = payer instanceof PublicKey ? payer : new PublicKey(payer);
	const toPubkey = recipient instanceof PublicKey ? recipient : new PublicKey(recipient);
	let transfer = SystemProgram.transfer({
		fromPubkey,
		toPubkey,
		lamports
	});
	if (insideCherry) {
		transfer = withAppReference(transfer, appId || APP_ID);
	}
	const tx = new Transaction({
		feePayer: fromPubkey,
		recentBlockhash: blockhash
	});
	tx.add(transfer);
	if (memo) {
		tx.add(new TransactionInstruction({
			programId: MEMO_PROGRAM,
			keys: [{ pubkey: fromPubkey, isSigner: true, isWritable: false }],
			data: new TextEncoder().encode(memo)
		}));
	}
	return tx;
}

export function enoughBalance(balance, lamports) {
	return BigInt(balance) >= BigInt(lamports) + BigInt(FEE_BUFFER);
}
