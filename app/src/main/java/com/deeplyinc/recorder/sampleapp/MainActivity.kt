package com.deeplyinc.recorder.sampleapp

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.deeplyinc.recorder.sampleapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        internal const val TAG = "DeeplyRecorder"
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        requestPermission.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            val isGranted = it.value
            when (it.key) {
                Manifest.permission.RECORD_AUDIO -> {
                    if (isGranted) {
                        Log.d(TAG, "Audio recording permission granted")
                    }
                }
                else -> {}
            }
        }
    }
}