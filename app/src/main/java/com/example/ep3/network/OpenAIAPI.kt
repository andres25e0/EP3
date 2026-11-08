package com.example.ep3.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.example.ep3.modelo.OpenAIRequest
import com.example.ep3.modelo.OpenAIReasoning
import com.example.ep3.modelo.OpenAIResponse
import com.example.ep3.modelo.OpenAIError
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.EOFException

import java.io.IOException

import java.net.SocketTimeoutException

import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

sealed interface ResultadoIA {
    data class Exito(val texto: String, val responseId: String) : ResultadoIA
    data class Error(val mensaje: String, val http: Int? = null, val codigo: String = "") : ResultadoIA
}
class OpenAIAPI(private val apiKey: String, private val service: OpenAIService = crearServicio()) {
    suspend fun consultar(prompt: String): ResultadoIA = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey.startsWith("key_") || apiKey == "TU_API_KEY_AQUI") {
            return@withContext ResultadoIA.Error("Configura la clave secreta OPENAI_API_KEY en local.properties y recompila. El Tracking ID no es una clave secreta.")
        }

        try {
            coroutineContext.ensureActive()
            val response = service.crearRespuesta("Bearer ${apiKey.trim()}", crearSolicitud(prompt))
            coroutineContext.ensureActive()
            if (response.isSuccessful) {
                procesarRespuesta(response.code(), response.body())
            } else {
                val body = response.errorBody()?.use { it.string() }.orEmpty()
                val error = try { gson.fromJson(body, OpenAIResponse::class.java)?.error }
                    catch (_: JsonParseException) { null }
                errorHttp(response.code(), error)
            }
        } catch (_: JsonParseException) {
            jsonInvalido()
        } catch (_: MalformedJsonException) {
            jsonInvalido()
        } catch (_: EOFException) {
            jsonInvalido()
        } catch (_: SocketTimeoutException) {
            ResultadoIA.Error("Se agotó el tiempo de espera. Intenta de nuevo más tarde.")
        } catch (_: UnknownHostException) {
            ResultadoIA.Error("No se pudo encontrar OpenAI. Comprueba tu conexión a Internet.")
        } catch (_: IOException) {
            ResultadoIA.Error("No se pudo conectar con OpenAI. Revisa Internet e inténtalo de nuevo.")
        }
    }

    companion object {
        const val ENDPOINT = "https://api.openai.com/v1/responses"
        const val MODEL = "gpt-6-luna"

        private val gson = Gson()

        fun crearServicio(baseUrl: String = "https://api.openai.com/v1/"): OpenAIService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(OpenAIService::class.java)
        }

        fun crearSolicitud(prompt: String) = OpenAIRequest(
            model = MODEL,
            instructions = "Eres el asistente escolar de TuStudio. Responde en español, con claridad y extensión moderada, como máximo 350 palabras. Usa texto sencillo.",
            input = prompt,
            reasoning = OpenAIReasoning("none"),
            maxOutputTokens = 1200,
            store = false
        )

        private fun jsonInvalido() = ResultadoIA.Error("OpenAI devolvió JSON inválido.", 200, "invalid_json")

        fun procesarRespuesta(http: Int, response: OpenAIResponse?): ResultadoIA {
            if (http !in 200..299 || response?.error != null) return errorHttp(http, response?.error)
            if (response == null) return ResultadoIA.Error("OpenAI respondió sin texto útil.", http, "empty_output")
            if (response.status != "completed") {
                return ResultadoIA.Error("La respuesta de OpenAI no se completó. Intenta con un tema más breve.", http, "incomplete_response")
            }
            val texts = mutableListOf<String>()
            for (item in response.output.orEmpty()) {
                if (item?.type != "message" || item.role != "assistant") continue
                if (item.status != null && item.status != "completed")
                    return ResultadoIA.Error("El mensaje recibido está incompleto.", http, "incomplete_message")
                for (block in item.content.orEmpty()) {
                    if (block?.type == "refusal")
                        return ResultadoIA.Error("OpenAI no pudo responder a esta solicitud. Reformula el tema.", http, "refusal")
                    if (block?.type == "output_text") {
                        block.text?.trim()?.takeIf { it.isNotEmpty() }?.let(texts::add)
                    }
                }
            }
            if (texts.isEmpty()) return ResultadoIA.Error("OpenAI respondió sin texto útil.", http, "empty_output")
            return ResultadoIA.Exito(texts.joinToString("\n\n"), response.id.orEmpty())
        }

        private fun errorHttp(http: Int, error: OpenAIError?): ResultadoIA.Error {
            val code = error?.code.orEmpty()
            val type = error?.type.orEmpty()
            val message = error?.message.orEmpty().lowercase()
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
