package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import java.util.UUID

interface VauhtijuoksuDatabase {
    /**
     * Get all gamedata objects
     */
    fun getGameData(): Future<List<GameData>>

    /**
     * Get gamedata by id
     *
     * @return Null if no such gamedata record exists
     */
    fun getGameDataById(id: UUID): Future<GameData?>

    fun addGameData(gd: GameData): Future<GameData>

    /**
     * Delete a gamedata record with given id
     *
     * @return Future<Boolean> indicating whether a record with given id existed, and was therefore deleted
     */
    fun deleteGameData(id: UUID): Future<Boolean>
}
