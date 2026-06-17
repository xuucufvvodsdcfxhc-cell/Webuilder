package com.example.data

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"

    suspend fun generateWebSnippet(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "{\"responseAr\": \"الرجاء إدخال مفتاح API الخاص بـ Gemini في Secrets لتفعيل المساعد الذكي!\", \"htmlSnippet\": \"<div class='p-6 bg-red-50 border border-red-200 text-red-800 rounded-xl'><p class='font-bold'>تنبيه:</p><p class='text-sm'>مفتاح GEMINI_API_KEY غير متوفر أو فارغ. يرجى إضافته عبر لوحة Secrets في AI Studio لتصميم صفحات بالذكاء الاصطناعي المباشر.</p></div>\"}"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val systemInstruction = """
            You are an advanced Web AI Assistant inside an Android IDE inspired by Sketchware Pro.
            The user wants to generate code components, visual layouts, pages, or forms.
            You must structure your response strictly inside a JSON object with two fields (do not output any other text or markdown wrapping):
            1. "responseAr": A friendly, helpful Arabic summary (max 2 sentences) explaining the generated design element.
            2. "htmlSnippet": A valid, single block HTML element string (using nested Tailwind CSS class configurations for rich design - gradients, glassmorphism, flex containers, grids) representing the exact design requested. Do not include separate full page boilerplate html/head/body tags unless requested, just a complete reusable parent element div or card block containing text and tags.
            
            Example layout output format:
            {"responseAr": "لقد صممت لك قسماً جذاباً لعرض الخدمات بتأثيرات بصرية عصرية وحركية.", "htmlSnippet": "<div class='bg-slate-900 text-white rounded-2xl p-8 flex flex-col gap-4 shadow-xl border border-slate-800'><h3 class='text-2xl font-extrabold text-blue-400'>خدماتنا</h3><p class='text-slate-400'>نقدم أفضل الحلول البرمجية الممتازة لتطوير أعمالك في أسرع وقت.</p></div>"}
        """.trimIndent()

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Prompt: $prompt")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val requestBody = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "{\"responseAr\": \"فشل التواصل مع مخدم Google AI. كود الخطأ: ${response.code}\", \"htmlSnippet\": \"\"}"
                }
                val bodyString = response.body?.string() ?: return@withContext "{\"responseAr\": \"رد فارغ من المخدم.\", \"htmlSnippet\": \"\"}"
                
                val rootJson = JSONObject(bodyString)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "{}")
                        }
                    }
                }
                "{\"responseAr\": \"لم يتم العثور على رد مناسب.\", \"htmlSnippet\": \"\"}"
            }
        } catch (e: Exception) {
            "{\"responseAr\": \"حدث خطأ غير متوقع: ${e.localizedMessage}\", \"htmlSnippet\": \"\"}"
        }
    }
}
