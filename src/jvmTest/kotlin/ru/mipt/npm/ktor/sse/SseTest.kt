package ru.mipt.npm.ktor.sse

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.response.cacheControl
import io.ktor.response.respondBytesWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@OptIn(KtorExperimentalAPI::class)
suspend fun ApplicationCall.respondSse(events: Flow<SseEvent>) {
    response.cacheControl(CacheControl.NoCache(null))
    respondBytesWriter(contentType = ContentType.Text.EventStream) {
        writeSseFlow(events)
    }
}

class SseTest {
    @OptIn(KtorExperimentalAPI::class)
    @Test
    fun testSseIntegration() {
        runBlocking(Dispatchers.Default) {
            val server = embeddedServer(CIO, 12080) {
                routing {
                    get("/") {
                        val flow = flow {
                            repeat(5) {
                                delay(300)
                                emit(it)
                            }
                        }.map {
                            SseEvent(data = it.toString(), id = it.toString())
                        }
                        call.respondSse(flow)
                    }
                }
            }
            server.start(wait = false)
            delay(1000)
            val client = HttpClient(io.ktor.client.engine.cio.CIO)
            client.readSse("http://localhost:12080") {
                println(it)
            }
            delay(2000)
            println("Closing the client after waiting")
            client.close()
            server.stop(1000, 1000)
        }
    }
}