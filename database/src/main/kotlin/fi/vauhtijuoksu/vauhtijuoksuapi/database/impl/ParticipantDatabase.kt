package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.ParticipantDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import jakarta.inject.Inject
import mu.KotlinLogging
import java.util.UUID

class ParticipantDatabase
    @Inject
    constructor(
        private val pool: PgPool,
        configuration: DatabaseConfiguration,
    ) : BaseDatabase(configuration),
        VauhtijuoksuDatabase<Participant> {
        private val logger = KotlinLogging.logger {}

        @Suppress("MaxLineLength")
        private val getAll =
            pool.preparedQuery(
                """
            SELECT 
                p.id,
                p.display_name,
                COALESCE(
                    json_agg(
                        json_build_object(
                            'platform', s.platform,
                            'username', s.username
                        )
                    ) FILTER (WHERE s.participant_id IS NOT NULL), 
                    '[]'
                ) AS social_medias
            FROM public.participants p
            LEFT JOIN public.social_medias s 
            ON p.id = s.participant_id
            GROUP BY p.id, p.display_name
            ORDER BY p.display_name;
            """,
            )

        @Suppress("MaxLineLength")
        private val getOne =
            pool.preparedQuery(
                """
            SELECT 
                p.id,
                p.display_name,
                COALESCE(
                    json_agg(
                        json_build_object(
                            'platform', s.platform,
                            'username', s.username
                        )
                    ) FILTER (WHERE s.participant_id IS NOT NULL),
                    '[]'
                ) AS social_medias
            FROM public.participants p
            LEFT JOIN public.social_medias s 
            ON p.id = s.participant_id
            WHERE p.id = $1
            GROUP BY p.id, p.display_name;
            """,
            )
        private val delete = pool.preparedQuery("DELETE from participants where id = $1;")

        override fun getAll(): Future<List<Participant>> =
            getAll
                .execute()
                .map { it.map(mapperToType<ParticipantDbModel>()) }
                .map { it.map(ParticipantDbModel::toParticipant) }
                .orServerError()

        override fun getById(id: UUID): Future<Participant> {
            return getOne
                .execute(Tuple.of(id))
                .map {
                    return@map it.first()
                }.recover {
                    if (it is NoSuchElementException) {
                        throw MissingEntityException("No Participant with id $id")
                    } else {
                        throw it
                    }
                }.map(mapperToType<ParticipantDbModel>())
                .map(ParticipantDbModel::toParticipant)
        }

        override fun delete(id: UUID): Future<Unit> =
            delete
                .execute(Tuple.of(id))
                .expectOneChangedRow()

        override fun add(record: Participant): Future<Unit> {
            return pool.withTransaction { client ->
                client
                    .preparedQuery("""INSERT INTO participants values ($1, $2)""")
                    .execute(
                        ParticipantDbModel.fromParticipant(record).let {
                            Tuple.of(
                                it.id,
                                it.display_name,
                            )
                        },
                    ).flatMap {
                        return@flatMap if (record.socialMedias.isEmpty()) {
                            Future.succeededFuture()
                        } else {
                            client
                                .preparedQuery("INSERT INTO social_medias VALUES ($1, $2, $3)")
                                .executeBatch(
                                    record.socialMedias.map {
                                        Tuple.of(
                                            record.id,
                                            it.platform,
                                            it.username,
                                        )
                                    },
                                )
                        }
                    }.recover {
                        throw ServerError("Failed to insert $record because of ${it.message}")
                    }.onSuccess {
                        logger.debug { "Inserted participant $record" }
                    }.mapEmpty()
            }
        }

        override fun update(record: Participant): Future<Unit> {
            return pool.withTransaction { client ->
                client
                    .preparedQuery(
                        """UPDATE participants SET  
                    display_name = $1
                    WHERE id = $2""",
                    ).execute(
                        ParticipantDbModel.fromParticipant(record).let {
                            Tuple.of(
                                it.display_name,
                                it.id,
                            )
                        },
                    ).expectOneChangedRow()
                    .flatMap {
                        client
                            .preparedQuery("DELETE FROM social_medias where participant_id = $1")
                            .execute(Tuple.of(record.id))
                    }.flatMap {
                        return@flatMap if (record.socialMedias.isEmpty()) {
                            Future.succeededFuture()
                        } else {
                            client
                                .preparedQuery("INSERT INTO social_medias VALUES ($1, $2, $3)")
                                .executeBatch(
                                    record.socialMedias.map {
                                        Tuple.of(
                                            record.id,
                                            it.platform,
                                            it.username,
                                        )
                                    },
                                )
                        }
                    }.mapEmpty()
            }
        }
    }
