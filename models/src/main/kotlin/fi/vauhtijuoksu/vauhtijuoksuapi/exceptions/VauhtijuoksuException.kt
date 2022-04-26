package fi.vauhtijuoksu.vauhtijuoksuapi.exceptions

sealed class VauhtijuoksuException : Throwable {
    constructor(message: String?) : super(message)
    constructor(cause: Throwable) : super(cause)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

class MissingEntityException(message: String) : VauhtijuoksuException(message)

class UserError(message: String?, cause: Throwable?) : VauhtijuoksuException(message, cause) {
    constructor(message: String?) : this(message, null)
}

class ServerError : VauhtijuoksuException {
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
}
