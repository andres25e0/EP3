package com.example.ep3.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

sealed interface ResultadoIA {
    data class Exito(val texto: String, val responseId: String) : ResultadoIA
    data class Error(val mensaje: String, val http: Int? = null, val codigo: String = "") : ResultadoIA
}
class OpenAIAPI(private val apiKey: String) {
    suspend fun consultar(prompt: String): ResultadoIA = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey.startsWith("key_") || apiKey == "TU_API_KEY_AQUI") {
            return@withContext ResultadoIA.Error("Configura la clave secreta OPENAI_API_KEY en local.properties y recompila. El Tracking ID no es una clave secreta.")
        }
        var connection: HttpURLConnection? = null
        try {
            coroutineContext.ensureActive()
            connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(crearSolicitud(prompt).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            coroutineContext.ensureActive()
            procesarRespuesta(status, body)
        } catch (_: SocketTimeoutException) {
            ResultadoIA.Error("Se agotó el tiempo de espera. Intenta de nuevo más tarde.")
        } catch (_: UnknownHostException) {
            ResultadoIA.Error("No se pudo encontrar OpenAI. Comprueba tu conexión a Internet.")
        } catch (_: IOException) {
            ResultadoIA.Error("No se pudo conectar con OpenAI. Revisa Internet e inténtalo de nuevo.")
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val ENDPOINT = "https://api.openai.com/v1/responses"
        const val MODEL = "gpt-6-luna"

        fun crearSolicitud(prompt: String): JSONObject = JSONObject()
            .put("model", MODEL)
            .put("instructions", "Eres el asistente escolar de TuStudio. Responde en español, con claridad y extensión moderada, como máximo 350 palabras. Usa texto sencillo.")
            .put("input", prompt)
            .put("reasoning", JSONObject().put("effort", "none"))
            .put("max_output_tokens", 1200)
            .put("store", false)

        fun procesarRespuesta(http: Int, body: String): ResultadoIA {
            val json = try { JSONObject(body) } catch (_: JSONException) {
                return if (http !in 200..299) errorHttp(http, null)
                else ResultadoIA.Error("OpenAI devolvió JSON inválido.", http, "invalid_json")
            }
            val error = json.optJSONObject("error")
            if (http !in 200..299 || error != null) return errorHttp(http, error)
            if (json.optString("status") != "completed") {
                return ResultadoIA.Error("La respuesta de OpenAI no se completó. Intenta con un tema más breve.", http, "incomplete_response")
            }
            val texts = mutableListOf<String>()
            val output = json.optJSONArray("output")
            if (output != null) for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                if (item.optString("type") != "message" || item.optString("role") != "assistant") continue
                if (item.optString("status", "completed") != "completed")
                    return ResultadoIA.Error("El mensaje recibido está incompleto.", http, "incomplete_message")
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val block = content.optJSONObject(j) ?: continue
                    if (block.optString("type") == "refusal")
                        return ResultadoIA.Error("OpenAI no pudo responder a esta solicitud. Reformula el tema.", http, "refusal")
                    if (block.optString("type") == "output_text") {
                        (block.opt("text") as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let(texts::add)
                    }
                }
            }
            if (texts.isEmpty()) return ResultadoIA.Error("OpenAI respondió sin texto útil.", http, "empty_output")
            return ResultadoIA.Exito(texts.joinToString("\n\n"), json.optString("id"))
        }

        private fun errorHttp(http: Int, error: JSONObject?): ResultadoIA.Error {
            val code = error?.optString("code").orEmpty()
            val type = error?.optString("type").orEmpty()
            val message = error?.optString("message").orEmpty().lowercase()
            val quota = code == "insufficient_quota" || type == "insufficient_quota" ||
                code == "billing_hard_limit_reached" || message.contains("current quota") ||
                message.contains("billing") || message.contains("credit balance")
            val explanation = when (http) {
                400 -> "Solicitud inválida. Revisa el modelo y los parámetros configurados."
                401 -> "Autenticación fallida. Revisa la clave secreta de OpenAI y su vigencia."
                403 -> "Permisos insuficientes. Revisa el acceso de tu proyecto y organización."
                404 -> "Modelo o recurso no encontrado. Comprueba el acceso a $MODEL."
                429 -> if (quota) "Cuota o créditos insuficientes. Revisa la facturación de OpenAI API."
                    else "Límite de solicitudes alcanzado. Espera antes de volver a intentar."
                500, 503 -> "Fallo temporal de OpenAI. Intenta de nuevo más tarde."
                else -> "OpenAI no pudo completar la solicitud."
            }
            val safeCode = code.takeIf { it.matches(Regex("[a-z_]{1,64}")) }.orEmpty()
            return ResultadoIA.Error("$explanation (HTTP $http)" + if (safeCode.isNotEmpty()) " [$safeCode]" else "", http, safeCode)
        }
    }
}
