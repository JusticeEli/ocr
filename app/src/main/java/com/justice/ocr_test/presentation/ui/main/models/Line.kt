package com.justice.ocr_test.presentation.ui.main.models

data class Line(
    val appearance: Appearance,
    val boundingBox: List<Float>,
    val text: String,
    val words: List<Word>
)