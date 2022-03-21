package com.justice.ocr_test

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface OcrService {


    @Multipart
    @POST("analyze/")
    suspend fun loadImage(
        @Part
        image: MultipartBody.Part,
        @Header("Content-Type") content_type: String,
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String
    ): Response<JsonObject>

    @POST("analyze/")
    suspend fun loadImage_2(
        @Body
        image: JsonObject,
        @Header("Content-Type") content_type: String,
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String
    ): Response<JsonObject>


}