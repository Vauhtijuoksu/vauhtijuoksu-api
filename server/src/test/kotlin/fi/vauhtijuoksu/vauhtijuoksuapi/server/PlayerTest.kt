package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players.ModifyPlayerRequest
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players.NewPlayerRequest
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players.PlayerResponse
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class PlayerTest : ServerTestBase() {
    @Test
    fun `get all players returns an empty list when there are no players`(testContext: VertxTestContext) {
        `when`(playerDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))
        client.get("/players").send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    assertEquals(JsonArray(), it.body().toJsonArray())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `add a player`(testContext: VertxTestContext) {
        val createdPlayer = Player(
            UUID.randomUUID(),
            "it's a me",
            "glukooosi",
            "hlupost#1234",
        )
        lateinit var createdId: UUID
        `when`(playerDatabase.add(any())).thenAnswer {
            val input: Player = it.getArgument(0)
            createdId = input.id
            return@thenAnswer Future.succeededFuture<Unit>()
        }

        client.post("/players")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(
                NewPlayerRequest(
                    createdPlayer.displayName,
                    createdPlayer.twitchChannel,
                    createdPlayer.discordNick,
                ),
            )
            .onFailure(testContext::failNow)
            .map {
                testContext.verify {
                    assertEquals(201, it.statusCode())
                    assertEquals(
                        JsonObject.mapFrom(PlayerResponse.fromPlayer(createdPlayer.copy(id = createdId))),
                        it.body().toJsonObject(),
                    )
                    testContext.completeNow()
                }
            }
    }

    @Test
    fun `modify a player`(testContext: VertxTestContext) {
        val id = UUID.randomUUID()

        val existingPlayer = Player(
            id,
            "Hlupost",
            "watch me senpai",
            "Glu koosi",
        )

        val newPlayer = existingPlayer.copy(displayName = "Glukoosi", twitchChannel = null, discordNick = null)

        `when`(playerDatabase.getById(id)).thenReturn(
            Future.succeededFuture(existingPlayer),
            Future.succeededFuture(newPlayer),
        )
        `when`(playerDatabase.update(newPlayer)).thenReturn(Future.succeededFuture())

        client.patch("/players/$id")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(
                ModifyPlayerRequest(
                    "Glukoosi",
                    null,
                    null,
                ),
            )
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(
                        JsonObject.mapFrom(PlayerResponse.fromPlayer(newPlayer)),
                        it.body().toJsonObject(),
                    )
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `remove a player`(testContext: VertxTestContext) {
        val id = UUID.randomUUID()
        `when`(playerDatabase.delete(any())).thenReturn(Future.succeededFuture())
        client.delete("/players/$id")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(204, it.statusCode())
                    verify(playerDatabase).delete(id)
                }
                testContext.completeNow()
            }
    }
}
