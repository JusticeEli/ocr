package com.justice.ocr_test

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.justice.ocr_test.databinding.FragmentMainBinding
import com.justice.ocr_test.presentation.ui.main.MainViewModel
import com.justice.ocr_test.utils.Resource
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*


@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private val TAG = "MainFragment"
    private val viewModel: MainViewModel by viewModels()
    lateinit var binding: FragmentMainBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)
        setHasOptionsMenu(true)
        subscribeToObservers()
        setOnClickListener()
        setUpToolBar()
    }

    private fun setUpToolBar() {
        binding.toolbar.root.inflateMenu(R.menu.menu_main)
        binding.toolbar.root.title = "Main"
        binding.toolbar.root.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.toolbar.root.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.itemSetAnswers -> {
                    goToSetAnswersScreen()
                }
                R.id.itemRetry -> {
                    retry()
                }
                R.id.itemExit -> {
                    requireActivity().finish()
                }


            }
            true

        }
    }

    fun retry() =
        viewModel.setEvent(
            MainViewModel.Event.ImageReceived(
                requireContext(),
                viewModel.imageUriLiveData.value!!
            )
        )


    private fun goToSetAnswersScreen() {
        findNavController().navigate(MainFragmentDirections.actionGlobalSetAnswersFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)


    }

    private fun subscribeToObservers() {

        lifecycleScope.launchWhenStarted {

            launch {
                viewModel.imageReceivedeStatus.collect {
                    when (it.status) {
                        Resource.Status.LOADING -> {
                            showProgress(true)

                        }
                        Resource.Status.SUCCESS -> {
                            showProgress(false)
                            showToastInfo(it.data!!.message)
                            updateLabel(it.data!!)
                        }
                        Resource.Status.ERROR -> {
                            showProgress(false)
                            showToastInfo("Error: ${it.exception?.message}")
                            Log.e(TAG, "subscribeToObservers: ", it.exception)
                        }
                    }
                }
            }
        }
    }

    private fun updateLabel(data: MainViewModel.Result) {
        Log.d(TAG, "updateLabel: data:$data")
        binding.tvMarks.text = data.message
        binding.tvTime.text = data.timeTakenToAnalyse

    }


    private fun showToastInfo(message: String) {
        Toasty.info(requireContext(), message).show()
    }

    private fun showProgress(visible: Boolean) {
        Log.d(TAG, "showProgress:$visible ")
        binding.progressBar.isVisible = visible
    }


    private fun launchImagePicker() {
        Log.d(TAG, "launchImagePicker: ")
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(requireContext(), this);
    }


    private fun setOnClickListener() {
        binding.btnChoosePhoto.setOnClickListener {
            launchImagePicker()
        }

    }


    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        Log.d(TAG, "onActivityResult: ")
        if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(dataIntent)
            Log.d(TAG, "onActivityResult: result:$result")
            if (resultCode === RESULT_OK) {
                val uri = result.uri
                Log.d(TAG, "onActivityResult: uri:$uri")
                binding.ivChoosenImage.setImageURI(uri)
                viewModel.setImageUri(uri)
                viewModel.setEvent(MainViewModel.Event.ImageReceived(requireContext(), uri!!))
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                Log.e(TAG, "onActivityResult: Error", error)
            }
        }


    }


}
