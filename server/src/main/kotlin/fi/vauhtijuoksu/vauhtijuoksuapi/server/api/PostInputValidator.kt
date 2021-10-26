package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model

interface PostInputValidator<T : Model> : InputValidator<T>
