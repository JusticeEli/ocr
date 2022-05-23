package com.justice.ocr_test.presentation.ui.main.models

data class Word(
    val boundingBox: List<Float>,
    val confidence: Double,
    val text: String

)