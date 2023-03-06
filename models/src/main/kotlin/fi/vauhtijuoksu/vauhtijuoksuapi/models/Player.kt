package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.UUID

data class Player(
    override val id: UUID,
    val displayName: String,
    val twitchChannel: String?,
    val discordNick: String?,
) : Model
