package com.example.ep3.modelo

import java.io.Serializable

data class RespuestaIA(
    val tipoConsulta: String,
    val textoOriginal: String,
    val resultadoGenerado: String
) : Serializable
