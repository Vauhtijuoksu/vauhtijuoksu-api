package fi.vauhtijuoksu.vauhtijuoksuapi.server

class DependencyInjectionConstants private constructor() {
    companion object {
        const val PUBLIC_CORS = "public_cors_handler"
        const val AUTHENTICATED_CORS = "authenticated_cors_handler"
    }
}
