package com.justice.ocr_test

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.justice.ocr_test.Constants.KEY_ANSWERS
import com.justice.ocr_test.Constants.SHARED_PREF
import com.justice.ocr_test.databinding.ActivitySetAnswersBinding
import java.lang.reflect.Type

class SetAnswersActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SetAnswersFragment"
    }

    var teachersAnswers = mutableListOf<Answer>()
    lateinit var sharedPreferences: SharedPreferences

    lateinit var answerAdapter: ExamAnswerAdapter
    lateinit var binding: ActivitySetAnswersBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences =
            getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)

        fetchAnswerFromSharedPref()
        initRecyclerView()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.submitBtn.setOnClickListener {
            submitBtnClicked()
        }
    }

    private fun submitBtnClicked() {
        Log.d(TAG, "submitBtnClicked: ")
        saveAnswersToSharedPref()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

    }

    private fun initRecyclerView() {
        Log.d(TAG, "initRecyclerView: ")
        teachersAnswers.sortBy {
            it.number
        }

        answerAdapter = ExamAnswerAdapter(teachersAnswers)
        binding.recyclerView.apply {
            setLayoutManager(LinearLayoutManager(this@SetAnswersActivity))
            setHasFixedSize(true)
            setAdapter(answerAdapter)

        }

    }


    fun saveAnswersToSharedPref() {
        Log.d(TAG, "saveAnswersToSharedPref:teachersAnswers:$teachersAnswers ")
        val gson = Gson()
        val json = gson.toJson(teachersAnswers)

        sharedPreferences.edit().putString(KEY_ANSWERS, json).apply()

    }

    fun fetchAnswerFromSharedPref() {
        Log.d(TAG, "fetchAnswerFromSharedPref: ")

        val gson = Gson()
        val json: String? = sharedPreferences.getString(KEY_ANSWERS, null)
        if (json == null) {
            Log.d(TAG, "fetchAnswerFromSharedPref:default answers failed")
            teachersAnswers.clear()
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

    }

}