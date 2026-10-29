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
 * Mobile Wallet Adapter client, web3-solana, and rpc-solana are Apache-2.0.
 */

package com.shatteredpixel.shatteredpixeldungeon.android

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.SupporterScene
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.networking.KtorNetworkDriver
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.Commitment
import com.solana.rpc.SolanaRpcClient
import com.solana.transaction.Message
import com.solana.transaction.toUnsignedTransaction
import com.watabou.utils.PlatformSupport
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * One voluntary SOL transfer. The session does not set a wallet package, so Android lists every
 * installed Mobile Wallet Adapter wallet. Nothing here is written to the save or to preferences.
 */
class SolanaTipActivity : ComponentActivity() {

    private lateinit var sender: ActivityResultSender
    private lateinit var walletAdapter: MobileWalletAdapter
    private var delivered = false
    private var sessionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sender = ActivityResultSender(this)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val waiting = TextView(this)
        waiting.setTextColor(Color.WHITE)
        waiting.gravity = Gravity.CENTER
        waiting.textSize = 16f
        waiting.setPadding(48, 48, 48, 48)
        waiting.text = Messages.get(SupporterScene::class.java, "tip_working")
        setContentView(waiting)

        val identityName = applicationInfo.loadLabel(packageManager).toString()
        walletAdapter = MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse(IDENTITY_URI),
                iconUri = Uri.parse("favicon.ico"),
                identityName = identityName
            )
        )
        walletAdapter.blockchain = if (BuildConfig.TIP_CLUSTER == "mainnet") {
            Solana.Mainnet
        } else {
            Solana.Devnet
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionStarted || delivered) return
        sessionStarted = true
        // Wait until this window is actually in front. A translucent activity that calls
        // startActivity from onCreate is treated as a background launch on many phones,
        // so the wallet never appears. Seeker's preinstalled wallet is exempt from that.
        window.decorView.post { if (!delivered && !isFinishing) beginTip() }
    }

    private fun beginTip() {
        val lamports = intent.getLongExtra(EXTRA_LAMPORTS, 0L)
        if (lamports <= 0L) {
            deliver(PlatformSupport.SolanaTip.FAILED, "")
            return
        }

        lifecycleScope.launch {
            val result = try {
                walletAdapter.transact(sender) { authResult ->
                    val account = authResult.accounts.firstOrNull()
                        ?: error("no account")
                    val senderKey = SolanaPublicKey(account.publicKey)
                    val recipient = SolanaPublicKey.from(BuildConfig.TIP_RECIPIENT)
                    val rpcUrl = AndroidPlatformSupport.tipRpcUrl()
                    val rpc = SolanaRpcClient(rpcUrl, KtorNetworkDriver())
                    val balance = rpc.getBalance(senderKey, Commitment.CONFIRMED).let { response ->
                        response.result ?: error(response.error?.message ?: "no balance")
                    }
                    if (balance < lamports + BASE_FEE_LAMPORTS) {
                        throw ShortBalance()
                    }
                    val blockhash = rpc.getLatestBlockhash(Commitment.CONFIRMED).result?.blockhash
                        ?: error("no blockhash")
                    val bytes = Message.Builder()
                        .addInstruction(SystemProgram.transfer(senderKey, recipient, lamports))
                        .setRecentBlockhash(blockhash)
                        .build()
                        .toUnsignedTransaction()
                        .serialize()
                    withContext(Dispatchers.IO) {
                        rejectFailedSimulation(rpcUrl, bytes)
                    }
                    signAndSendTransactions(arrayOf(bytes)).signatures.firstOrNull()
                }
            } catch (cancelled: CancellationException) {
                if (!delivered) deliver(PlatformSupport.SolanaTip.CANCELLED, "")
                throw cancelled
            } catch (shortBalance: ShortBalance) {
                deliver(PlatformSupport.SolanaTip.FAILED, PlatformSupport.SolanaTip.SHORT)
                return@launch
            } catch (error: Throwable) {
                deliver(PlatformSupport.SolanaTip.FAILED, clip(error.message ?: error.javaClass.simpleName))
                return@launch
            }

            when (result) {
                is TransactionResult.NoWalletFound ->
                    deliver(PlatformSupport.SolanaTip.NO_WALLET, "")
                is TransactionResult.Failure -> {
                    val message = result.message
                    if (message == PlatformSupport.SolanaTip.SHORT) {
                        deliver(PlatformSupport.SolanaTip.FAILED, PlatformSupport.SolanaTip.SHORT)
                    } else if (isCancel(message)) {
                        deliver(PlatformSupport.SolanaTip.CANCELLED, "")
                    } else {
                        deliver(PlatformSupport.SolanaTip.FAILED, clip(message))
                    }
                }
                is TransactionResult.Success -> {
                    val signature = result.payload
                    val encoded = if (signature == null || signature.isEmpty()) {
                        ""
                    } else {
                        Base58.encodeToString(signature)
                    }
                    deliver(PlatformSupport.SolanaTip.SUBMITTED, encoded)
                }
            }
        }
    }

    override fun onDestroy() {
        if (!delivered) {
            deliver(PlatformSupport.SolanaTip.CANCELLED, "")
        }
        super.onDestroy()
    }

    private fun rejectFailedSimulation(rpcUrl: String, transaction: ByteArray) {
        val config = JSONObject()
            .put("encoding", "base64")
            .put("sigVerify", false)
            .put("replaceRecentBlockhash", false)
            .put("commitment", "confirmed")
        val params = JSONArray()
            .put(Base64.encodeToString(transaction, Base64.NO_WRAP))
            .put(config)
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "simulateTransaction")
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
            if (code !in 200..299) {
                error(simulationDetail(body, "simulation HTTP $code"))
            }
            val root = try {
                JSONObject(body)
            } catch (malformed: JSONException) {
                error("simulation failed")
            }
            if (root.has("error") && !root.isNull("error")) {
                val message = root.optJSONObject("error")?.optString("message").orEmpty()
                error(if (message.isEmpty()) "simulation failed" else message)
            }
            val value = root.optJSONObject("result")?.optJSONObject("value")
                ?: error("simulation failed")
            if (!value.has("err") || !value.isNull("err")) {
                val err = value.opt("err")
                error(if (err == null || err === JSONObject.NULL) "simulation failed" else err.toString())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun simulationDetail(body: String, fallback: String): String {
        return try {
            val message = JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
            if (message.isEmpty()) fallback else message
        } catch (ignored: JSONException) {
            fallback
        }
    }

    private fun deliver(kind: Int, detail: String) {
        if (delivered) return
        delivered = true
        listener?.onResult(kind, detail)
        if (!isFinishing) finish()
    }

    companion object {
        const val EXTRA_LAMPORTS = "lamports"
        private const val BASE_FEE_LAMPORTS = 5_000L
        private const val IDENTITY_URI = "https://cinder.zhijianzaowu.com/"

        private class ShortBalance : RuntimeException(PlatformSupport.SolanaTip.SHORT)

        @JvmField
        var tipInFlight: Boolean = false

        @JvmField
        var listener: PlatformSupport.SolanaTipListener? = null

        private fun isCancel(message: String): Boolean {
            val lower = message.lowercase()
            return lower.contains("did not authorize")
                    || lower.contains("interrupted")
                    || lower.contains("cancel")
        }

        private fun clip(message: String): String {
            val trimmed = message.trim()
            return if (trimmed.length <= 180) trimmed else trimmed.substring(0, 180)
        }
    }
}
