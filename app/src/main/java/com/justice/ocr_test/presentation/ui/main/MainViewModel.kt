package com.justice.ocr_test.presentation.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.justice.ocr_test.utils.Constants
import com.justice.ocr_test.presentation.ui.models.Answer
import com.justice.ocr_test.utils.Resource
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVision
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient
import com.microsoft.azure.cognitiveservices.vision.computervision.implementation.ComputerVisionImpl
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OperationStatusCodes
import com.microsoft.azure.cognitiveservices.vision.computervision.models.ReadOperationResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.reflect.Type
import java.util.*

class MainViewModel @ViewModelInject constructor(
    private val computerVisionClient: ComputerVisionClient,
    private val sharedPreferences: SharedPreferences
) :
    ViewModel() {


    private val TAG = "MainViewModel"
    private val DELAY_TIME_IN_MILLIS = 60_000L //1 minutes
    fun setEvent(event: MainViewModel.Event) {
        viewModelScope.launch {
            when (event) {
                is Event.ImageReceived -> {
                    imageReceived(event.context, event.uri)
                }
            }
        }
    }

    private val _imageReceivedeStatus = Channel<Resource<String>>()
    val imageReceivedeStatus = _imageReceivedeStatus.receiveAsFlow()

    private suspend fun imageReceived(context: Context, uri: Uri) {
        _imageReceivedeStatus.send(Resource.loading(""))
        val byteArray = readBytesFromImageUri(context, uri)!!
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            readFromFile(byteArray)

        }else{
            val message="imageReceived: phone version is lower than OREO"
            val e=Exception(message)
            Log.e(TAG, "imageReceived: ",e )
            _imageReceivedeStatus.send(Resource.error(e))
        }

    }


    @Throws(IOException::class)
    private fun readBytesFromImageUri(context: Context, uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }

    /**
     * OCR with READ : Performs a Read Operation on a local image
     * @param client instantiated vision client
     * // * @param localFilePath local file path from which to perform the read operation against
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private suspend fun readFromFile(byteArray: ByteArray) {
        Log.d(TAG, "ReadFromFile: ")
        try {
            // val localImageBytes = Files.readAllBytes(rawImage.toPath())
            val localImageBytes = byteArray

            // Cast Computer Vision to its implementation to expose the required methods
            val vision = computerVisionClient.computerVision() as ComputerVisionImpl

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
            getAnalyzedResultsFromOperationLocation(vision, operationLocation)
        } catch (e: Exception) {
            Log.e(TAG, "ReadFromFile: Error", e)
            _imageReceivedeStatus.send(Resource.error(e))
        }
    }

    @Throws(InterruptedException::class)
    private suspend fun getAnalyzedResultsFromOperationLocation(
        vision: ComputerVision,
        operationLocation: String
    ) {
        Log.d(TAG, "Polling for Read results ...")
        // Extract OperationId from Operation Location
        val operationId = extractOperationIdFromOpLocation(operationLocation)
        var pollForResult = true
        var readResults: ReadOperationResult? = null
        while (pollForResult) {
            // Poll for result every second
            delay(DELAY_TIME_IN_MILLIS)
            readResults = vision.getReadResult(UUID.fromString(operationId))

            // The results will no longer be null when the service has finished processing the request.
            if (readResults != null) {
                // Get request status
                val status = readResults.status()
                if (status == OperationStatusCodes.FAILED) {
                    val message =
                        "getAnalyzedResultsFromOperationLocation: Error:Failed to get data from operationLocation:$operationId"
                    val exception = Exception(message)
                    Log.e(TAG, "getAnalyzedResultsFromOperationLocation: Error", exception)
                    _imageReceivedeStatus.send(Resource.error(exception))
                    return
                }
                if (status == OperationStatusCodes.SUCCEEDED) {
                    pollForResult = false
                }
            }
        }
        analyzeAnswers_From_Azure(readResults!!.analyzeResult().readResults())
    }

    private fun extractOperationIdFromOpLocation(operationLocation: String): String {
        val lastIndex = operationLocation.lastIndexOf("/") + 1
        val operationId = operationLocation.substring(lastIndex)
        Log.d(TAG, "do_post: operationId:$operationId")
        return operationId
    }

    private suspend fun analyzeAnswers_From_Azure(readResults: List<com.microsoft.azure.cognitiveservices.vision.computervision.models.ReadResult>) {
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

    private suspend fun startTheMarkingProcess(studentsAnswers: MutableList<Answer>) {
        Log.d(TAG, "startTheMarkingProcess: size:${studentsAnswers.size}")
        Log.d(TAG, "startTheMarkingProcess: studentsAnswers:$studentsAnswers")
        val teachersAnswers = fetchAnswerFromSharedPref()
        studentsAnswers.retainAll(teachersAnswers)
        Log.d(TAG, "startTheMarkingProcess: marks size:${studentsAnswers.size}")
        val message = "Total marks is ${studentsAnswers.size}/50"
        _imageReceivedeStatus.send(Resource.success(message))
    }


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

    sealed class Event {
        data class ImageReceived(val context: Context, val uri: Uri) : Event()
        object SetLocationClicked : Event()
    }

}