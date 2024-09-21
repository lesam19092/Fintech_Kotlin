import dsl.html
import dto.News
import service.KudoService
import java.time.LocalDate


suspend fun main(args: Array<String>) {


    val kudoService = KudoService()
    val newsList = kudoService.getNews(1000)
    val period = LocalDate.of(2022, 9, 1)..LocalDate.of(2024, 9, 15)
    val mostRatedNews = kudoService.getMostRatedNews(77, period, newsList)
    DSL(mostRatedNews)
    kudoService.saveNews(("save.csv"), mostRatedNews)

}

private fun DSL(mostRatedNews: List<News>) {
    val result = html {
        head {
            title { +"Result list" }
        }
        body {

            p {
                +"elements of list"
                ul {
                    for (newsItem in mostRatedNews) {
                        li {
                            +"${newsItem.title}"
                        }
                    }
                }
            }
        }
    }
    println(result)

}

