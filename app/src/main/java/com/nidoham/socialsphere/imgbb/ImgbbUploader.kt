package com.nidoham.socialsphere.imgbb

import com.nidoham.socialsphere.api.ImgbbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

class ImgbbUploader(
    private val apiKey: String = ImgbbApi.API_KEY
) {

    private val client = OkHttpClient()

    suspend fun uploadImage(imageFile: File): ImgbbUploadResult =
        withContext(Dispatchers.IO) {
            try {
                // Validate file exists and is readable
                if (!imageFile.exists()) {
                    return@withContext ImgbbUploadResult(
                        success = false,
                        imageUrl = null,
                        errorMessage = "File does not exist: ${imageFile.absolutePath}"
                    )
                }

                if (!imageFile.canRead()) {
                    return@withContext ImgbbUploadResult(
                        success = false,
                        imageUrl = null,
                        errorMessage = "Cannot read file: ${imageFile.absolutePath}"
                    )
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image",
                        imageFile.name,
                        imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()

                    if (!response.isSuccessful) {
                        val errorMsg = try {
                            body?.let { JSONObject(it).optString("error", response.message) }
                                ?: response.message
                        } catch (e: Exception) {
                            response.message
                        }

                        return@withContext ImgbbUploadResult(
                            success = false,
                            imageUrl = null,
                            errorMessage = "Upload failed (${response.code}): $errorMsg"
                        )
                    }

                    if (body.isNullOrEmpty()) {
                        return@withContext ImgbbUploadResult(
                            success = false,
                            imageUrl = null,
                            errorMessage = "Empty response from server"
                        )
                    }

                    val json = JSONObject(body)

                    // Check if response contains expected data
                    if (!json.has("data")) {
                        return@withContext ImgbbUploadResult(
                            success = false,
                            imageUrl = null,
                            errorMessage = "Invalid response format: missing 'data' field"
                        )
                    }

                    val dataObject = json.getJSONObject("data")
                    if (!dataObject.has("url")) {
                        return@withContext ImgbbUploadResult(
                            success = false,
                            imageUrl = null,
                            errorMessage = "Invalid response format: missing 'url' field"
                        )
                    }

                    val imageUrl = dataObject.getString("url")

                    ImgbbUploadResult(
                        success = true,
                        imageUrl = imageUrl,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                ImgbbUploadResult(
                    success = false,
                    imageUrl = null,
                    errorMessage = "Exception: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                )
            }
        }
}