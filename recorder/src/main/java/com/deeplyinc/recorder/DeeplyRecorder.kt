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
import java.io.UnsupportedEncodingException

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
     * Audio format is strongly coupled with the type of buffer.
     * ex) AudioFormat.ENCODING_PCM_16BIT -> ShortArray
     * ex) AudioFormat.ENCODING_PCM_FLOAT -> FloatArray
     * Currently we only support ENCODING_PCM_16BIT. If you need floating point audio samples,
     * you need to scale the short values.
     */
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    /**
     * Specify the buffer size of the recorder, which is the number of audio samples recorded at
     * one time. If the given size is less than the minimum size of the buffer, it is set to the
     * (2 * minimum size of the buffer). Default value is (2 * AudioRecord.getMinBufferSize())
     */
    bufferSize: Int = 2 * AudioRecord.getMinBufferSize(
        sampleRate,
        channel,
        audioFormat
    )
) {
    init {
        require(bufferSize > AudioRecord.getMinBufferSize(
            sampleRate,
            channel,
            audioFormat,
        ))
    }


    /**
     * A buffer for storing audio samples recorded from microphone.
     */
    private var buffer: FloatArray = FloatArray(bufferSize)
    private var bufferInShort: ShortArray = ShortArray(bufferSize)

    private var audioRecorder: AudioRecord? = null
    private var run = false

    /**
     * Start recording, and emit the recorded audio samples. Note that RECORD_AUDIO permission must
     * be granted before starting the recording.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Flow<FloatArray> = flow {
        audioRecorder = when (audioFormat) {
            AudioFormat.ENCODING_PCM_FLOAT -> AudioRecord(audioSource, sampleRate, channel, audioFormat, buffer.size)
            AudioFormat.ENCODING_PCM_16BIT -> AudioRecord(audioSource, sampleRate, channel, audioFormat, bufferInShort.size)
            else -> throw UnsupportedEncodingException()
        }
        if (audioRecorder?.state == AudioRecord.STATE_UNINITIALIZED) {
            throw Exception("Failed to initialize AudioRecord")
        }
        audioRecorder?.let {
            it.startRecording()
            if (it.state == AudioRecord.STATE_UNINITIALIZED) {
                throw IllegalStateException("Failed to initialize AudioRecord")
            }
            run = true
            while (run) {
                when (audioFormat) {
                    AudioFormat.ENCODING_PCM_FLOAT -> {
                        it.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    }
                    AudioFormat.ENCODING_PCM_16BIT -> {
                        it.read(bufferInShort, 0, bufferInShort.size, AudioRecord.READ_BLOCKING)
                        convertToFloat(bufferInShort, buffer)
                    }
                    else -> throw UnsupportedEncodingException()
                }
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
    fun getBufferSize(): Int = bufferInShort.size

    /**
     * Convert [-32768, 32767] 'short' type data to [-1.0, 1.0] 'float' type data
     * Warning: this method OVERWRITE the contents of floatArray.
     */
    private fun convertToFloat(from: ShortArray, to: FloatArray) {
        require(from.size == to.size)
        val size = from.size
        for (i in 0 until size) {
            to[i] = from[i] / 32768.0F
        }
    }

    companion object {
        const val TAG = "DeeplyRecorder"
    }
}