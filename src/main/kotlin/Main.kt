import client.FileClient
import dto.News
import io.ktor.client.call.NoTransformationFoundException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import service.KudoService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

val logger = LoggerFactory.getLogger("Main")
val kudaGoClient = KudoService()
val fileClient = FileClient()


fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()
    logger.info("Program started")
    fileClient.clearFile("src/main/resources/save.csv")


    val countOfThreads: Int = System.getenv("countOfThreads")?.toInt() ?: 1
    val executor = Executors.newFixedThreadPool(countOfThreads)

    val maxConcurrentRequests: Int = System.getenv("maxConcurrentRequests")?.toInt() ?: 1
    val semaphore = Semaphore(maxConcurrentRequests)



    val channel = Channel<List<News>>()

    val scope = CoroutineScope(Dispatchers.Default)
    val tasks = (1..countOfThreads).map { i ->
        scope.launch {
            try {
                for (page in 1..20) {
                    if (page % countOfThreads == i - 1) {
                        if (semaphore.tryAcquire()) {
                            try {
                                val requestContent = kudaGoClient.getNews(page = page)
                                if (requestContent.isNotEmpty()) channel.send(requestContent)
                            } finally {
                                semaphore.release()
                            }
                        } else {
                            logger.warn("API access locked for thread $i, retrying in 10 seconds...")
                            delay(10000)
                        }
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
    logger.info("Finished all threads")
    val endTime = System.currentTimeMillis()
    logger.info("Total execution time: ${endTime - startTime} ms")
}