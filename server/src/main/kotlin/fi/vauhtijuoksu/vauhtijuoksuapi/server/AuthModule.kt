package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.OAuthConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.discord.DiscordUserHandler
import io.vertx.core.Vertx
import io.vertx.ext.auth.ChainAuth
import io.vertx.ext.auth.authentication.AuthenticationProvider
import io.vertx.ext.auth.authorization.RoleBasedAuthorization
import io.vertx.ext.auth.htpasswd.HtpasswdAuth
import io.vertx.ext.auth.htpasswd.HtpasswdAuthOptions
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.auth.oauth2.OAuth2Options
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.BasicAuthHandler
import io.vertx.ext.web.handler.ChainAuthHandler
import io.vertx.ext.web.handler.OAuth2AuthHandler

class AuthModule : AbstractModule() {
    @Provides
    @Singleton
    fun htpasswd(vertx: Vertx, conf: ServerConfiguration): HtpasswdAuth =
        HtpasswdAuth.create(vertx, HtpasswdAuthOptions().setHtpasswdFile(conf.htpasswdFileLocation))

    @Provides
    @Singleton
    fun basicAuthHandler(htpasswdAuth: HtpasswdAuth): BasicAuthHandler = BasicAuthHandler.create(htpasswdAuth)

    @Provides
    @Singleton
    fun oAuthOptions(config: OAuthConfiguration): OAuth2Options = OAuth2Options().apply {
        clientId = config.clientId
        clientSecret = config.clientSecret
        site = config.baseAuthorizationUrl
        authorizationPath = config.authorizationPath
        tokenPath = config.tokenPath
    }

    @Provides
    @Singleton
    fun oAuth(vertx: Vertx, oAuth2Options: OAuth2Options): OAuth2Auth = OAuth2Auth.create(vertx, oAuth2Options)

    /**
     * OAuth2 handler for Discord
     *
     * Intentionally not @Singleton. When used in login flow there's a need for callback, i.e. unauthenticated users are
     * authenticated instead of rejected. In the ChainAuth, that is used to protect api endpoints, we'll just reject
     * unauthenticated users because non-GET requests cannot be redirected.
     */
    @Provides
    fun oAuthHandler(
        config: OAuthConfiguration,
        oAuth2Auth: OAuth2Auth,
        vertx: Vertx,
    ): OAuth2AuthHandler =
        OAuth2AuthHandler.create(vertx, oAuth2Auth, config.callbackUrl)
            .withScopes(config.scopes)

    @Provides
    @Singleton
    fun anyAuth(htpasswdAuth: HtpasswdAuth, oAuth2Auth: OAuth2Auth): AuthenticationProvider =
        ChainAuth.any().add(htpasswdAuth).add(oAuth2Auth)

    @Provides
    @Singleton
    fun anyAuthHandler(basicAuth: BasicAuthHandler, oAuth: OAuth2AuthHandler): AuthenticationHandler =
        ChainAuthHandler.any()
            .add(basicAuth)
            .add(oAuth)

    @Provides
    @Singleton
    fun admin(discord: DiscordUserHandler, basic: BasicAuthorizationProvider): AuthorizationHandler =
        AuthorizationHandler.create(RoleBasedAuthorization.create(Roles.ADMIN.name))
            .addAuthorizationProvider(discord)
            .addAuthorizationProvider(basic)
}
