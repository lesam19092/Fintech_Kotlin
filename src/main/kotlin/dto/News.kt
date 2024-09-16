package dto

import dto.serializer.URISerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class News(


    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("publication_date") val publicationDate: Long,
    @SerialName("slug") val slug: String? = null,
    @SerialName("place") val place: String? = null,
    @SerialName("description") val description: String? = null,
    @Serializable(with = URISerializer::class)
    @SerialName("siteUrl") val siteUrl: URI? = null,
    @SerialName("favorites_count") val favoritesCount: Int? = null,
    @SerialName("comments_count") val commentsCount: Int? = null,
    val rating: Double? = null


)



