package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableVauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Created
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Deleted
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Event
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Modified
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Singleton
class ObservableDatabaseImpl<T : Model>
@Inject
constructor(
    private val vertx: Vertx,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val db: InternalDatabase<T>,
    private val counter: AtomicLong = AtomicLong(0L),
) : ObservableVauhtijuoksuDatabase<T>, VauhtijuoksuDatabase<T> by db {
    private val lockName = "lock-${db::class.simpleName}"

    override suspend fun getAllAndCounter(): Pair<Long, List<T>> {
        return 0L to db.getAll().coAwait()
    }

    override val flow: MutableSharedFlow<Event<T>> = MutableSharedFlow()

    private val logger = KotlinLogging.logger {}

    private fun emit(event: Event<T>) = GlobalScope.launch(coroutineDispatcher) {
        logger.info { "Emiting an event $event to flow $flow" }
        flow.emit(event)
    }

    override fun add(record: T): Future<Unit> {
        return vertx.sharedData().getLock(lockName).compose { lock ->
            db.add(record).map { lock }
        }.map { lock ->
            val c = counter.incrementAndGet()
            emit(Created(record, c))
            lock.release()
        }
    }

    override fun delete(id: UUID): Future<Unit> {
        return vertx.sharedData().getLock(lockName).compose { lock ->
            db.delete(id).map { lock }
        }.map { lock ->
            val c = counter.incrementAndGet()
            emit(Deleted(id, c))
            lock.release()
        }
    }

    override fun update(record: T): Future<Unit> {
        logger.info { "Updating record $record" }
        return vertx.sharedData().getLock(lockName).compose { lock ->
            db.update(record).map { lock }
        }.map { lock ->
            logger.info { "Udate done. Sending event" }
            val c = counter.incrementAndGet()
            emit(Modified(record, c))
            lock.release()
            logger.info { "Done with the update" }
        }
    }
}
