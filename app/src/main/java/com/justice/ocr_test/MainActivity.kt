package com.justice.ocr_test

import android.R.attr
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.io.FileInputStream
import java.lang.reflect.Type
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import android.R.attr.data
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream





class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private val TAG = "MainActivity"
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences =
            getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)

        setOnClickListener()
        //  launchGallery()
        //launchImagePicker()
        /*    CoroutineScope(Dispatchers.IO).launch {
               //  val operationId = do_post_url()
                val operationId = "bf45a229-118d-4b9d-a6bb-2cd594891fc4"
                do_get(operationId)
            }*/


    }

    private fun launchImagePicker() {
        Log.d(TAG, "launchImagePicker: ")
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            //.setAspectRatio(1, 72)
            //.setOutputUri()
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(this);
    }


    private suspend fun do_post_url(): String {
        Log.d(TAG, "do_post_url: loading...")
        val service = provideRetrofitService()


//val contentType="multipart/form-data"
        val contentType = "application/json"
        // val response = service.loadImage(body,contentType,subscriptionKey)
        val jsonObject = JsonObject()
        //    val answersheetURL =
        //       "https://user-images.githubusercontent.com/63531125/160584193-65a29b7a-004a-4aa6-ad02-f0a488ef0a8f.jpg"
        val answersheetURL =
            "https://user-images.githubusercontent.com/63531125/160826685-b6dd47be-2b0e-44fc-9bf1-85cb736660be.jpg"
        jsonObject.addProperty("url", answersheetURL)
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
        val lastIndex = response.headers().get("operation-location")!!.lastIndexOf("/") + 1
        val operationId = response.headers().get("operation-location")!!.substring(lastIndex)
        Log.d(TAG, "do_post: operationId:$operationId")
        return operationId
    }

    //operation-location: https://school-management.cognitiveservices.azure.com/vision/v3.2/read/analyzeResults/d7738ff8-d20e-4a89-ab3e-13ce75079c52

    private suspend fun do_get(operationId: String) {
        Log.d(TAG, "do_get: operationId:$operationId")
        Log.d(TAG, "do_get: loading...")
        val service = provideRetrofitService()

//val contentType="multipart/form-data"
        val contentType = "application/json"
        // val response = service.loadImage(body,contentType,subscriptionKey)
        val response = service.get_image(operationId, contentType, subscriptionKey)
        Log.d(TAG, "do_get: reponse:$response")

        if (response.isSuccessful) {
            if (!response.body()!!.status.equals("succeeded")) {
                Log.d(TAG, "do_get: not succeeded ,retrying...")
                delay(60_000)
//1s 1000ms
//1m 60s
                do_get(operationId)
                return
            }

            Log.d(TAG, "do_get: success")
            val body = response.body()
            // Log.d(TAG, "do_get:body: $body")
            // Log.d(TAG, "do_get:headers: ${response.headers()}")
            // Log.d(TAG, "do_get: ${response.body()!!.analyzeResult.readResults}")
            /*    for (readResult in response.body()!!.analyzeResult.readResults) {
                    Log.d(TAG, "do_get: readResult")
                    var index = 0
                    for (line in readResult.lines) {
                        Log.d(TAG, "do_get: line $index >>${line.text}")
                        //  Log.d(TAG, "do_get: words:${line.words}")
                        index += 1
                    }

                }*/
            analyzeAnswers(response.body()!!.analyzeResult.readResults)

        } else {
            val body = response.errorBody()!!.string()!!
            Log.d(TAG, "do_get:Error:${body} ")
        }


    }

    private fun analyzeAnswers(readResults: List<ReadResult>) {
        Log.d(TAG, "analyzeAnswers: ")
        val studentsAnswers = mutableListOf<Answer>()
        for (readResult in readResults) {
            var index = 0
            for (line in readResult.lines) {
                Log.d(TAG, "analyzeAnswers: line $index >>${line.text}")
                val data = line.text.split(",")
                if (data.size == 5) {
                    val answer = Answer()
                    answer.number = Integer.valueOf(data[0].trim())
                    val choices = data.toList().map { it.trim() }
                    if (!choices.contains("A")) {
                        answer.choice = "A"
                    } else if (!choices.contains("B")) {
                        answer.choice = "B"
                    } else if (!choices.contains("C")) {
                        answer.choice = "C"
                    } else if (!choices.contains("D")) {
                        answer.choice = "D"
                    } else {
                        answer.choice = "No Answer Written"
                    }
                    Log.d(TAG, "analyzeAnswers: answer:$answer")
                    studentsAnswers.add(answer)
                }


                index += 1
            }

        }
        Log.d(TAG, "analyzeAnswers: studentAnswers:$studentsAnswers")
        startTheMarkingProcess(studentsAnswers)
    }

    private fun analyzeAnswers_From_Azure(readResults: List<com.microsoft.azure.cognitiveservices.vision.computervision.models.ReadResult>) {
        Log.d(TAG, "analyzeAnswers: ")
        val studentsAnswers = mutableListOf<Answer>()
        for (readResult in readResults) {
            var index = 0
            for (line in readResult.lines()) {
                Log.d(TAG, "analyzeAnswers: line $index >>${line.text()}")
                val data = line.text().split(",")
                if (data.size == 5) {
                    val answer = Answer()
                    answer.number = Integer.valueOf(data[0].trim())
                    val choices = data.toList().map { it.trim() }
                    if (!choices.contains("A")) {
                        answer.choice = "A"
                    } else if (!choices.contains("B")) {
                        answer.choice = "B"
                    } else if (!choices.contains("C")) {
                        answer.choice = "C"
                    } else if (!choices.contains("D")) {
                        answer.choice = "D"
                    } else {
                        answer.choice = "No Answer Written"
                    }
                    Log.d(TAG, "analyzeAnswers: answer:$answer")
                    studentsAnswers.add(answer)
                }


                index += 1
            }

        }
        Log.d(TAG, "analyzeAnswers: studentAnswers:$studentsAnswers")
        startTheMarkingProcess(studentsAnswers)
    }

    private fun startTheMarkingProcess(studentsAnswers: MutableList<Answer>) {
        Log.d(TAG, "startTheMarkingProcess: size:${studentsAnswers.size}")
        Log.d(TAG, "startTheMarkingProcess: studentsAnswers:$studentsAnswers")
        val teachersAnswers = fetchAnswerFromSharedPref()
        studentsAnswers.retainAll(teachersAnswers)
        Log.d(TAG, "startTheMarkingProcess: marks size:${studentsAnswers.size}")
    }

    lateinit var sharedPreferences: SharedPreferences

    fun fetchAnswerFromSharedPref(): MutableList<Answer> {
        Log.d(TAG, "fetchAnswerFromSharedPref: ")
        var teachersAnswers = mutableListOf<Answer>()

        val gson = Gson()
        val json: String? = sharedPreferences.getString(Constants.KEY_ANSWERS, null)
        if (json == null) {
            Log.d(TAG, "fetchAnswerFromSharedPref:default answers failed")
            teachersAnswers.clear()
            for (i in 1..50) {
                val answer = Answer()
                answer.choice = "A"
                answer.number = i
                teachersAnswers.add(answer)
            }
        } else {
            Log.d(TAG, "fetchAnswerFromSharedPref: default answers success were found")
            val type: Type = object : TypeToken<ArrayList<Answer?>?>() {}.getType()
            teachersAnswers = gson.fromJson(json, type)
        }
        Log.d(TAG, "fetchAnswerFromSharedPref:size:${teachersAnswers.size} ")
        Log.d(TAG, "fetchAnswerFromSharedPref:teachersAnswers:$teachersAnswers ")
        return teachersAnswers
    }

    private suspend fun do_post_file(file: File): String {
        Log.d(TAG, "do_post_file: file:${file.path}")
        Log.d(TAG, "do_post_file: loading...")
        val service = provideRetrofitService()
        val requestFile: RequestBody =
            RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)


        //val contentType = "multipart/form-data"
        val contentType = "application/octet-stream"
        // val contentType = "application/json"
        // val response = service.loadImage(body,contentType,subscriptionKey)
        val jsonObject = JsonObject()
        val answersheetURL =
            "https://user-images.githubusercontent.com/63531125/160557552-fdde9276-9c03-45a5-adaa-c6d1809cf24d.jpg"
        val dummyURL =
            "https://www.researchgate.net/publication/334355929/figure/tbl2/AS:778947612639232@1562726991220/List-of-words-used-in-the-experiment-containing-clear-variants-of-the-allophone-l.png"
        // jsonObject.addProperty("url", answersheetURL)
        val response = service.loadImage(body, contentType, subscriptionKey)
        Log.d(TAG, "do_post_file: response:$response")
        if (response.isSuccessful) {
            Log.d(TAG, "do_post_file: success")
            val body = response.body()
            Log.d(TAG, "do_post_file:body: $body")
            Log.d(TAG, "do_post_file:headers: ${response.headers()}")

        } else {
            val body = response.errorBody()!!.string()!!
            Log.d(TAG, "do_post:Error:${body} ")
        }
        val lastIndex = response.headers().get("operation-location")!!.lastIndexOf("/") + 1
        val operationId = response.headers().get("operation-location")!!.substring(lastIndex)
        Log.d(TAG, "do_post: operationId:$operationId")
        return operationId
    }


    fun provideRetrofitService(): OcrService {
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


    private fun setOnClickListener() {
        binding.button.setOnClickListener {
            launchImagePicker()
        }

    }

    private fun launchGallery() {

        launchGalleryWithFragment()
    }

    fun launchGalleryWithFragment() {
        Log.d(TAG, "launchGalleryWithFragment: ")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //  intent.type = "image/*"
        intent.type = "*/*"
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            PICK_IMAGE_REQUEST
        )
    }

    /*    @SuppressLint("NewApi")
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

                val originalFile = getFile(applicationContext, path!!)
                var fileCopy = File.createTempFile("file", ".jpg")
                copy(originalFile, fileCopy)
                *//*     CoroutineScope(Dispatchers.IO).launch {
                     try {

                         // val operationId = do_post_file(originalFile!!)
                         val operationId = do_post_url()
                         do_get(operationId)
                     } catch (e: Exception) {
                         Log.e(TAG, "onActivityResult: Error", e)
                     }
                 }*//*
            CoroutineScope(Dispatchers.IO).launch {
                ReadFromFile(compVisClient, originalFile!!)
            }


        }

    }*/

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        Log.d(TAG, "onActivityResult: ")
        if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(dataIntent)
            Log.d(TAG, "onActivityResult: result:$result")
            if (resultCode === RESULT_OK) {
                val resultUri = result.uri
                Log.d(TAG, "onActivityResult: uri:$resultUri")
                CoroutineScope(Dispatchers.IO).launch {
                    val compVisClient = Authenticate()!!
                  val byteArray=  readBytes(this@MainActivity,resultUri)!!

                   // val originalFile = getFile(applicationContext, resultUri!!)
                   // File.
                    ReadFromFile(compVisClient, File(""),bytes = byteArray)
                }

            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                Log.e(TAG, "onActivityResult: Error", error)
            }
        }


    }
    @Throws(IOException::class)
    private fun readBytes(context: Context, uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }
    private fun getByteArrayFromBitmap(bmp: Bitmap): ByteArray {

        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        bmp.recycle()
        return byteArray
    }


    @Throws(IOException::class)
    fun getFile(context: Context, uri: Uri): File? {
        Log.d(TAG, "getFile: ")
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
        Log.d(TAG, "createFileFromStream: ")
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
        Log.d(TAG, "queryName: ")
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
    private suspend fun ReadFromFile(client: ComputerVisionClient, rawImage: File,bytes: ByteArray=ByteArray(2)) {
        // val TAG = "MainActivity"
        Log.d(TAG, "ReadFromFile: ")
        try {
            val rawImage = File(rawImage.path);
           // val localImageBytes = Files.readAllBytes(rawImage.toPath())
            val localImageBytes = bytes

            // Cast Computer Vision to its implementation to expose the required methods
            val vision = client.computerVision() as ComputerVisionImpl

            // Read in remote image and response header
            val response = vision.readInStreamWithServiceResponseAsync(localImageBytes, null, null)
                .toBlocking()
                .single()
            val responseHeader = response.headers()

            Log.d(TAG, "ReadFromFile: response body:${response.response().raw().body()}")
            Log.d(TAG, "ReadFromFile: response headers:${response.response().raw().headers()}")
            Log.d(TAG, "ReadFromFile: response error_body:${response.response().errorBody()}")

// Extract the operationLocation from the response header
            val operationLocation = response.response().raw().header("operation-location")!!
            Log.d(TAG, "ReadFromFile: header:$responseHeader")
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
            val builder = StringBuilder()
            for (line in pageResult.lines()) {
                builder.append(line.text())
                builder.append("\n")
            }
            Log.d(TAG, "getAndPrintReadResult:\n ${builder.toString()}")
            // android.util.Log.d(TAG, "getAndPrintReadResult: ")(builder.toString())
        }


        analyzeAnswers_From_Azure(readResults!!.analyzeResult().readResults())
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
