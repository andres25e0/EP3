package com.example.ep3.network

import com.example.ep3.modelo.OpenAIRequest
import com.example.ep3.modelo.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIService {
    @Headers("Content-Type: application/json")
    @POST("responses")
    suspend fun crearRespuesta(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}
