package com.example.ep3.network

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenAIAPITest {
    private val completed = """{"id":"resp_local","status":"completed","output":[
        {"type":"reasoning","summary":[]},
        {"type":"message","role":"assistant","status":"completed","content":[
            {"type":"output_text","text":"Primero"},{"type":"output_text","text":"Segundo"}]},
        {"type":"message","role":"assistant","content":[{"type":"output_text","text":"Tercero"}]}
    ]}"""

    private fun localResponse(body: String, http: Int = 200): ResultadoIA =
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(http).setBody(body))
            val api = OpenAIAPI("local-test", OpenAIAPI.crearServicio(server.url("/v1/").toString()))
            runBlocking { api.consultar("Tema de prueba") }
        }

    @Test fun retrofitSerializesRequestAndCollectsAllOutputText() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(completed))
            val api = OpenAIAPI("local-test", OpenAIAPI.crearServicio(server.url("/v1/").toString()))
            val result = runBlocking { api.consultar("Tema \"citado\"\nInformática") }
            assertEquals("Primero\n\nSegundo\n\nTercero", (result as ResultadoIA.Exito).texto)
            assertEquals("resp_local", result.responseId)
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/v1/responses", request.path)
            assertEquals("Bearer local-test", request.getHeader("Authorization"))
            assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("gpt-6-luna", json["model"].asString)
            assertEquals("Tema \"citado\"\nInformática", json["input"].asString)
            assertEquals("none", json["reasoning"].asJsonObject["effort"].asString)
            assertEquals(1200, json["max_output_tokens"].asInt)
            assertFalse(json["store"].asBoolean)
            assertFalse(json.has("tools"))
            assertEquals("Eres el asistente escolar de TuStudio. Responde en español, con claridad y extensión moderada, como máximo 350 palabras. Usa texto sencillo.", json["instructions"].asString)
        }
    }

    @Test fun rejectsEmptyIncompleteRefusalAndNullOutput() {
        for (body in listOf(
            """{"status":"completed","output":[]}""",
            """{"status":"completed","output":[null,{"type":"message","role":"assistant","content":[null,{"type":"output_text","text":"  "}]}]}""",
            completed.replace("\"status\":\"completed\"", "\"status\":\"incomplete\""),
            """{"status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"refusal"}]}]}""",
            """{"status":"completed","output":null}""", "null"
        )) assertTrue(body, localResponse(body) is ResultadoIA.Error)
    }

    @Test fun handlesMalformedJsonAndInvalidShape() {
        for (body in listOf("{broken", """{"output":""", "[]")) {
            assertEquals("invalid_json", (localResponse(body) as ResultadoIA.Error).codigo)
        }
    }

    @Test fun preservesHttpErrorsAndHidesProviderSecrets() {
        for (http in listOf(400, 401, 403, 404, 429, 500, 503)) {
            val result = localResponse("""{"error":{"code":"test_error","message":"private-value"}}""", http) as ResultadoIA.Error
            assertEquals(http, result.http)
            assertFalse(result.mensaje.contains("private-value"))
        }
        assertEquals(503, (localResponse("<html>unavailable</html>", 503) as ResultadoIA.Error).http)
    }

    @Test fun distinguishesQuotaFromRateLimit() {
        val quota = localResponse("""{"error":{"code":"insufficient_quota"}}""", 429) as ResultadoIA.Error
        val rate = localResponse("""{"error":{"code":"rate_limit_exceeded"}}""", 429) as ResultadoIA.Error
        assertTrue(quota.mensaje.contains("créditos"))
        assertTrue(rate.mensaje.contains("Espera"))
    }

    @Test fun detectsProviderErrorInsideSuccessfulHttp() {
        assertTrue(localResponse("""{"error":{"code":"server_error"}}""") is ResultadoIA.Error)
        assertTrue(localResponse(completed.replace("Primero", "Error de medición")) is ResultadoIA.Exito)
    }

    @Test fun missingKeyNeverSendsRequest() {
        MockWebServer().use { server ->
            val api = OpenAIAPI("", OpenAIAPI.crearServicio(server.url("/v1/").toString()))
            assertTrue(runBlocking { api.consultar("tema") } is ResultadoIA.Error)
            assertEquals(0, server.requestCount)
        }
    }
}
