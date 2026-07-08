package com.example.parentalcontrolapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CloudConfigManager {
    private const val BASE_URL = "http://10.0.2.2:8080"
    private const val ENDPOINT = "/%E5%AE%B6%E6%97%8F_accounts/family_id/blocked_packages"
    
    private val client = OkHttpClient()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    
    @Volatile
    var blockedPackages: Set<String> = setOf(
        "com.google.android.youtube",
        "com.instagram.android"
    )
        private set

    init {
        // Poll mock server every 3 seconds to fetch remote configurations
        scheduler.scheduleWithFixedDelay({
            fetchBlockedPackages()
        }, 0, 3, TimeUnit.SECONDS)
    }

    fun fetchBlockedPackages() {
        val request = Request.Builder()
            .url(BASE_URL + ENDPOINT)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CloudConfigManager", "Failed to fetch remote config: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    try {
                        val jsonArray = JSONArray(body)
                        val packages = mutableSetOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            packages.add(jsonArray.getString(i))
                        }
                        blockedPackages = packages
                        Log.d("CloudConfigManager", "Updated blocked packages from cloud: $blockedPackages")
                    } catch (e: Exception) {
                        Log.e("CloudConfigManager", "JSON parsing error: ${e.message}")
                    }
                } else {
                    Log.w("CloudConfigManager", "Server returned error code: ${response.code}")
                }
                response.close()
            }
        })
    }

    fun updateBlockedPackages(packages: List<String>) {
        val jsonArray = JSONArray(packages)
        val requestBody = jsonArray.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL + ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CloudConfigManager", "Failed to upload remote config: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("CloudConfigManager", "Uploaded config successfully to cloud: $packages")
                    fetchBlockedPackages() // immediately trigger update
                } else {
                    Log.w("CloudConfigManager", "Upload server returned error: ${response.code}")
                }
                response.close()
            }
        })
    }
}
