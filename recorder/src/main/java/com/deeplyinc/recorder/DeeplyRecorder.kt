package com.deeplyinc.recorder

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * An audio recorder wrapping AudioRecord to support Kotlin flow.
 */
class DeeplyRecorder(
    /** Specify the source of audio, and default value is MediaRecorder.AudioSource.MIC */
    private val audioSource: Int = MediaRecorder.AudioSource.MIC,
    /** Specify the sampling rate of audio recording, and default value is 16000. */
    private val sampleRate: Int = 16000,
    /** Specify the recording channel, and default value is AudioFormat.CHANNEL_IN_MONO */
    private val channel: Int = AudioFormat.CHANNEL_IN_MONO,
    /**
     * Specify the buffer size of the recorder, which is the number of audio samples recorded at
     * one time. If the given size is less than the minimum size of the buffer, it is set to the
     * minimum size of the buffer. Default value is AudioRecord.getMinBufferSize()
     */
    bufferSize: Int? = null
) {
    /**
     * Audio format is strongly coupled with the type of buffer.
     * ex) AudioFormat.ENCODING_PCM_16BIT -> ShortArray
     * ex) AudioFormat.ENCODING_PCM_FLOAT -> FloatArray
     * Currently we only support ENCODING_PCM_16BIT. If you need floating point audio samples,
     * you need to scale the short values.
     */
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    /**
     * A buffer for storing audio samples recorded from microphone.
     */
    private var buffer: ShortArray = ShortArray(
        bufferSize ?: AudioRecord.getMinBufferSize(
            sampleRate,
            channel,
            audioFormat
        )
    )

    private var audioRecorder: AudioRecord? = null
    private var run = false

    /**
     * Start recording, and emit the recorded audio samples. Note that RECORD_AUDIO permission must
     * be granted before starting the recording.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Flow<ShortArray> = flow {
        audioRecorder = AudioRecord(audioSource, sampleRate, channel, audioFormat, buffer.size)
        audioRecorder?.let {
            it.startRecording()
            run = true
            while (run) {
                it.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                emit(buffer)
            }
            it.stop()
            it.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stop recording.
     */
    fun stop() {
        run = false
    }

    /**
     * @return true if currently recording, otherwise false.
     */
    fun isRecording() = audioRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    /**
     * @return Audio buffer size, which is the number of audio samples recorded at one time.
     */
    fun getBufferSize(): Int = buffer.size
}