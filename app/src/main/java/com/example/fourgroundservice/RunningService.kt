package com.example.fourgroundservice

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*

class RunningService : Service() {

    private var isServiceRunning = false

    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var soundRunnable: Runnable
    private var isCompleted = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> start("16/12/03")
            Actions.STOP.toString() -> stopService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(date: String) {
        if (isServiceRunning) return

        val notification = Notification.Builder(this, "running_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Foreground Service is Active")
            .setContentText("Playing sound based on date pattern")
            .build()

        startForeground(1, notification)

        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).also {
            startSound(date)
        }

        isServiceRunning = true
    }

    private fun startSound(date: String) {
        val parts = date.split("/")
        if (parts.size != 3) {
            stopService()
            return
        }

        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()

        val dateSequence = mutableListOf<Pair<Int, Int>>()

        dateSequence.addAll(generateBeepPattern(day))
        dateSequence.add(Pair(2000, 0))

        dateSequence.addAll(generateBeepPattern(month))
        dateSequence.add(Pair(2000, 0))

        dateSequence.addAll(generateBeepPattern(year))

        var currentIndex = 0

        soundRunnable = object : Runnable {
            override fun run() {
                if (isCompleted) return

                if (currentIndex >= dateSequence.size) {
                    isCompleted = true
                    return
                }

                val (countOrDuration, beepDuration) = dateSequence[currentIndex]
                if (beepDuration == 0) {
                    handler.postDelayed(this, countOrDuration.toLong())
                } else {
                    repeat(countOrDuration) {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, beepDuration)
                        Thread.sleep(beepDuration.toLong())
                    }
                    handler.postDelayed(this, 1000)
                }

                currentIndex++
            }
        }

        handler.post(soundRunnable)
    }

    private fun generateBeepPattern(number: Int): List<Pair<Int, Int>> {
        val beepPattern = mutableListOf<Pair<Int, Int>>()

        number.toString().forEach { digit ->
            val beepCount = digit.toString().toInt()
            beepPattern.add(Pair(beepCount, 500))
            beepPattern.add(Pair(1000, 0))
        }

        return beepPattern
    }

    private fun stopService() {
        handler.removeCallbacks(soundRunnable)
        toneGenerator?.release()
        toneGenerator = null
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(soundRunnable)
        toneGenerator?.release()
    }

    enum class Actions {
        START, STOP
    }
}
