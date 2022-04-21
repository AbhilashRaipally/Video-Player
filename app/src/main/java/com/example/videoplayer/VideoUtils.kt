package com.example.videoplayer

import android.content.Context
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.IOException

object VideoUtils {

    const val NOTIFICATION_ID = 3001
    const val PLAYBACK_CHANNEL_ID = "com.example.videoplayer"

    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }

    fun getSampleVideos(context: Context): List<Video>{
        val jsonFileString = getJsonDataFromAsset(context, "media.exolist.json")
        val gson = Gson()
        val listVideoType = object : TypeToken<List<Video>>() {}.type
        return gson.fromJson(jsonFileString, listVideoType)
    }

}