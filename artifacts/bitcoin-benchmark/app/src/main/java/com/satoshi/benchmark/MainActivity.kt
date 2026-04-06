package com.satoshi.benchmark

import android.app.AlertDialog
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {

    private lateinit var editTargetAddress: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvKeysPerSec: TextView
    private lateinit var tvTotalKeys: TextView
    private lateinit var tvConsole: TextView
    private lateinit var scrollConsole: ScrollView

    private val isRunning = AtomicBoolean(false)
    private val totalKeys = AtomicLong(0)
    private val keysThisSecond = AtomicLong(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val numberFormat = NumberFormat.getNumberInstance()
    private val networkParams by lazy { MainNetParams.get() }
    private val targetHashSet = HashSet<String>()
    private var benchmarkThread: Thread? = null

    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            if (isRunning.get()) {
                val kps = keysThisSecond.getAndSet(0)
                val total = totalKeys.get()
                tvKeysPerSec.text = numberFormat.format(kps)
                tvTotalKeys.text = numberFormat.format(total)
                appendConsole("[STAT] " + numberFormat.format(kps) + " keys/sec | total: " + numberFormat.format(total))
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            editTargetAddress = findViewById(R.id.editTargetAddress)
            btnStart = findViewById(R.id.btnStart)
            btnStop = findViewById(R.id.btnStop)
            tvKeysPerSec = findViewById(R.id.tvKeysPerSec)
            tvTotalKeys = findViewById(R.id.tvTotalKeys)
            tvConsole = findViewById(R.id.tvConsole)
            scrollConsole = findViewById(R.id.scrollConsole)
            btnStart.setOnClickListener { startBenchmark() }
            btnStop.setOnClickListener { stopBenchmark() }
            appendConsole("[INFO] App started successfully")
            appendConsole("[INFO] BitcoinJ 0.16.2 | Mainnet | P2PKH")
            loadTargetsFromAssets()
            appendConsole("[INFO] Ready. Press START BENCHMARK.")
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Startup Error")
                .setMessage(e.javaClass.simpleName + ": " + e.message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun loadTargetsFromAssets() {
        try {
            val reader = BufferedReader(InputStreamReader(assets.open("targets.txt")))
            var count
