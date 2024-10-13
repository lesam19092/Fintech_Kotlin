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

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Main")
    val startTime = System.currentTimeMillis()
    logger.info("Program started")

    val kudaGoClient = KudoService()
    val fileClient = FileClient()
    val countOfThreads: Int = 16
    val executor = Executors.newFixedThreadPool(countOfThreads)

    fileClient.clearFile("src/main/resources/save.csv")

    val channel = Channel<List<News>>()
    val semaphore = Semaphore(2)

    val scope = CoroutineScope(Dispatchers.Default)
    val tasks = (1..countOfThreads).map { i ->
        scope.launch {
            try {
                for (page in 1..20) {
                    if (page % countOfThreads == i - 1) {
                        if (semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                            try {
                                val requestContent = kudaGoClient.getNews(page = page)
                                if (requestContent.isNotEmpty()) channel.send(requestContent)
                            } finally {
                                semaphore.release()
                            }
                        } else {
                            logger.warn("API access locked for thread $i, retrying in 30 seconds...")
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