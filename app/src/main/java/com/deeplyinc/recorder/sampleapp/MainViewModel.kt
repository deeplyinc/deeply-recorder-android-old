package com.deeplyinc.recorder.sampleapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deeplyinc.recorder.DeeplyRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    private val recorder = DeeplyRecorder(sampleRate = 16000, bufferSize = 48000)

    init {
        startRecording()
    }

    private fun startRecording() {
        if (recorder.isRecording()) return
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return

        viewModelScope.launch(Dispatchers.Default) {
            recorder.start().collect { audioSamples ->
                Log.d(MainActivity.TAG, "${audioSamples.size} audio samples recorded")
            }
        }
    }

    private fun stopRecording() {
        if (recorder.isRecording()) {
            recorder.stop()
        }
    }
}