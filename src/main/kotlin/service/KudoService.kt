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
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.isReadable
import kotlin.math.exp

class KudoService {

    private val logger = LoggerFactory.getLogger(::KudoService.javaClass)


    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl =
        "https://kudago.com/public-api/v1.4/news/?fields=id,title,slug,description,publication_date,favorites_count,comments_count&expand=&order_by=&text_format=text&ids=&location=&actual_only=true"

    suspend fun getNews(count: Int = 100): List<News> {
        try {
            val response: ApiResponse = client.use {
                it.get(baseUrl) {
                    parameter("page_size", count)
                    parameter("order_by", "-publication_date")
                    parameter("location", "spb")
                }
                    .body()
            }
            logger.info("информация получена!")
            return response.results!!.map { news ->
                news.copy(rating = calculateRating(news.favoritesCount ?: 0, news.commentsCount ?: 0))
            }
        } catch (e: Exception) {
            logger.error("произошла проблема с запросом")
            return listOf()
        }
    }

    fun getMostRatedNews(count: Int, period: ClosedRange<LocalDate>, list: List<News>): List<News> {
        logger.info("началсь сортировка новостей")
        return list.filter { localDate(it) in period }
            .sortedByDescending { it.rating }
            .take(count)
    }


    fun saveNews(path: String, news: Collection<News>) {
        try {
            val filePath = Paths.get(path)

            if (!Files.exists(filePath) || !filePath.isReadable()) {
                logger.warn("Ошибка при работе с файлом: $filePath. Файл либо отсутствует, либо не является читаемым.")
                return
            }
            val csv = news.joinToString("\n") { news ->
                "\"${news.id}\",\"${news.title}\",\"${news.publicationDate}\",\"${news.slug}\",\"${news.place}\",\"${news.description}\",\"${news.siteUrl}\",\"${news.favoritesCount}\",\"${news.commentsCount}\",\"${news.rating}\""
            }
            Files.write(filePath, csv.toByteArray())
            logger.info("Записано ${news.size} новостей")
        } catch (e: IOException) {
            logger.error("Error writing to file: $path", e)
        }

    }

    private fun localDate(news: News): LocalDate {
        val timestampInMillis: Long = news.publicationDate * 1000
        val publicationDate =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampInMillis), ZoneId.systemDefault()).toLocalDate()
        return publicationDate
    }

    private fun calculateRating(favoritesCount: Int, commentsCount: Int): Double =
        1 / (1 + exp(-(favoritesCount.toDouble() / (commentsCount + 1))))
}


