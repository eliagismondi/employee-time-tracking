package com.example.gestionale_personale.domain.usecase

import android.util.Log
import com.example.gestionale_personale.BuildConfig
import com.example.gestionale_personale.data.remote.ChatChoice
import com.example.gestionale_personale.data.remote.ChatMessage
import com.example.gestionale_personale.data.remote.ChatRequest
import com.example.gestionale_personale.data.remote.GroqApi
import com.example.gestionale_personale.data.remote.ResponseFormat
import com.example.gestionale_personale.domain.model.Employee
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class AiProcessingUseCase {

    private val groqApi: GroqApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        groqApi = retrofit.create(GroqApi::class.java)
    }

    suspend fun processAudioAndExtractShifts(
        audioFile: File,
        activeEmployees: List<Employee>
    ): Pair<String?, List<AiExtractedShift>> {
        return try {
            // 1. Trascrizione Audio via Whisper
            val requestFile = audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val model = "whisper-large-v3".toRequestBody("text/plain".toMediaTypeOrNull())

            Log.d("AiUseCase", "Avvio trascrizione audio...")
            val transcription = groqApi.transcribeAudio(body, model).text
            Log.d("AiUseCase", "Trascrizione: $transcription")

            // 2. Estrazione JSON via LLaMA
            val systemPrompt = buildSystemPrompt(activeEmployees)

            val chatRequest = ChatRequest(
                model = "llama-3.1-8b-instant",
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = "Testo: $transcription")
                ),
                response_format = ResponseFormat(type = "json_object"),
                temperature = 0.0 // Massimo determinismo
            )

            Log.d("AiUseCase", "Avvio estrazione JSON...")
            val response = groqApi.extractWorkEntries(chatRequest)
            val jsonString = response.choices.firstOrNull()?.message?.content ?: "{}"
            Log.d("AiUseCase", "JSON Estratto: $jsonString")

            // 3. Parsing del JSON
            val result = parseJsonResult(jsonString)
            Pair(result.first, result.second)

        } catch (e: Exception) {
            Log.e("AiUseCase", "Errore durante l'elaborazione AI", e)
            Pair("Errore di connessione o elaborazione AI: ${e.message}", emptyList())
        }
    }

    private fun buildSystemPrompt(employees: List<Employee>): String {
        val empNames = employees.joinToString(", ") { "${it.id}|${it.firstName} ${it.lastName}" }

        return """
            Sei un assistente per un'impresa edile/ponteggi. Il tuo compito è estrarre i turni di lavoro da una trascrizione vocale confusa.
            
            Dipendenti attivi in azienda (formato ID|Nome Completo):
            $empNames
            
            REGOLE OBBLIGATORIE:
            1. Devi mappare il nome pronunciato all'ID esatto del dipendente corrispondente della lista sopra. Se il nome non assomiglia a nessuno in lista, omettilo.
            2. Estrai l'orario di INIZIO e FINE per ogni turno, nel formato "HH:mm". Se non menzionato, usa null.
            3. Estrai il NOME DEL CANTIERE. Se non menzionato, usa null.
            4. Estrai le NOTE (es. lavori fatti). Se non menzionato, usa null.
            5. Estrai una stringa opzionale "generalNotes" se nell'audio c'è un resoconto generale (es. stato dei mezzi, meteo).
            6. DEVI RESTITUIRE ESCLUSIVAMENTE UN OGGETTO JSON. NON aggiungere testo prima o dopo.

            ESEMPIO DI OUTPUT:
            {
              "generalNotes": "Domani piove a Civitanova, ricordarsi i teli.",
              "shifts": [
                {
                  "employeeId": "id-del-dipendente",
                  "startTime": "08:00",
                  "endTime": "12:00",
                  "worksite": "Fermo",
                  "notes": "Montaggio base"
                }
              ]
            }
        """.trimIndent()
    }

    private fun parseJsonResult(jsonString: String): Pair<String, List<AiExtractedShift>> {
        val shiftsList = mutableListOf<AiExtractedShift>()
        var generalNotes = ""

        try {
            val jsonObject = JSONObject(jsonString)
            if (jsonObject.has("generalNotes")) {
                val notes = jsonObject.getString("generalNotes")
                if (notes != "null") generalNotes = notes
            }

            if (jsonObject.has("shifts")) {
                val shiftsArray = jsonObject.getJSONArray("shifts")
                for (i in 0 until shiftsArray.length()) {
                    val shiftObj = shiftsArray.getJSONObject(i)

                    val empId = if (shiftObj.has("employeeId") && !shiftObj.isNull("employeeId")) shiftObj.getString("employeeId") else null
                    val start = if (shiftObj.has("startTime") && !shiftObj.isNull("startTime")) shiftObj.getString("startTime") else null
                    val end = if (shiftObj.has("endTime") && !shiftObj.isNull("endTime")) shiftObj.getString("endTime") else null
                    val worksite = if (shiftObj.has("worksite") && !shiftObj.isNull("worksite")) shiftObj.getString("worksite") else null
                    val notes = if (shiftObj.has("notes") && !shiftObj.isNull("notes")) shiftObj.getString("notes") else null

                    if (empId != null) {
                        shiftsList.add(AiExtractedShift(empId, start, end, worksite, notes))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AiUseCase", "Errore Parsing JSON", e)
        }

        return Pair(generalNotes, shiftsList)
    }
}

data class AiExtractedShift(
    val employeeId: String,
    val startTime: String?,
    val endTime: String?,
    val worksite: String?,
    val notes: String?
)
