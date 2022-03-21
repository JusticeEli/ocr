package com.justice.ocr_test

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.justice.ocr_test.databinding.ActivityMainBinding
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVision
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionManager
import com.microsoft.azure.cognitiveservices.vision.computervision.implementation.ComputerVisionImpl
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OperationStatusCodes
import com.microsoft.azure.cognitiveservices.vision.computervision.models.ReadOperationResult
import java.io.File
import java.nio.file.Files
import java.util.*
import android.provider.OpenableColumns

import android.database.Cursor
import com.google.gson.JsonObject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.io.FileOutputStream

import java.io.OutputStream

import java.io.InputStream

import java.io.IOException
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private val TAG = "MainActivity"
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: ")
        setOnClickListener()
      // launchGallery()
        CoroutineScope(Dispatchers.IO).launch {
            do_post(File(""))
        }

    }

    private suspend fun do_post(file: File) {
        Log.d(TAG, "do_post: loading...")
        val service = provideRetrofitService()
        //pass it like this
        val requestFile: RequestBody =
            RequestBody.create(MediaType.parse("multipart/form-data"), file)

        // MultipartBody.Part is used to send also the actual file name
        val body: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", file.name, requestFile)

//val contentType="multipart/form-data"
        val contentType = "application/json"
        // val response = service.loadImage(body,contentType,subscriptionKey)
        val jsonObject=JsonObject()
        jsonObject.addProperty("url","https://www.researchgate.net/publication/334355929/figure/tbl2/AS:778947612639232@1562726991220/List-of-words-used-in-the-experiment-containing-clear-variants-of-the-allophone-l.png")
        val response = service.loadImage_2(jsonObject, contentType, subscriptionKey)
        Log.d(TAG, "do_post: reponse:$response")
        if (response.isSuccessful) {
            Log.d(TAG, "do_post: success")
            val body = response.body()
            Log.d(TAG, "do_post:body: $body")
            Log.d(TAG, "do_post:headers: ${response.headers()}")

        } else {
            val body = response.errorBody()!!.string()!!
            Log.d(TAG, "do_post:Error:${body} ")
        }


    }


    fun provideRetrofitService(): OcrService {
        val subscriptionKey = "358439c19db049cbb02e9150e456e5aa";
        val endpoint = "school-management.cognitiveservices.azure.com";
      //  val BASE_URL = "https://${endpoint}vision/v3.2/"
        val BASE_URL = "https://$endpoint/vision/v3.2/read/analyze/"

       // val BASE_URL = "https://eastus.api.cognitive.microsoft.com/vision/v3.2/read/analyze/"
//https://westcentralus.api.cognitive.microsoft.com/vision/v3.2/read/analyze"  should return operation-location
        //("https://westus.api.cognitive.microsoft.com/vision/v3.2/analyze");
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


    private fun setOnClickListener() {
        binding.button.setOnClickListener {
            launchGallery()
        }

    }

    private fun launchGallery() {

        launchGalleryWithFragment()
    }

    fun launchGalleryWithFragment() {
        Log.d(TAG, "launchGalleryWithFragment: ")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        Log.d(TAG, "onActivityResult: ")
        var path: Uri? = null
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (dataIntent == null || dataIntent.data == null) {
                Log.d(TAG, "onActivityResult: data_intent is null")
                return
            }
            path = dataIntent.data
            Log.d(TAG, "onActivityResult: path:$path")


// Create an authenticated Computer Vision client.
            // Create an authenticated Computer Vision client.
            val compVisClient = Authenticate()!!

            val file = getFile(applicationContext, path!!)
            CoroutineScope(Dispatchers.IO).launch {
                do_post(file!!)

                //  ReadFromFile(compVisClient, file!!)

            }


        }

    }


    @Throws(IOException::class)
    fun getFile(context: Context, uri: Uri): File? {
        val destinationFilename: File =
            File(context.getFilesDir().getPath() + File.separatorChar + queryName(context, uri))
        try {
            context.getContentResolver().openInputStream(uri).use { ins ->
                createFileFromStream(
                    ins!!,
                    destinationFilename
                )
            }
        } catch (ex: Exception) {
            Log.e("Save File", ex.message!!)
            ex.printStackTrace()
        }
        return destinationFilename
    }

    fun createFileFromStream(ins: InputStream, destination: File?) {
        try {
            FileOutputStream(destination).use { os ->
                val buffer = ByteArray(4096)
                var length: Int
                while (ins.read(buffer).also { length = it } > 0) {
                    os.write(buffer, 0, length)
                }
                os.flush()
            }
        } catch (ex: Exception) {
            Log.e("Save File", ex.message!!)
            ex.printStackTrace()
        }
    }

    private fun queryName(context: Context, uri: Uri): String {
        val returnCursor: Cursor = context.getContentResolver().query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    /**
     * OCR with READ : Performs a Read Operation on a local image
     * @param client instantiated vision client
     * // * @param localFilePath local file path from which to perform the read operation against
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun ReadFromFile(client: ComputerVisionClient, rawImage: File) {
        Log.d(TAG, "ReadFromFile: ")
        val localFilePath = "src\\main\\resources\\myImage.png"
        Log.d(TAG, "ReadFromFile: Read with local file: $localFilePath")
        try {
            // File rawImage = new File(localFilePath);
            val localImageBytes = Files.readAllBytes(rawImage.toPath())

            // Cast Computer Vision to its implementation to expose the required methods
            val vision = client.computerVision() as ComputerVisionImpl

            // Read in remote image and response header
            val responseHeader =
                vision.readInStreamWithServiceResponseAsync(localImageBytes, null, null)
                    .toBlocking()
                    .single()
                    .headers()

// Extract the operationLocation from the response header
            Log.d(TAG, "ReadFromFile: header:$responseHeader")
            val operationLocation = responseHeader.operationLocation()
            Log.d(TAG, "ReadFromFile: Operation Location:$operationLocation")
            getAndPrintReadResult(vision, operationLocation)
        } catch (e: Exception) {
            Log.e(TAG, "ReadFromFile: Error", e)
        }
    }


    /**
     * Polls for Read result and prints results to console
     * @param vision Computer Vision instance
     * @return operationLocation returned in the POST Read response header
     */
    @Throws(InterruptedException::class)
    private fun getAndPrintReadResult(vision: ComputerVision, operationLocation: String) {
        Log.d(TAG, "Polling for Read results ...")
        // Extract OperationId from Operation Location
        val operationId = extractOperationIdFromOpLocation(operationLocation)
        var pollForResult = true
        var readResults: ReadOperationResult? = null
        while (pollForResult) {
            // Poll for result every second
            Thread.sleep(1000)
            readResults = vision.getReadResult(UUID.fromString(operationId))

            // The results will no longer be null when the service has finished processing the request.
            if (readResults != null) {
                // Get request status
                val status = readResults.status()
                if (status == OperationStatusCodes.FAILED || status == OperationStatusCodes.SUCCEEDED) {
                    pollForResult = false
                }
            }
        }

        // Print read results, page per page
        for (pageResult in readResults!!.analyzeResult().readResults()) {

            Log.d(TAG, "getAndPrintReadResult: Printing Read results for page " + pageResult.page())
            Log.d(
                TAG,
                "getAndPrintReadResult:Printing Read results for page  ${pageResult.page()} "
            )
            val builder = StringBuilder()
            for (line in pageResult.lines()) {
                builder.append(line.text())
                builder.append("\n")
            }
            Log.d(TAG, "getAndPrintReadResult: ${builder.toString()}")
            // android.util.Log.d(TAG, "getAndPrintReadResult: ")(builder.toString())
        }
    }


    /**
     * Extracts the OperationId from a Operation-Location returned by the POST Read operation
     * @param operationLocation
     * @return operationId
     */
    private fun extractOperationIdFromOpLocation(operationLocation: String?): String {
        if (operationLocation != null && !operationLocation.isEmpty()) {
            val splits = operationLocation.split("/").toTypedArray()
            if (splits != null && splits.size > 0) {
                return splits[splits.size - 1]
            }
        }
        throw IllegalStateException("Something went wrong: Couldn't extract the operation id from the operation location")
    }


    val subscriptionKey = "358439c19db049cbb02e9150e456e5aa";
    val endpoint = "https://school-management.cognitiveservices.azure.com/";


    fun Authenticate(): ComputerVisionClient? {
        Log.d(TAG, "Authenticate: ")
        val subscriptionKey = "358439c19db049cbb02e9150e456e5aa";
        val endpoint = "https://school-management.cognitiveservices.azure.com/";


        return ComputerVisionManager.authenticate(subscriptionKey).withEndpoint(endpoint)
    }
}