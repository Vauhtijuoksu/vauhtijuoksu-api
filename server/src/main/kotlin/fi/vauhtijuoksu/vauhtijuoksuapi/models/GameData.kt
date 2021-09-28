package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.net.URL
import java.util.Date

data class GameData(
    val game: String,
    val player: String,
    val start_time: Date,
    val end_time: Date,
    val category: String,
    val device: String,
    val published: String,
    val vod_link: URL?,
    val img_filename: String,
    val player_twitch: String,
)
