import dsl.html
import dto.News
import service.KudoService
import java.time.LocalDate


suspend fun main(args: Array<String>) {


    val kudoService = KudoService()
    val newsList = kudoService.getNews(75)
    val period = LocalDate.of(2023, 9, 1)..LocalDate.of(2024, 9, 15)
    val mostRatedNews = kudoService.getMostRatedNews(20, period, newsList)
    DSL(mostRatedNews)
    kudoService.saveNews(("C:\\Users\\danil\\Desktop\\Fintech_kotlin\\src\\main\\resources\\save.csv"), mostRatedNews)

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

