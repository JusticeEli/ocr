package com.justice.ocr_test.presentation.ui.main.models

data class ReadResult(
    val angle: Float,
    val height: Float,
    val lines: List<Line>,
    val page: Float,
    val unit: String,
    val width: Float
)