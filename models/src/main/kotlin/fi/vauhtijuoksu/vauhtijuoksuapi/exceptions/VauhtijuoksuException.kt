package fi.vauhtijuoksu.vauhtijuoksuapi.exceptions

open class VauhtijuoksuException : Throwable {
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
}

class MissingEntityException(message: String) : VauhtijuoksuException(message)

class UserError(message: String) : VauhtijuoksuException(message)

class ServerError : VauhtijuoksuException {
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
}
