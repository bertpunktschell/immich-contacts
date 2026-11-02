package com.bertschell.immichcontacts.data

import android.content.Context
import coil.request.ImageRequest

/**
 * Helper for building Coil ImageRequests with Immich API key auth.
 * Immich requires x-api-key header even for thumbnails.
 */
object ImmichImageLoader {

    /**
     * Build an authenticated ImageRequest for an Immich asset thumbnail.
     */
    fun thumbnailRequest(
        context: Context,
        serverUrl: String,
        apiKey: String,
        assetId: String,
        size: Int = 256
    ): ImageRequest {
        val url = "${serverUrl.trimEnd('/')}/api/assets/$assetId/thumbnail?size=$size"
        return ImageRequest.Builder(context)
            .data(url)
            .addHeader("x-api-key", apiKey)
            .crossfade(true)
            .build()
    }

    /**
     * Build an authenticated ImageRequest for an Immich person thumbnail.
     */
    fun personThumbnailRequest(
        context: Context,
        serverUrl: String,
        apiKey: String,
        personId: String
    ): ImageRequest {
        val url = "${serverUrl.trimEnd('/')}/api/people/$personId/thumbnail"
        return ImageRequest.Builder(context)
            .data(url)
            .addHeader("x-api-key", apiKey)
            .crossfade(true)
            .build()
    }
}