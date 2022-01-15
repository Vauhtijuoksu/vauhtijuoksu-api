package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model

fun interface PostInputValidator<T : Model> : InputValidator<T>
