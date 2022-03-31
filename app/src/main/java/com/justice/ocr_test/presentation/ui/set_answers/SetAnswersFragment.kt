package com.justice.ocr_test.presentation.ui.set_answers

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.justice.ocr_test.R
import com.justice.ocr_test.databinding.FragmentSetAnswersBinding
import com.justice.ocr_test.presentation.ui.models.Answer
import com.justice.ocr_test.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetAnswersFragment : Fragment(R.layout.fragment_set_answers) {

    companion object {
        private const val TAG = "SetAnswersFragment"
    }

    var teachersAnswers = mutableListOf<Answer>()
    lateinit var sharedPreferences: SharedPreferences

    lateinit var answerAdapter: ExamAnswerAdapter
    lateinit var binding: FragmentSetAnswersBinding
    private val viewModel: SetAnswersViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSetAnswersBinding.bind(view)
        subscribeToObservers()
        viewModel.setEvent(SetAnswersViewModel.Event.FetchTeachersAnswers)
        setOnClickListeners()

        setUpToolBar()
    }

    private fun setUpToolBar() {
        binding.toolbar.root.inflateMenu(R.menu.menu_set_answers)
        binding.toolbar.root.title = "Answers"
        binding.toolbar.root.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.toolbar.root.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.itemMain -> {
                    goToMainScreen()
                }


            }
            true

        }
    }

    private fun subscribeToObservers() {
        lifecycleScope.launchWhenStarted {
            launch {
                viewModel.fetchTeachersAnswersStatus.collect {
                    when (it.status) {
                        Resource.Status.LOADING -> {

                        }
                        Resource.Status.SUCCESS -> {
                            initRecyclerView(it.data!!)
                        }
                        Resource.Status.ERROR -> {
                        }
                    }
                }
            }
            launch {
                viewModel.saveTeachersAnswersStatus.collect {
                    when (it.status) {
                        Resource.Status.LOADING -> {

                        }
                        Resource.Status.SUCCESS -> {
                            goToMainScreen()
                        }
                        Resource.Status.ERROR -> {
                        }
                    }
                }
            }
        }
    }

    private fun setOnClickListeners() {
        binding.btnSubmit.setOnClickListener {
            submitBtnClicked()
        }
    }

    private fun submitBtnClicked() {
        Log.d(TAG, "submitBtnClicked: ")
        viewModel.setEvent(SetAnswersViewModel.Event.SaveTeachersAnswers(answerAdapter.getCurrentList()))

    }

    private fun goToMainScreen() {
        findNavController().navigate(SetAnswersFragmentDirections.actionSetAnswersFragmentToMainFragment())
    }

    private fun initRecyclerView(teachersAnswers: MutableList<Answer>) {
        Log.d(TAG, "initRecyclerView: ")
        teachersAnswers.sortBy {
            it.number
        }

        answerAdapter = ExamAnswerAdapter(teachersAnswers)
        binding.recyclerView.apply {
            setLayoutManager(LinearLayoutManager(requireContext()))
            setHasFixedSize(true)
            setAdapter(answerAdapter)

        }

    }


}