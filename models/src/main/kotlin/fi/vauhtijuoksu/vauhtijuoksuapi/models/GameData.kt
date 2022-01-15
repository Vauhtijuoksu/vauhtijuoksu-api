package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.net.URL
import java.util.Date
import java.util.UUID

data class GameData(
    override val id: UUID,
    val game: String,
    val player: String,
    val startTime: Date,
    val endTime: Date,
    val category: String,
    val device: String,
    val published: String,
    val vodLink: URL?,
    val imgFilename: String?,
    val playerTwitch: String?,
) : Model
