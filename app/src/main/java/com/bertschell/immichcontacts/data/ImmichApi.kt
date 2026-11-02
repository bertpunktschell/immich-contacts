package com.bertschell.immichcontacts.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import com.bertschell.immichcontacts.data.model.*
import com.bertschell.immichcontacts.data.model.ImmichAsset
import io.ktor.http.isSuccess

class ImmichApi(private val baseUrl: String, inApiKey: String) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val apiKey = inApiKey
    private val TAG = "ImmichApi"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(DefaultRequest) {
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
        }
    }

    private fun apiPath(path: String): String {
        val base = baseUrl.trimEnd('/')
        return if (path.startsWith("/api")) "$base$path" else "$base/api$path"
    }

    suspend fun testConnection(): ImmichUser {
        return client.get(apiPath("/users/me")).body()
    }

    suspend fun getServerVersion(): ImmichServerVersion {
        return client.get(apiPath("/server/version")).body()
    }

    suspend fun getPeople(): ImmichPeopleResponse {
        return client.get(apiPath("/people")).body()
    }

    suspend fun getPerson(personId: String): ImmichPerson {
        return client.get(apiPath("/people/$personId")).body()
    }

    suspend fun getAssetsForPerson(personId: String, page: Int = 1, size: Int = 100): ImmichSearchResponse {
        val body = buildJsonObject {
            put("personIds", JsonArray(listOf(JsonPrimitive(personId))))
            put("type", "IMAGE")
            put("page", page)
            put("size", size)
        }
        val response = client.post(apiPath("/search/metadata")) { setBody(body) }
        return parseSearchResponse(response)
    }

    /** Fetch as raw string, log it, then tolerate: typed parse -> manual fallback parse. */
    private suspend fun parseSearchResponse(response: HttpResponse): ImmichSearchResponse {
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP ${response.status}: ${raw.take(300)}")
        }
        return try {
            json.decodeFromString<ImmichSearchResponse>(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Typed parse failed (${e.message}); manual fallback", e)
            manualParse(raw)
        }
    }

    /** Manual parse: extract asset ids from assets.items regardless of unknown shapes. */
    private fun manualParse(raw: String): ImmichSearchResponse = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        val assetsObj = root["assets"] as? JsonObject
        val itemsArr = (assetsObj?.get("items") ?: root["items"])?.jsonArray ?: JsonArray(emptyList())
        val assets = itemsArr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            ImmichAsset(
                id = id,
                type = (obj["type"] as? JsonPrimitive)?.content ?: "IMAGE",
                originalFileName = (obj["originalFileName"] as? JsonPrimitive)?.content ?: "",
                fileCreatedAt = (obj["fileCreatedAt"] as? JsonPrimitive)?.content ?: "",
                localDateTime = (obj["localDateTime"] as? JsonPrimitive)?.content ?: "",
            )
        }
        ImmichSearchResponse(assets = ImmichSearchAssets(total = assets.size, items = assets))
    }.getOrElse {
        Log.e(TAG, "manualParse failed entirely", it)
        ImmichSearchResponse()
    }

    suspend fun searchAssets(query: String, page: Int = 1, size: Int = 50): ImmichSearchResponse {
        val body = buildJsonObject {
            put("originalFileName", query)
            put("type", "IMAGE")
            put("page", page)
            put("size", size)
        }
        val response = client.post(apiPath("/search/metadata")) { setBody(body) }
        return parseSearchResponse(response)
    }

    /** Get all images via search/metadata — reliable asset-object format */
    suspend fun getAllImages(page: Int = 1, size: Int = 100): ImmichSearchResponse {
        val body = buildJsonObject {
            put("type", "IMAGE")
            put("page", page)
            put("size", size)
        }
        val response = client.post(apiPath("/search/metadata")) { setBody(body) }
        return parseSearchResponse(response)
    }

    suspend fun getTimelineBuckets(size: String = "MONTH"): List<ImmichTimelineBucket> {
        val response = client.get(apiPath("/timeline/buckets")) {
            url { parameters.append("size", size) }
        }
        return response.body()
    }

    suspend fun getAssetThumbnail(assetId: String): ByteArray {
        val response = client.get(apiPath("/assets/$assetId/thumbnail"))
        if (!response.status.isSuccess()) {
            val err = response.bodyAsText().take(300)
            Log.w(TAG, "thumbnail failed: $err")
            throw IllegalStateException("Thumbnail HTTP ${response.status}: $err")
        }
        return response.bodyAsBytes()
    }

    suspend fun getAssetOriginal(assetId: String): ByteArray {
        return client.get(apiPath("/assets/$assetId/original")).bodyAsBytes()
    }

    fun thumbnailUrl(assetId: String): String = apiPath("/assets/$assetId/thumbnail")

    fun originalUrl(assetId: String): String = apiPath("/assets/$assetId/original")

    fun personThumbnailUrl(personId: String): String = apiPath("/people/$personId/thumbnail")

    suspend fun close() {
        client.close()
    }
}
