package com.example

import android.app.Application
import com.example.receiver.ReminderReceiver
import com.example.service.VoiceSpeaker

class MediVoiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.createNotificationChannel(this)
        VoiceSpeaker.getInstance(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        VoiceSpeaker.getInstance(this).shutdown()
    }
}
