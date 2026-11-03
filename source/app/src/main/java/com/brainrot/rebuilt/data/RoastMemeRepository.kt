package com.brainrot.rebuilt.data

import android.content.Context
import com.brainrot.rebuilt.domain.model.RoastMeme
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader
import kotlin.random.Random

class RoastMemeRepository(private val context: Context) {

    private val cachedPools = mutableMapOf<String, List<RoastMeme>>()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        try {
            context.assets.open("roast_gifs.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val root = JsonParser.parseReader(reader).asJsonObject
                    if (root.has("pools")) {
                        val poolsObj = root.getAsJsonObject("pools")
                        for (entry in poolsObj.entrySet()) {
                            val lang = entry.key
                            val array = entry.value.asJsonArray
                            val memes = mutableListOf<RoastMeme>()
                            for (elem in array) {
                                val obj = elem.asJsonObject
                                val id = obj.get("id")?.asString ?: "meme"
                                val source = obj.get("source")?.asString ?: "tenor"
                                val url = obj.get("url")?.asString ?: ""
                                val caption = obj.get("caption")?.asString ?: "Time to get back to work!"
                                val w = obj.get("w")?.asInt ?: 400
                                val h = obj.get("h")?.asInt ?: 400
                                memes.add(RoastMeme(id, source, url, caption, w, h, lang))
                            }
                            cachedPools[lang] = memes
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Provide fallback English memes if file reading fails
            cachedPools["english"] = listOf(
                RoastMeme("fallback_1", "local", "", "Stop scrolling! Your future self is waiting.", 400, 400, "english"),
                RoastMeme("fallback_2", "local", "", "Another reel? Really?", 400, 400, "english")
            )
        }
    }

    fun getAvailableLanguages(): List<String> {
        return cachedPools.keys.toList().sorted()
    }

    fun getRandomMeme(language: String = "english"): RoastMeme {
        val pool = cachedPools[language] ?: cachedPools["english"] ?: cachedPools.values.firstOrNull()
        if (pool.isNullOrEmpty()) {
            return RoastMeme("fallback_default", "local", "", "Time's up! Put down the screen.", 400, 400, language)
        }
        return pool[Random.nextInt(pool.size)]
    }

    fun getPoolSize(language: String): Int {
        return cachedPools[language]?.size ?: 0
    }
}

