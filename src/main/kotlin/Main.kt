import client.FileClient
import dto.News
import io.ktor.client.call.NoTransformationFoundException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import service.KudoService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val kudaGoClient = KudoService()
    val fileClient = FileClient()
    val countOfThreads: Int = 10
    val executor = Executors.newFixedThreadPool(countOfThreads)

    fileClient.clearFile("src/main/resources/save.csv")

    val channel = Channel<List<News>>()

    val scope = CoroutineScope(Dispatchers.Default)
    val tasks = (1..countOfThreads).map { i ->
        scope.launch {
            try {
                for (page in 1..20) {
                    if (page % countOfThreads == i - 1) {
                        val requestContent = kudaGoClient.getNews(page = page)

                        if (!requestContent.isEmpty()) channel.send(requestContent)
                    }
                }
            } catch (e: NoTransformationFoundException) {
                println("Error: ${e.message}")
            }
        }
    }

    val readerJob = scope.launch {
        while (true) {
            val result = channel.receiveCatching().getOrNull()
            if (result != null) {
                fileClient.saveNews("src/main/resources/save.csv", result)
            } else {
                break
            }
        }
    }

    runBlocking {
        tasks.forEach { it.join() }
        channel.close()
        readerJob.join()
    }

    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.HOURS)
    println("Finished all threads")
}