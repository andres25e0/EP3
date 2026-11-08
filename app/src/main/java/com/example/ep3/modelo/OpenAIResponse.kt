package com.example.ep3.modelo

import com.google.gson.annotations.SerializedName

data class OpenAIResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("output") val output: List<OpenAIOutput?>? = null,
    @SerializedName("error") val error: OpenAIError? = null
)

data class OpenAIOutput(
    @SerializedName("type") val type: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("content") val content: List<OpenAIContent?>? = null
)

data class OpenAIContent(
    @SerializedName("type") val type: String? = null,
    @SerializedName("text") val text: String? = null
)

data class OpenAIError(
    @SerializedName("code") val code: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("message") val message: String? = null
)
