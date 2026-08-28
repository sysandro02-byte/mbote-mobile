package com.loukatech.mbote.service

import android.content.Context
import com.loukatech.mbote.model.AppLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader

object MboteI18nService {
    private var translations: Map<String, Map<String, String>> = emptyMap()
    private val json = Json { ignoreUnknownKeys = true }

    fun initialize(context: Context) {
        try {
            val inputStream = context.assets.open("mbote_i18n_config.json")
            val content = InputStreamReader(inputStream).readText()
            val parsedObject = json.decodeFromString<JsonObject>(content)
            val resultMap = mutableMapOf<String, Map<String, String>>()

            parsedObject.forEach { (langKey, jsonElement) ->
                if (jsonElement is JsonObject) {
                    val stringsMap = mutableMapOf<String, String>()
                    jsonElement.forEach { (key, value) ->
                        stringsMap[key] = value.jsonPrimitive.content
                    }
                    resultMap[langKey] = stringsMap
                }
            }
            translations = resultMap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getString(key: String, language: AppLanguage, defaultVal: String = ""): String {
        val langCode = language.code
        val langMap = translations[langCode] ?: translations["fr"]
        return langMap?.get(key) ?: defaultVal.ifBlank { key }
    }
}
