package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.UUID

data class WebsocketTemplate(
        override val id: UUID,
        val message: String,
): Model