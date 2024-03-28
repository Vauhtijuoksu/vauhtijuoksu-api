package fi.vauhtijuoksu.vauhtijuoksuapi.server

class ApiConstants private constructor() {
    companion object {
        const val OK = 200
        const val CREATED = 201
        const val NO_CONTENT = 204
        const val BAD_REQUEST = 400
        const val NOT_FOUND = 404
        const val METHOD_NOT_ALLOWED = 405
        const val INTERNAL_SERVER_ERROR = 500
        val USER_ERROR_CODES = 400..<500
    }
}
