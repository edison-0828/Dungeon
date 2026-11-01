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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.watabou.utils.GameSettings;

import java.util.ArrayList;
import java.util.Locale;

public class GateMerchant {

	public static final long PACK_LAMPORTS = 50_000_000L;

	public enum Result {
		SAVED,
		DUPLICATE
	}

	public enum Offer {
		WEAPON("weapon", PACK_LAMPORTS, "cindervault:weapon") {
			@Override
			public ArrayList<Item> contents() {
				ArrayList<Item> items = new ArrayList<>();
				Shortsword sword = new Shortsword();
				sword.level(3);
				items.add(sword.identify());
				LeatherArmor armor = new LeatherArmor();
				armor.level(3);
				items.add(armor.identify());
				RingOfAccuracy ring = new RingOfAccuracy();
				ring.level(1);
				items.add(ring.identify());
				items.add(new ThrowingKnife().quantity(3).identify());
				return items;
			}
		};

		public final String id;
		public final long lamports;
		public final String memo;

		Offer(String id, long lamports, String memo) {
			this.id = id;
			this.lamports = lamports;
			this.memo = memo;
		}

		public abstract ArrayList<Item> contents();

		public static Offer byMemo(String memo) {
			if (memo == null) return null;
			for (Offer offer : values()) {
				if (memo.contains(offer.memo)) return offer;
			}
			return null;
		}

		public static Offer byId(String id) {
			for (Offer offer : values()) {
				if (offer.id.equals(id)) return offer;
			}
			return null;
		}
	}

	private static final String KEY_STOCK = "gate_merchant_stock";
	private static final String KEY_SIGS = "gate_merchant_sigs";
	private static final String KEY_OWNED = "gate_merchant_owned";
	private static final String KEY_DELIVERED = "gate_merchant_delivered";
	private static final String KEY_WALLET = "gate_merchant_wallet";
	private static final int MAX_SIGS = 24;

	private GateMerchant() {}

	public static int pendingCount() {
		return tokens().size();
	}

	public static ArrayList<Offer> stock() {
		return readStock();
	}

	public static String wallet() {
		String wallet = GameSettings.getString(KEY_WALLET, "");
		return wallet == null ? "" : wallet;
	}

	public static boolean claimed(Offer offer) {
		return offer != null && (contains(KEY_OWNED, offer.id)
				|| contains(KEY_DELIVERED, offer.id)
				|| tokens().contains(offer.id));
	}

	public static void noteChain(String wallet, String memoCsv) {
		if (wallet != null && !wallet.isEmpty()) GameSettings.put(KEY_WALLET, wallet.trim());
		ArrayList<String> owned = readList(KEY_OWNED);
		ArrayList<String> stock = tokens();
		ArrayList<String> delivered = readList(KEY_DELIVERED);
		if (memoCsv != null) {
			for (String part : memoCsv.split(",")) {
				Offer offer = Offer.byMemo(part.trim());
				if (offer == null) continue;
				if (!owned.contains(offer.id)) owned.add(offer.id);
				if (!delivered.contains(offer.id) && !stock.contains(offer.id)) stock.add(offer.id);
			}
		}
		writeList(KEY_OWNED, owned);
		writeTokens(stock);
	}

	public static Result remember(Offer offer, String signature) {
		String sig = signature == null ? "" : signature.trim();
		ArrayList<String> sigs = readSigs();
		if (!sig.isEmpty() && sigs.contains(sig)) {
			return Result.DUPLICATE;
		}
		if (contains(KEY_OWNED, offer.id) || contains(KEY_DELIVERED, offer.id)) {
			return Result.DUPLICATE;
		}
		ArrayList<String> owned = readList(KEY_OWNED);
		owned.add(offer.id);
		writeList(KEY_OWNED, owned);
		ArrayList<String> stock = tokens();
		if (!stock.contains(offer.id)) {
			stock.add(offer.id);
			writeTokens(stock);
		}
		if (!sig.isEmpty()) {
			sigs.add(sig);
			while (sigs.size() > MAX_SIGS) {
				sigs.remove(0);
			}
			writeSigs(sigs);
		}
		return Result.SAVED;
	}

	public static void deliver(Hero hero) {
		ArrayList<String> left = new ArrayList<>();
		ArrayList<String> delivered = readList(KEY_DELIVERED);
		ArrayList<String> owned = readList(KEY_OWNED);
		for (String id : owned) {
			if (Offer.byId(id) == null) continue;
			if (grant(id) && !delivered.contains(id)) delivered.add(id);
		}
		for (String token : tokens()) {
			if (Offer.byId(token) != null) {
				if (owned.contains(token)) {
					left.add(token);
				} else if (!grant(token)) {
					left.add(token);
				} else if (!delivered.contains(token)) {
					delivered.add(token);
				}
				continue;
			}
			if (grant(token)) {
				if (!delivered.contains(token)) delivered.add(token);
			} else {
				left.add(token);
			}
		}
		writeTokens(left);
		writeList(KEY_DELIVERED, delivered);
	}

	private static boolean grant(String token) {
		ArrayList<Item> items = itemsFor(token);
		if (items.isEmpty()) return false;
		boolean stored = true;
		for (Item item : items) {
			if (!item.collect()) stored = false;
		}
		return stored;
	}

	private static ArrayList<Item> itemsFor(String token) {
		Offer offer = Offer.byId(token);
		if (offer != null) return offer.contents();
		ArrayList<Item> items = new ArrayList<>();
		if ("bundle".equals(token)) {
			items.add(new TimekeepersHourglass().identify());
			items.add(new RingOfHaste().identify());
			items.add(new Food().quantity(5).identify());
		} else if ("shortsword".equals(token)) {
			items.add(new Shortsword().identify());
		} else if ("leather".equals(token)) {
			items.add(new LeatherArmor().identify());
		} else if ("mail".equals(token)) {
			items.add(new MailArmor().identify());
		}
		return items;
	}

	public static String formatSol(long lamports) {
		long whole = lamports / 1_000_000_000L;
		int frac = (int) (lamports % 1_000_000_000L);
		if (frac == 0) return Long.toString(whole);
		String digits = String.format(Locale.US, "%09d", frac);
		int end = digits.length();
		while (end > 0 && digits.charAt(end - 1) == '0') end--;
		return whole + "." + digits.substring(0, end);
	}

	private static ArrayList<String> tokens() {
		ArrayList<String> tokens = new ArrayList<>();
		String raw = GameSettings.getString(KEY_STOCK, "");
		if (raw == null || raw.isEmpty()) return tokens;
		for (String part : raw.split(",")) {
			String token = part.trim();
			if (known(token)) tokens.add(token);
		}
		return tokens;
	}

	private static void writeTokens(ArrayList<String> tokens) {
		StringBuilder raw = new StringBuilder();
		for (String token : tokens) {
			if (raw.length() > 0) raw.append(',');
			raw.append(token);
		}
		GameSettings.put(KEY_STOCK, raw.toString());
	}

	private static boolean known(String token) {
		return Offer.byId(token) != null
				|| "bundle".equals(token)
				|| "shortsword".equals(token)
				|| "leather".equals(token)
				|| "mail".equals(token);
	}

	private static boolean contains(String key, String id) {
		return readList(key).contains(id);
	}

	private static ArrayList<String> readList(String key) {
		ArrayList<String> values = new ArrayList<>();
		String raw = GameSettings.getString(key, "");
		if (raw == null || raw.isEmpty()) return values;
		for (String part : raw.split(",")) {
			String token = part.trim();
			if (!token.isEmpty() && !values.contains(token)) values.add(token);
		}
		return values;
	}

	private static void writeList(String key, ArrayList<String> values) {
		StringBuilder raw = new StringBuilder();
		for (String value : values) {
			if (raw.length() > 0) raw.append(',');
			raw.append(value);
		}
		GameSettings.put(key, raw.toString());
	}

	private static ArrayList<Offer> readStock() {
		ArrayList<Offer> stock = new ArrayList<>();
		for (String token : tokens()) {
			Offer offer = Offer.byId(token);
			if (offer != null) stock.add(offer);
		}
		return stock;
	}

	private static ArrayList<String> readSigs() {
		ArrayList<String> sigs = new ArrayList<>();
		String raw = GameSettings.getString(KEY_SIGS, "");
		if (raw == null || raw.isEmpty()) return sigs;
		for (String part : raw.split(",")) {
			String sig = part.trim();
			if (!sig.isEmpty()) sigs.add(sig);
		}
		return sigs;
	}

	private static void writeSigs(ArrayList<String> sigs) {
		StringBuilder raw = new StringBuilder();
		for (String sig : sigs) {
			if (raw.length() > 0) raw.append(',');
			raw.append(sig);
		}
		GameSettings.put(KEY_SIGS, raw.toString());
	}
}
