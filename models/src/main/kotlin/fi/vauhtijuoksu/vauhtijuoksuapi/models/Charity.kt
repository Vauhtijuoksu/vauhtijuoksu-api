package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.UUID

data class Charity(
    override val id: UUID,
    val name: String,
    val url: String?,
    val goal: Int?,
    val imgFilename: String?,
) : Model
