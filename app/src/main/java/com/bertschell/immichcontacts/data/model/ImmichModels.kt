package com.bertschell.immichcontacts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ImmichPerson(
    val id: String,
    val name: String = "",
    val birthDate: String? = null,
    val thumbnailPath: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
)

@Serializable
data class ImmichPeopleResponse(
    val people: List<ImmichPerson> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ImmichAsset(
    val id: String,
    val type: String = "IMAGE",
    val originalFileName: String = "",
    val originalMimeType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val fileCreatedAt: String = "",
    val localDateTime: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val people: List<ImmichAssetPerson> = emptyList(),
)

@Serializable
data class ImmichAssetPerson(
    val id: String,
    val name: String = "",
)

@Serializable
data class ImmichSearchResponse(
    val assets: ImmichSearchAssets = ImmichSearchAssets(),
)

@Serializable
data class ImmichSearchAssets(
    val total: Int = 0,
    val count: Int = 0,
    val items: List<ImmichAsset> = emptyList(),
)

@Serializable
data class ImmichTimelineBucket(
    val timeBucket: String,
    val count: Int,
)

@Serializable
data class ImmichUser(
    val id: String,
    val email: String,
    val name: String,
)

@Serializable
data class ImmichServerVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
)
