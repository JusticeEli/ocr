package com.justice.ocr_test.presentation.ui.main.models

data class AnalyzeResult(
    val readResults: List<ReadResult>,
    val version: String
)