package com.example.voiceeye

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var etBot: EditText
    private lateinit var etChat: EditText
    private lateinit var cbWifiOnly: CheckBox

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        tvStatus.text = if (granted) "Permission granted" else "Permission denied"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        NotificationHelper.createChannel(this)

        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        etBot = findViewById(R.id.etBotToken)
        etChat = findViewById(R.id.etChatId)
        cbWifiOnly = findViewById(R.id.cbWifiOnly)

        btnStart.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            val i = Intent(this, RecordingService::class.java)
            i.putExtra("TELEGRAM_BOT_TOKEN", etBot.text.toString())
            i.putExtra("TELEGRAM_CHAT_ID", etChat.text.toString())
            i.putExtra("UPLOAD_WIFI_ONLY", cbWifiOnly.isChecked)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
            tvStatus.text = "Service started"
        }

        btnStop.setOnClickListener {
            val i = Intent(this, RecordingService::class.java)
            stopService(i)
            tvStatus.text = "Service stopped"
        }
    }
}
