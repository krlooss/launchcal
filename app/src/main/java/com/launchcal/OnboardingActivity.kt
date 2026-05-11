package com.launchcal

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        findViewById<TextView>(R.id.onboardingDismiss).setOnClickListener {
            getSharedPreferences("launcher", MODE_PRIVATE)
                .edit().putBoolean("onboarding_done", true).apply()
            finish()
        }
    }
}
