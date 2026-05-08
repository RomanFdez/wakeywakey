package com.sierraespada.wakeywakey.windows.calendar

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Servidor HTTP mínimo usando [ServerSocket] estándar (sin APIs internas del JDK).
 * Escucha en localhost:PORT/callback y captura el parámetro ?code= del redirect OAuth.
 *
 * Ciclo de vida:
 *  1. [start] — lanza el servidor en background
 *  2. [awaitCode] — suspende hasta que el browser redirige con ?code=
 *  3. [stop] — cierra el servidor
 */
class OAuthCallbackServer(private val port: Int) {

    private var serverSocket: ServerSocket? = null
    private val deferred = CompletableDeferred<Result<String>>()

    val redirectUri: String get() = "http://localhost:$port/callback"

    fun start(): OAuthCallbackServer {
        val ss = ServerSocket(port)
        serverSocket = ss

        Thread(null, {
            runCatching {
                val client = ss.accept()
                client.use { socket ->
                    val request = socket.getInputStream()
                        .bufferedReader()
                        .readLine() ?: ""

                    // GET /callback?code=xxx&state=yyy HTTP/1.1
                    val query = request
                        .removePrefix("GET /callback?")
                        .substringBefore(" HTTP")
                        .takeIf { it.isNotBlank() && it != "GET /callback" }

                    val params = query
                        ?.split("&")
                        ?.associate { p ->
                            p.split("=", limit = 2).let { parts ->
                                parts[0] to URLDecoder.decode(parts.getOrNull(1) ?: "", StandardCharsets.UTF_8)
                            }
                        }
                        ?: emptyMap()

                    val code  = params["code"]
                    val error = params["error"]
                    System.err.println("OAuthCallback: code=${code?.take(20)}... error=$error params=${params.keys}")

                    val (status, html) = when {
                        code  != null -> { deferred.complete(Result.success(code));                          200 to SUCCESS_HTML }
                        error != null -> { deferred.complete(Result.failure(Exception("OAuth error: $error"))); 400 to ERROR_HTML   }
                        else          -> { deferred.complete(Result.failure(Exception("No code in callback"))); 400 to ERROR_HTML   }
                    }

                    val body    = html.toByteArray()
                    val headers = "HTTP/1.1 $status OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().let { out ->
                        out.write(headers.toByteArray())
                        out.write(body)
                        out.flush()
                    }
                }
            }.onFailure { e ->
                if (!deferred.isCompleted) deferred.complete(Result.failure(e))
            }
            runCatching { ss.close() }
        }, "oauth-callback-server").also { it.isDaemon = true }.start()

        return this
    }

    suspend fun awaitCode(): String = withContext(Dispatchers.IO) {
        deferred.await().getOrThrow()
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
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
