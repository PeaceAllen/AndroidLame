package com.fjz.android.androidlame

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fjz.android.mylibrary.LameUtil
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class RecordingService : Service() {

    companion object {
        private const val TAG = "RECORD-SERVICE"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "RecordingChannel"
        private const val NOTIFICATION_ID = 1
        
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128
        private const val QUALITY = 2
    }

    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    private val lameUtil = LameUtil()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (isRecording.get()) return
        isRecording.set(true)
        
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording MP3")
            .setContentText("Recording in progress...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        recordingThread = Thread {
            doRecording()
        }.apply { start() }
    }

    private fun doRecording() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            isRecording.set(false)
            return
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord state not initialized")
            isRecording.set(false)
            return
        }

        val outputDir = getExternalFilesDir(null)
        val mp3File = File(outputDir, "recorded_${System.currentTimeMillis()}.mp3")
        val outputStream = FileOutputStream(mp3File)

        lameUtil.init(SAMPLE_RATE, 1, SAMPLE_RATE, BIT_RATE, QUALITY)

        val pcmBuffer = ShortArray(minBufferSize)
        val mp3Buffer = ByteArray((minBufferSize * 1.25 + 7200).toInt())

        audioRecord.startRecording()
        Log.i(TAG, "Start recording to ${mp3File.absolutePath}")

        try {
            while (isRecording.get()) {
                val readSize = audioRecord.read(pcmBuffer, 0, minBufferSize)
                if (readSize > 0) {
                    val encodedSize = lameUtil.encode(pcmBuffer, null, readSize, mp3Buffer)
                    if (encodedSize > 0) {
                        outputStream.write(mp3Buffer, 0, encodedSize)
                    }
                }
            }
            
            val flushSize = lameUtil.flush(mp3Buffer)
            if (flushSize > 0) {
                outputStream.write(mp3Buffer, 0, flushSize)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording: ${e.message}")
        } finally {
            try {
                audioRecord.stop()
                audioRecord.release()
                outputStream.close()
                lameUtil.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val intent = Intent("RECORDING_FINISHED").apply {
                setPackage(packageName)
                putExtra("filePath", mp3File.absolutePath)
            }
            sendBroadcast(intent)

            Log.i(TAG, "Recording finished: path = ${mp3File.absolutePath}")
        }
    }

    private fun stopRecording() {
        isRecording.set(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
