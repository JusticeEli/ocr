package com.justice.ocr_test.presentation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApplicationClass : Application() {
    //check if current user is admin

    private val TAG = "ApplicationClass"
}