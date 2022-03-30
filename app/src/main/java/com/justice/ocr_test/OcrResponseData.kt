package com.justice.ocr_test

data class OcrResponseData(
    val analyzeResult: AnalyzeResult,
    val createdDateTime: String,
    val lastUpdatedDateTime: String,
    val status: String
)