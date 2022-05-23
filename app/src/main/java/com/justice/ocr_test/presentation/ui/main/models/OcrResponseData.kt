package com.justice.ocr_test.presentation.ui.main.models

import com.justice.ocr_test.presentation.ui.main.models.AnalyzeResult

data class OcrResponseData(
    val analyzeResult: AnalyzeResult,
    val createdDateTime: String,
    val lastUpdatedDateTime: String,
    val status: String
)