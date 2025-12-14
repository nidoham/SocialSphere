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
                    if (!response.isSuccessful) {
                        return@withContext ImgbbUploadResult(
                            false,
                            null,
                            "Upload failed: ${response.message}"
                        )
                    }

                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)

                    val imageUrl =
                        json.getJSONObject("data").getString("url")

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
                    errorMessage = e.localizedMessage
                )
            }
        }
}
