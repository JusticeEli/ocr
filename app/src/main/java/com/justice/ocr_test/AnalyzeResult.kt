package com.justice.ocr_test

data class AnalyzeResult(
    val readResults: List<ReadResult>,
    val version: String
)