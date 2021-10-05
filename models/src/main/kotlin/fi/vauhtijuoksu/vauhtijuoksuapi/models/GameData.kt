package fi.vauhtijuoksu.vauhtijuoksuapi.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URL
import java.util.Date
import java.util.UUID

data class GameData(
    val id: UUID,
    val game: String,
    val player: String,
    @JsonProperty("start_time")
    val startTime: Date,
    @JsonProperty("end_time")
    val endTime: Date,
    val category: String,
    val device: String,
    val published: String,
    @JsonProperty("vod_link")
    val vodLink: URL?,
    @JsonProperty("img_filename")
    val imgFilename: String,
    @JsonProperty("player_twitch")
    val playerTwitch: String,
)
