package com.fjz.android.androidlame.aac

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission

/*
* Created by Jinzhen Feng on 2026/3/4.
* Copyright (c) 2026 . All rights reserved.
*/
class AACRecorder(
    private val mPath: String,
) {

    companion object {
        private const val TAG = "AAC-REC"
    }
    private var mSampleRate = 44100
    private var mChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private var mBitRate = 128000

    @Volatile
    private var mRunning = false

    private var mAudioRecord: AudioRecord? = null
    private var mCodec: MediaCodec? = null
    private var mEncoderMediaFormat: MediaFormat? = null
    private var mMuxer: MediaMuxer? = null
    private var mTrackIndex = -1
    private var mMuxerStarted = false
    private var mTotalSamples = 0L

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {

        // 获取audio record的最小缓存字节数
        val minBufSize = AudioRecord.getMinBufferSize(
            mSampleRate,
            mChannelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 设置采集的pcm格式
        var format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(mSampleRate)
            .setChannelMask(mChannelConfig)
            .build()

        // 创建一个audiorecord实例
        mAudioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufSize)
            .build()

        // 配置编码器
        mEncoderMediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            mSampleRate,
            1
        )
        mEncoderMediaFormat?.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        mEncoderMediaFormat?.setInteger(
            MediaFormat.KEY_BIT_RATE,
            mBitRate
        )
        mEncoderMediaFormat?.setInteger(
            MediaFormat.KEY_MAX_INPUT_SIZE,
            minBufSize
        )

        mCodec = MediaCodec.createEncoderByType(
            MediaFormat.MIMETYPE_AUDIO_AAC
        )
        mCodec?.configure(
            mEncoderMediaFormat, null, null,
            MediaCodec.CONFIGURE_FLAG_ENCODE)

        mCodec?.setCallback(mEncoderCallback)
        mCodec?.start()

        // 创建MediaMuxer
        mMuxer = MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        mAudioRecord?.startRecording()
        mRunning = true


    }


    private val mEncoderCallback: MediaCodec.Callback = object: MediaCodec.Callback() {
        override fun onError(
            codec: MediaCodec,
            e: MediaCodec.CodecException
        ) {
            Log.i(TAG, "MediaCodec Error: ${e.message}")
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            val inputBuffer = codec.getInputBuffer(index) ?: return
            inputBuffer.clear()

            // 1. 如果用户已经调用了 stop()，则发送 EOS 标记
            if (!mRunning) {
                codec.queueInputBuffer(
                    index, 0, 0, 0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                return
            }

            // 2. 正常读取数据
            val read = mAudioRecord?.read(inputBuffer, inputBuffer.capacity()) ?: 0

            if (read > 0) {
                // 正常入队
                val pts = mTotalSamples * 1_000_000L / mSampleRate
                mTotalSamples += read / 2
                codec.queueInputBuffer(index, 0, read, pts, 0)
            } else if (read < 0) {
                // 3. 处理错误（例如 log 出错误码），而不是当做 EOS 处理
                Log.e( TAG, "AudioRecord read error: $read")
            }
            // 如果 read == 0，通常什么都不做，等待下一次回调即可
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            val outputBuffer = codec.getOutputBuffer(index) ?: return

            if (info.size > 0 && mMuxerStarted) {
                outputBuffer.position(info.offset)
                outputBuffer.limit(info.offset + info.size)
                mMuxer?.writeSampleData(mTrackIndex, outputBuffer, info)
            }

            codec.releaseOutputBuffer(index, false)

            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                stopInternal()
            }
        }

        override fun onOutputFormatChanged(
            codec: MediaCodec,
            format: MediaFormat
        ) {
            mTrackIndex = mMuxer?.addTrack(format) ?: -1
            mMuxer?.start()
            mMuxerStarted = true
        }

    }


    fun stop() {
        mRunning = false
    }

    private fun stopInternal() {

        try {
            mAudioRecord?.stop()
            mAudioRecord?.release()

            mCodec?.stop()
            mCodec?.release()

            if (mMuxerStarted) {
                mMuxer?.stop()
                mMuxer?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
