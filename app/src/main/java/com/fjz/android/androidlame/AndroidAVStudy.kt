package com.fjz.android.androidlame

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fjz.android.androidlame.aac.AACRecorderActivity
import com.fjz.android.androidlame.databinding.ActivityAndroidAvstudyBinding
import com.fjz.android.androidlame.mp3.Mp3RecorderActivity

class AndroidAVStudy : AppCompatActivity() {

    private lateinit var mBinding: ActivityAndroidAvstudyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mBinding = ActivityAndroidAvstudyBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
    }

    private fun initViews() {

        mBinding.btnMp3.setOnClickListener {
            startActivity(Intent(this, Mp3RecorderActivity::class.java))
        }

        mBinding.btnAAC.setOnClickListener {
            startActivity(Intent(this, AACRecorderActivity::class.java))
        }

    }
}