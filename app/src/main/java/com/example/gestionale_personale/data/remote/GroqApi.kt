package com.example.gestionale_personale.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class GroqRequest(
    val model: String = "whisper-large-v3",
    val file: okhttp3.MultipartBody.Part
)

data class ChatRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat? = null,
    val temperature: Double = 0.0
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class TranscriptionResponse(
    val text: String
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessage
)

interface GroqApi {
    @POST("audio/transcriptions")
    suspend fun transcribeAudio(
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("model") model: okhttp3.RequestBody
    ): TranscriptionResponse

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun extractWorkEntries(
        @Body request: ChatRequest
    ): ChatResponse
}
