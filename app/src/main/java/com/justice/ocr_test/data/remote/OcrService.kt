package com.justice.ocr_test.data.remote

import com.google.gson.JsonObject
import com.justice.ocr_test.presentation.ui.main.models.OcrResponseData
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface OcrService {


    @Multipart
    @Streaming
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
    ): Response<Void>
    @GET("analyzeResults/{operationId}")
    suspend fun get_image(
        @Path("operationId")
        operationId: String,
        @Header("Content-Type") content_type: String,
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String
    ): Response<OcrResponseData>


}