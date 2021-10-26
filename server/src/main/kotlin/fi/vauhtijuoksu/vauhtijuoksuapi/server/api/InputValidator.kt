package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model

interface InputValidator<T : Model> {
    /**
     * Validate input T and and return string describing errors, if any
     */
    fun validate(input: T): String?
}
