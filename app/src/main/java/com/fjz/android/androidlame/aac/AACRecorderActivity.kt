package com.fjz.android.androidlame.aac

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fjz.android.androidlame.R
import com.fjz.android.androidlame.databinding.ActivityAacrecorderBinding
import com.fjz.android.androidlame.databinding.ActivityMainBinding
import java.io.File

class AACRecorderActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityAacrecorderBinding

    private var mRecorder: AACRecorder? = null

    private var mPlayer: MediaPlayer? = null


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (audioGranted) {
            startRecording()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivityAacrecorderBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mBinding.btnStart.setOnClickListener @RequiresPermission(Manifest.permission.RECORD_AUDIO){

            checkPermissionsAndToggle()
        }

        mBinding.btnStop.setOnClickListener {
            mRecorder?.stop()
        }

        mBinding.btnPlay.setOnClickListener {

            mPlayer = MediaPlayer()
            mPlayer?.setDataSource(mBinding.tvPath.text?.toString())
            mPlayer?.prepare()
            mPlayer?.start()


        }

    }

    private fun checkPermissionsAndToggle() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startRecording()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startRecording() {

        if (mRecorder == null) {
            val path = File(getExternalFilesDir("record"), "test.m4a").apply {
                if (!exists()) {
                    createNewFile()
                }
            }.absolutePath
            mRecorder = AACRecorder(path)

            mBinding.tvPath.text = path

            mRecorder?.start()
        }
    }
}