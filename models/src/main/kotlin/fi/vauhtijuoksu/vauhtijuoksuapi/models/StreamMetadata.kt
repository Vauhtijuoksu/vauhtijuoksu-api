package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.UUID

data class StreamMetadata(
    val donationGoal: Int?,
    val currentGameId: UUID?,
    val donateBarInfo: List<String>,
    val counters: List<Int>,
    val heartRates: List<Int>,
    var timers: List<Timer>,
    var nowPlaying: String?,
)
