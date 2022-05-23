package com.justice.ocr_test.presentation.ui.set_answers

import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.justice.ocr_test.presentation.ui.models.Answer
import com.justice.ocr_test.utils.Constants
import com.justice.ocr_test.utils.Resource
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.lang.reflect.Type


class SetAnswersViewModel @ViewModelInject constructor(private val sharedPreferences: SharedPreferences) :
    ViewModel() {

    private val TAG = "SetAnswersViewModel"

    fun setEvent(event: Event) {
        viewModelScope.launch {
            when (event) {
                Event.FetchTeachersAnswers -> {
                    fetchTeachersAnswers()
                }
                is Event.SaveTeachersAnswers -> {
                    saveTeachersAnswers(event.teachersAnswers)
                }
            }
        }
    }

    private val _fetchTeachersAnswersStatus = Channel<Resource<MutableList<Answer>>>()
    val fetchTeachersAnswersStatus = _fetchTeachersAnswersStatus.receiveAsFlow()
    private suspend fun fetchTeachersAnswers() {
        val teachersAnswers = fetchAnswerFromSharedPref();
        _fetchTeachersAnswersStatus.send(Resource.success(teachersAnswers))

    }

    fun fetchAnswerFromSharedPref(): MutableList<Answer> {
        Log.d(TAG, "fetchAnswerFromSharedPref: ")
        var teachersAnswers = mutableListOf<Answer>()
        val gson = Gson()
        val json: String? = sharedPreferences.getString(Constants.KEY_ANSWERS, null)
        if (json == null) {
            Log.d(TAG, "fetchAnswerFromSharedPref:default answers failed")
            for (i in 1..50) {
                val answer = Answer()
                answer.choice = "A"
                answer.number = i
                teachersAnswers.add(answer)
            }
        } else {
            Log.d(TAG, "fetchAnswerFromSharedPref: default answers success were found")
            val type: Type = object : TypeToken<ArrayList<Answer?>?>() {}.getType()
            teachersAnswers = gson.fromJson(json, type)
        }
        Log.d(TAG, "fetchAnswerFromSharedPref:teachersAnswers:$teachersAnswers ")
        return teachersAnswers

    }

    private val _saveTeachersAnswersStatus = Channel<Resource<Boolean>>()
    val saveTeachersAnswersStatus = _saveTeachersAnswersStatus.receiveAsFlow()
    private suspend fun saveTeachersAnswers(teachersAnswers: List<Answer>) {
        saveAnswersToSharedPref(teachersAnswers)
        _saveTeachersAnswersStatus.send(Resource.success(true))

    }

    fun saveAnswersToSharedPref(teachersAnswers: List<Answer>) {
        Log.d(TAG, "saveAnswersToSharedPref:teachersAnswers:$teachersAnswers ")
        val gson = Gson()
        val json = gson.toJson(teachersAnswers)
        sharedPreferences.edit().putString(Constants.KEY_ANSWERS, json).apply()
    }

    sealed class Event {
        object FetchTeachersAnswers : Event()
        data class SaveTeachersAnswers(val teachersAnswers: List<Answer>) : Event()
    }
}