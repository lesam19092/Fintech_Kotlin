package service

import dto.ApiResponse
import dto.News
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class KudoService {

    private val logger = LoggerFactory.getLogger(::KudoService.javaClass)


    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val baseUrl =
        "https://kudago.com/public-api/v1.4/news/" +
                "?fields=id,title,slug,description,publication_date," +
                "favorites_count,comments_count&expand=" +
                "&text_format=text&actual_only=true"


    suspend fun getNews(count: Int = 50, page: Int = 1): List<News> {
        logger.info("Fetching news: count=$count, page=$page")

        val response = client.get(baseUrl) {
            parameter("page_size", count)
            parameter("location", "spb")
            parameter("order_by", "publication_date")
            parameter("page", page)
        }

        if (response.headers["Content-Type"]?.contains("application/json") == true) {
            val newsResponse: ApiResponse = response.body()
            logger.debug("Fetched ${newsResponse.results.size} news items")
            return newsResponse.results
        } else {
            logger.warn("Skipped non-JSON response")
            return emptyList()
        }
    }

    fun getMostRatedNews(count: Int, period: ClosedRange<LocalDate>, list: List<News>): List<News> {
        logger.info("началсь сортировка новостей")
        return list.filter { localDate(it) in period }
            .sortedByDescending { it.rating }
            .take(count)
    }


    private fun localDate(news: News): LocalDate {
        val timestampInMillis: Long = news.publicationDate * 1000
        val publicationDate =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampInMillis), ZoneId.systemDefault()).toLocalDate()
        return publicationDate
    }
}


