package com.justice.ocr_test.utils

import android.content.SharedPreferences

val <T> T.exhaustive: T
    get() = this


fun SharedPreferences.resetTeachersAnswers()=this.edit().clear().commit()