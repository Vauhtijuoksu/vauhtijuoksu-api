package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants.NewParticipantRequest
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants.NewSocialMediaRequest
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants.ParticipantResponse
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class ParticipantTest : ServerTestBase() {
    @Test
    fun `get all participants returns an empty list when there are no participants`() = runTest {
        `when`(participantDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))
        client.get("/participants").send()
            .coAwait()
            .let {
                assertEquals(200, it.statusCode())
                assertEquals(JsonArray(), it.body().toJsonArray())
            }
    }

    @Test
    fun `add a participant`() = runTest {
        val createdParticipant = Participant(
            UUID.randomUUID(),
            "it's a me",
            listOf(
                SocialMedia.twitch("glukoosi"),
                SocialMedia.discord("hlupost#1234"),
            ),
        )
        lateinit var createdId: UUID
        `when`(participantDatabase.add(any())).thenAnswer {
            val input: Participant = it.getArgument(0)
            createdId = input.id
            return@thenAnswer Future.succeededFuture<Unit>()
        }

        client.post("/participants")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(
                NewParticipantRequest(
                    createdParticipant.displayName,
                    createdParticipant.socialMedias.map {
                        NewSocialMediaRequest(
                            it.platform.name,
                            it.username,
                        )
                    },
                ),
            )
            .coAwait()
            .let {
                assertEquals(201, it.statusCode())
                assertEquals(
                    JsonObject.mapFrom(ParticipantResponse.fromParticipant(createdParticipant.copy(id = createdId))),
                    it.body().toJsonObject(),
                )
            }
    }

    @Test
    fun `modify a participant`() = runTest {
        val id = UUID.randomUUID()

        val existingParticipant = Participant(
            id,
            "Hlupost",
            listOf(
                SocialMedia.twitch("watch me senpai"),
                SocialMedia.discord("Glu koosi"),
            ),
        )

        val newPlayer = existingParticipant.copy(displayName = "Glukoosi", socialMedias = listOf())

        `when`(participantDatabase.getById(id)).thenReturn(
            Future.succeededFuture(existingParticipant),
            Future.succeededFuture(newPlayer),
        )
        `when`(participantDatabase.update(newPlayer)).thenReturn(Future.succeededFuture())

        client.patch("/participants/$id")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(
                NewParticipantRequest(
                    "Glukoosi",
                    listOf(),
                ),
            )
            .coAwait()
            .let {
                assertEquals(
                    JsonObject.mapFrom(ParticipantResponse.fromParticipant(newPlayer)),
                    it.body().toJsonObject(),
                )
            }
    }

    @Test
    fun `remove a participant`() = runTest {
        val id = UUID.randomUUID()
        `when`(participantDatabase.delete(any())).thenReturn(Future.succeededFuture())
        client.delete("/participants/$id")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .coAwait()
            .let {
                assertEquals(204, it.statusCode())
                verify(participantDatabase).delete(id)
            }
    }
}
