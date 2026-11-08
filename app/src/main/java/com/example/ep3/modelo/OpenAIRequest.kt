package com.example.ep3.modelo

import com.google.gson.annotations.SerializedName

data class OpenAIRequest(
    @SerializedName("model") val model: String,
    @SerializedName("instructions") val instructions: String,
    @SerializedName("input") val input: String,
    @SerializedName("reasoning") val reasoning: OpenAIReasoning,
    @SerializedName("max_output_tokens") val maxOutputTokens: Int,
    @SerializedName("store") val store: Boolean
)

data class OpenAIReasoning(
    @SerializedName("effort") val effort: String
)
