package com.justice.ocr_test

data class Line(
    val appearance: Appearance,
    val boundingBox: List<Float>,
    val text: String,
    val words: List<Word>
)