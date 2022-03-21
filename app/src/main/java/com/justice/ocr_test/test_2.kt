package com.justice.ocr_test

import android.os.Build
import androidx.annotation.RequiresApi
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVision
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient
import com.microsoft.azure.cognitiveservices.vision.computervision.implementation.ComputerVisionImpl
import com.microsoft.azure.cognitiveservices.vision.computervision.models.*
import java.io.File
import java.nio.file.Files
import java.util.*

 object test_2 {

    /**
     * OCR with READ : Performs a Read Operation on a local image
     * @param client instantiated vision client
     * // * @param localFilePath local file path from which to perform the read operation against
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private   fun ReadFromFile(client: ComputerVisionClient, rawImage: File) {
        println("-----------------------------------------------")
        val localFilePath = "src\\main\\resources\\myImage.png"
        println("Read with local file: $localFilePath")
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
            val operationLocation = responseHeader.operationLocation()
            println("Operation Location:$operationLocation")
            getAndPrintReadResult(vision, operationLocation)
        } catch (e: Exception) {
            println(e.message)
            e.printStackTrace()
        }
    }


    /**
     * Polls for Read result and prints results to console
     * @param vision Computer Vision instance
     * @return operationLocation returned in the POST Read response header
     */
    @Throws(InterruptedException::class)
    private fun getAndPrintReadResult(vision: ComputerVision, operationLocation: String) {
        println("Polling for Read results ...")

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
            println("")
            println("Printing Read results for page " + pageResult.page())
            val builder = StringBuilder()
            for (line in pageResult.lines()) {
                builder.append(line.text())
                builder.append("\n")
            }
            println(builder.toString())
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
}