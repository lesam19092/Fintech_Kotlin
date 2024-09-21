package service


import dto.News
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

class KudoServiceTest {

    val service: KudoService = KudoService()


    @Test
    fun `should return list of 20 news on successful request`() = runBlocking {

        val result = service.getNews(20)
        assertEquals(result.size, 20)
    }

    @Test
    fun `should return list of 1 news on successful request`() = runBlocking {

        val result = service.getNews(1)
        assertAll(
            {
                assertEquals(result.get(0) is News, true)
                assertEquals(result.size, 1)
            }
        )
    }

    @Test
    fun `should return list of 100 news on successful request`() = runBlocking {
        val result = service.getNews()
        assertEquals(result.size, 100)
    }

    @Test
    fun `should sort news by rating and return empty list`() {
        val newsList = listOf(
            News(1, "News1", 1684275200000),
            News(2, "News2", 1684275200000),
            News(3, "News3", 1684275200000)
        )

        val period = LocalDate.of(2023, 5, 15)..LocalDate.of(2023, 5, 16)

        val result = service.getMostRatedNews(2, period, newsList)

        assertEquals(0, result.size)

    }


    @Test
    fun `should sort news by rating and return specified count`() {
        val newsList = listOf(
            News(1, "News1", 1684275200).copy(favoritesCount = 1).copy(commentsCount = 2),
            News(2, "News2", 1684275200).copy(favoritesCount = 10).copy(commentsCount = 27),
            News(3, "News3", 1684275200).copy(favoritesCount = 0).copy(commentsCount = 0),

            )
        val period = LocalDate.of(2023, 5, 15)..LocalDate.of(2023, 5, 18)

        val result = service.getMostRatedNews(2, period, newsList)


        assertAll(
            {
                assertEquals(2, result.size)
                assertEquals(0.5883486248147178, result[0].rating)
                assertEquals(0.5825702064623147, result[1].rating)
            }
        )

    }


    @Test
    fun `should save news to file`() {
        val newsList = listOf(
            News(1, "News1", 1684275200000, null, null, null, null, 1, 2)
        )
        val tempFile = File.createTempFile("news", ".csv")
        val path = tempFile.absolutePath

        service.saveNews(path, newsList)

        val content = Files.readString(Paths.get(path))
        assertEquals(
            "\"1\",\"News1\",\"1684275200000\",\"null\",\"null\",\"null\",\"null\",\"1\",\"2\",\"0.5825702064623147\"",
            content
        )
        tempFile.delete()
    }
}