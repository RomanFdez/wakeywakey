package com.sierraespada.wakeywakey.windows.calendar

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import java.net.InetSocketAddress

/**
 * Servidor HTTP mínimo que escucha en localhost:PORT/callback y captura el
 * parámetro ?code= del redirect OAuth.
 *
 * Ciclo de vida:
 *  1. [start] — lanza el servidor en background
 *  2. [awaitCode] — suspende hasta que el browser redirige con ?code=
 *  3. [stop] — cierra el servidor
 */
class OAuthCallbackServer(private val port: Int) {

    private var server: HttpServer? = null
    private val deferred = CompletableDeferred<Result<String>>()

    val redirectUri: String get() = "http://localhost:$port/callback"

    fun start(): OAuthCallbackServer {
        server = HttpServer.create(InetSocketAddress("localhost", port), 0).apply {
            createContext("/callback") { exchange ->
                val params = exchange.requestURI.query
                    ?.split("&")
                    ?.associate { p -> p.split("=", limit = 2).let { it[0] to (it.getOrNull(1) ?: "") } }
                    ?: emptyMap()

                val code  = params["code"]
                val error = params["error"]

                val (status, html) = when {
                    code  != null -> {
                        deferred.complete(Result.success(code))
                        200 to SUCCESS_HTML
                    }
                    error != null -> {
                        deferred.complete(Result.failure(Exception("OAuth error: $error")))
                        400 to ERROR_HTML
                    }
                    else -> {
                        deferred.complete(Result.failure(Exception("No code in callback")))
                        400 to ERROR_HTML
                    }
                }

                val bytes = html.toByteArray()
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            executor = null   // use default single-thread executor
            start()
        }
        return this
    }

    suspend fun awaitCode(): String = deferred.await().getOrThrow()

    fun stop() {
        server?.stop(0)
        server = null
    }

    companion object {
        private val SUCCESS_HTML = """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8">
            <title>WakeyWakey — Connected!</title>
            <style>
              body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;
                   height:100vh;margin:0;background:#1A1A2E;color:#fff;}
              .card{text-align:center;padding:40px;background:#16213E;border-radius:16px;}
              h2{color:#FFE03A;margin-bottom:8px;}
            </style>
            </head><body>
            <div class="card">
              <h2>✅ Calendar connected!</h2>
              <p>You can close this tab and go back to WakeyWakey.</p>
            </div>
            </body></html>
        """.trimIndent()

        private val ERROR_HTML = """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8">
            <title>WakeyWakey — Error</title>
            <style>
              body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;
                   height:100vh;margin:0;background:#1A1A2E;color:#fff;}
              .card{text-align:center;padding:40px;background:#16213E;border-radius:16px;}
              h2{color:#FF6B6B;margin-bottom:8px;}
            </style>
            </head><body>
            <div class="card">
              <h2>❌ Authentication failed</h2>
              <p>Please close this tab and try again in WakeyWakey.</p>
            </div>
            </body></html>
        """.trimIndent()
    }
}
