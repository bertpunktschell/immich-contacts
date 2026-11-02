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
import kotlinx.serialization.json.put
import com.bertschell.immichcontacts.data.model.*

class ImmichApi(private val baseUrl: String, inApiKey: String) {

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
        val response = client.post(apiPath("/search/metadata")) {
            setBody(buildJsonObject {
                put("personIds", JsonArray(listOf(JsonPrimitive(personId))))
                put("type", "IMAGE")
                put("page", page)
                put("size", size)
            })
        }
        Log.d(TAG, "getAssetsForPerson($personId) status=${response.status}")
        return response.body()
    }

    suspend fun searchAssets(query: String, page: Int = 1, size: Int = 50): ImmichSearchResponse {
        return client.post(apiPath("/search/metadata")) {
            setBody(buildJsonObject {
                put("originalFileName", query)
                put("type", "IMAGE")
                put("page", page)
                put("size", size)
            })
        }.body()
    }

    /** Get all images via search/metadata — reliable asset-object format */
    suspend fun getAllImages(page: Int = 1, size: Int = 100): ImmichSearchResponse {
        val response = client.post(apiPath("/search/metadata")) {
            setBody(buildJsonObject {
                put("type", "IMAGE")
                put("page", page)
                put("size", size)
            })
        }
        Log.d(TAG, "getAllImages page=$page status=${response.status}")
        return response.body()
    }

    suspend fun getTimelineBuckets(size: String = "MONTH"): List<ImmichTimelineBucket> {
        val response = client.get(apiPath("/timeline/buckets")) {
            url { parameters.append("size", size) }
        }
        Log.d(TAG, "getTimelineBuckets status=${response.status}")
        return response.body()
    }

    suspend fun getAssetThumbnail(assetId: String): ByteArray {
        return client.get(apiPath("/assets/$assetId/thumbnail")).bodyAsBytes()
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
