package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableSingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableVauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Created
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Deleted
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Event
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Modified
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.WebSocketRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.WebsocketRouterForModels
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonArray
import io.vertx.kotlin.coroutines.coAwait
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.UUID

suspend fun consumeChannel(
    channel: Channel<String>,
    writeFun: (String) -> Future<Void>,
) {
    for (event in channel) {
        writeFun(event).coAwait()
    }
}

context(CoroutineScope)
private suspend fun <T> sendUpdates(
    coroutineDispatcher: CoroutineDispatcher,
    initialValue: suspend (() -> Pair<Long, String>),
    events: Flow<Event<T>>,
    interesting: ((Event<T>) -> Boolean),
    toApiResponse: ((T) -> String),
    write: ((String) -> Future<Void>),
) {
    val logger = KotlinLogging.logger {}

    logger.info { "Collecting from flow $events" }
    val channel = Channel<String>(Channel.BUFFERED, BufferOverflow.SUSPEND)
    var (lastSeen, initial) = initialValue()

    val firstBatchId = launch(coroutineDispatcher) {
        logger.info { "Sending initial value $initial" }
        write(initial).coAwait()
    }

    launch {
        firstBatchId.join()
        consumeChannel(channel, write)
    }

    events.collect { event ->
        logger.info { "Collecting event $event" }
        lastSeen =
            if (lastSeen > event.counter) { // An old event - update happened before fetching the data -- update lastSeen
                lastSeen
            } else if (lastSeen == event.counter.dec()) { // Next event -- send update and update lastSeen
                if (interesting(event)) {
                    if (channel.trySend(jacksonObjectMapper().writeValueAsString(event.map(toApiResponse))).isFailure) {
                        logger.warn { "Consumer couldn't keep up with the updates" }
                        channel.close()
                        cancel()
                    }
                }
                event.counter
            } else { // Got from the buffer an event newer than lastSeen + 1 -- updates were missed and websocket should be closed
                throw UserError("Missed updates on the websocket")
            }
    }
}

sealed class SingletonWebSocketRouter<T> @Inject constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val db: ObservableDatabase<T>,
    private val toApiResponse: ((T) -> String),
) : WebSocketRouter<T> {
    private suspend fun initialValue(): Pair<Long, String> {
        return when (db) {
            is ObservableSingletonDatabase -> {
                db.getAndCounter().let {
                    it.first to it.second.let(toApiResponse)
                        .let { initial -> jacksonObjectMapper().writeValueAsString(initial) }
                }
            }

            is ObservableVauhtijuoksuDatabase -> {
                db.getAllAndCounter().let {
                    it.first to it.second.map(toApiResponse)
                        .let { list -> jacksonObjectMapper().writeValueAsString(JsonArray(list)) }
                }
            }
        }
    }

    context(CoroutineScope)
    protected suspend fun <T> handle(
        initialValue: suspend (() -> Pair<Long, String>),
        flow: Flow<Event<T>>,
        interesting: (Event<T>) -> Boolean,
        toApiResponse: (T) -> String,
        ws: ServerWebSocket,
    ) {
        ws.accept()

        sendUpdates(
            coroutineDispatcher,
            initialValue,
            flow,
            interesting,
            toApiResponse,
        ) { msg: String -> ws.writeTextMessage(msg) }

        flow.onCompletion {
            ws.close()
            cancel()
        }

        ws.closeHandler {
            cancel()
        }

        ws.exceptionHandler {
            cancel()
        }
    }

    override suspend fun handler(): Handler<ServerWebSocket> {
        return Handler { ws ->
            CoroutineScope(coroutineDispatcher).launch {
                handle(::initialValue, db.flow, { true }, toApiResponse, ws)
            }
        }
    }
}

class ModelWebSocketRouter<T : Model> @Inject constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val db: ObservableVauhtijuoksuDatabase<T>,
    private val toApiResponse: ((T) -> String),
) : WebsocketRouterForModels<T>, SingletonWebSocketRouter<T>(coroutineDispatcher, db, toApiResponse) {
    override suspend fun handlerForId(id: UUID): Handler<ServerWebSocket> {
        return Handler { ws ->
            CoroutineScope(coroutineDispatcher).launch {
                handle(
                    suspend {
                        db.getAllAndCounter().let { it.first to it.second.first { t -> t.id == id }.let(toApiResponse) }
                    },
                    db.flow,
                    { event: Event<T> ->
                        when (event) {
                            is Created -> event.value.id == id
                            is Modified -> event.value.id == id
                            is Deleted -> event.id == id
                        }
                    },
                    toApiResponse,
                    ws,
                )
            }
        }
    }
}
