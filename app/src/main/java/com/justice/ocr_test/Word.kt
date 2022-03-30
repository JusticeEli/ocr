package com.justice.ocr_test

data class Word(
    val boundingBox: List<Float>,
    val confidence: Double,
    val text: String

)