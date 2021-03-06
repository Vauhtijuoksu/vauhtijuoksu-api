package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model

fun interface InputValidator<T : Model> {
    /**
     * Validate input T and return string describing errors, if any
     */
    fun validate(input: T): String?
}
