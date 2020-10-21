package ru.mipt.npm.ktor.sse

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * The data class representing a SSE Event that will be sent to the client.
 */
public data class SseEvent(val data: String, val event: String? = null, val id: String? = null)

public suspend fun ByteWriteChannel.writeSseFlow(events: Flow<SseEvent>): Unit = events.collect { event ->
    if (event.id != null) {
        writeStringUtf8("id: ${event.id}\n")
    }
    if (event.event != null) {
        writeStringUtf8("event: ${event.event}\n")
    }
    for (dataLine in event.data.lines()) {
        writeStringUtf8("data: $dataLine\n")
    }
    writeStringUtf8("\n")
    flush()
}

public fun HttpClient.readSse(address: String, block: suspend (SseEvent) -> Unit): Job = launch {
    var reconnectDelay: Long = 3000
    var lastEventId: String? = null
    while (isActive) {
        get<HttpStatement>(address) {
            this.header("Accept-Encoding", "")
            this.header("Accept", "text/event-stream")
            this.header("Cache-Control", "no-cache")

            if (lastEventId != null) {
                header("Last-Event-Id", lastEventId);
            }
        }.execute { response: HttpResponse ->
            // Response is not downloaded here.
            val channel = response.receive<ByteReadChannel>()
            while (isActive) {
                //val lines = ArrayList<String>()
                val builder = StringBuilder()
                var id: String? = null
                var event: String? = null
                var retryValue: String? = null
                //read lines until blank line or the end of stream

                do {
                    val line = channel.readUTF8Line()
                    if (line != null && line.isNotBlank()) {
                        val colonIndex = line.indexOf(": ")
                        val key = line.substring(0 until colonIndex)
                        val value = line.substring(colonIndex)
                        when (key) {
                            "id" -> {
                                id = value
                                lastEventId = value
                            }
                            "event" -> event = value
                            "data" -> builder.append(value)
                            "retry" -> retryValue = value
                            else -> error("Unrecognized event-stream key $key")
                        }
                    }
                } while (line?.isBlank() != true)

                if (retryValue != null) {
                    val delayTime = retryValue.toLong()
                    check(delayTime>0){"SSE reconnect value must be positive"}
                    reconnectDelay = delayTime
                } else if (builder.isNotBlank()) {
                    block(SseEvent(builder.toString(), event, id))
                }
            }
        }
        delay(reconnectDelay)
    }
}

