package com.justice.ocr_test.di

import android.content.Context
import android.content.SharedPreferences
import com.justice.ocr_test.utils.Constants
import com.justice.ocr_test.data.remote.OcrService
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOcrService(@ApplicationContext context: Context): OcrService {

        val subscriptionKey = "358439c19db049cbb02e9150e456e5aa";
        val endpoint = "school-management.cognitiveservices.azure.com";
        //  val BASE_URL = "https://${endpoint}vision/v3.2/"
        //  val BASE_URL = "https://$endpoint/vision/v3.2/read/analyze/"
        // val BASE_URL = "https://school-management.api.cognitive.microsoft.com/vision/v3.2/read/"
        val BASE_URL = "https://school-management.cognitiveservices.azure.com/vision/v3.2/read/"


        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build().create(OcrService::class.java)
    }


    @Provides
    @Singleton
    fun provideSharedPref(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideComputerVisionClient(): ComputerVisionClient {
        val subscriptionKey = "358439c19db049cbb02e9150e456e5aa";
        val endpoint = "https://school-management.cognitiveservices.azure.com/";
        return ComputerVisionManager.authenticate(subscriptionKey).withEndpoint(endpoint)
    }
}


