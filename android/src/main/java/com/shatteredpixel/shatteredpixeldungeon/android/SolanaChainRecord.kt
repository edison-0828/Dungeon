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
 */

package com.shatteredpixel.shatteredpixeldungeon.android

import com.shatteredpixel.shatteredpixeldungeon.GateMerchant
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads pack memos after the wallet sheet has already closed.
 * Keeping this off the Mobile Wallet Adapter session means a slow RPC call
 * cannot cancel an authorization the user already approved.
 */
object SolanaChainRecord {

    fun rpcUrls(): List<String> {
        val urls = LinkedHashSet<String>()
        val configured = AndroidPlatformSupport.tipRpcUrl()
        if (configured.isNotEmpty()) urls.add(configured)
        if (BuildConfig.TIP_CLUSTER == "mainnet") {
            urls.add("https://api.mainnet-beta.solana.com")
            urls.add("https://solana-rpc.publicnode.com")
        } else {
            urls.add("https://api.devnet.solana.com")
        }
        return urls.toList()
    }

    fun ownedPackMemos(owner: String, recipient: String): Set<String> {
        var last: Throwable? = null
        for (url in rpcUrls()) {
            try {
                return scan(url, owner, recipient)
            } catch (error: Throwable) {
                last = error
            }
        }
        throw last ?: IllegalStateException("RPC failed")
    }

    private fun scan(rpcUrl: String, owner: String, recipient: String): Set<String> {
        val found = linkedSetOf<String>()
        var before: String? = null
        repeat(5) {
            val page = signaturePage(rpcUrl, owner, before)
            if (page.length() == 0) return found
            for (i in 0 until page.length()) {
                val row = page.optJSONObject(i) ?: continue
                if (row.has("err") && !row.isNull("err")) continue
                val memo = packMemo(row.optString("memo")) ?: continue
                val signature = row.optString("signature")
                if (signature.isEmpty()) continue
                if (paidPack(rpcUrl, signature, owner, recipient)) found.add(memo)
                if (found.size == PACK_MEMOS.size) return found
            }
            if (page.length() < 1000) return found
            before = page.optJSONObject(page.length() - 1)?.optString("signature")
            if (before.isNullOrEmpty()) return found
        }
        return found
    }

    private fun signaturePage(rpcUrl: String, owner: String, before: String?): JSONArray {
        val config = JSONObject().put("limit", 1000).put("commitment", "confirmed")
        if (!before.isNullOrEmpty()) config.put("before", before)
        val result = rpc(rpcUrl, "getSignaturesForAddress", JSONArray().put(owner).put(config))
        return result.optJSONArray("result") ?: JSONArray()
    }

    private fun paidPack(rpcUrl: String, signature: String, owner: String, recipient: String): Boolean {
        val config = JSONObject()
            .put("encoding", "jsonParsed")
            .put("commitment", "confirmed")
            .put("maxSupportedTransactionVersion", 0)
        val result = rpc(rpcUrl, "getTransaction", JSONArray().put(signature).put(config))
        val tx = result.optJSONObject("result") ?: return false
        if (tx.has("meta") && tx.optJSONObject("meta")?.has("err") == true
            && tx.optJSONObject("meta")?.isNull("err") == false) {
            return false
        }
        val message = tx.optJSONObject("transaction")?.optJSONObject("message")
        if (containsPayment(message?.optJSONArray("instructions"), owner, recipient)) return true
        val inner = tx.optJSONObject("meta")?.optJSONArray("innerInstructions") ?: return false
        for (i in 0 until inner.length()) {
            if (containsPayment(inner.optJSONObject(i)?.optJSONArray("instructions"), owner, recipient)) return true
        }
        return false
    }

    private fun containsPayment(instructions: JSONArray?, owner: String, recipient: String): Boolean {
        if (instructions == null) return false
        for (i in 0 until instructions.length()) {
            val parsed = instructions.optJSONObject(i)?.optJSONObject("parsed") ?: continue
            if (parsed.optString("type") != "transfer") continue
            val info = parsed.optJSONObject("info") ?: continue
            if (info.optString("source") == owner
                && info.optString("destination") == recipient
                && info.optLong("lamports") >= GateMerchant.PACK_LAMPORTS) {
                return true
            }
        }
        return false
    }

    private fun rpc(rpcUrl: String, method: String, params: JSONArray): JSONObject {
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", params)
        val connection = URL(rpcUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("RPC HTTP $code")
            val root = try {
                JSONObject(body)
            } catch (malformed: JSONException) {
                error("RPC failed")
            }
            if (root.has("error") && !root.isNull("error")) {
                val message = root.optJSONObject("error")?.optString("message").orEmpty()
                error(if (message.isEmpty()) "RPC failed" else message)
            }
            return root
        } finally {
            connection.disconnect()
        }
    }

    private fun packMemo(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        for (memo in PACK_MEMOS) {
            if (raw.contains(memo)) return memo
        }
        return null
    }

    private val PACK_MEMOS = arrayOf(GateMerchant.Offer.WEAPON.memo)
}
