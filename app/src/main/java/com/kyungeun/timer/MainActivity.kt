package com.kyungeun.timer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kyungeun.timer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isTimerRunning = false
    private lateinit var statusReceiver: BroadcastReceiver
    private lateinit var timeReceiver: BroadcastReceiver

    private var isResetCheck = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            if (isTimerRunning) pauseTimer() else startTimer()
        }

        binding.btnReset.setOnClickListener {
            resetTimer()
        }
    }

    override fun onStart() {
        super.onStart()
        moveToBackground()
    }

    override fun onResume() {
        super.onResume()

        getTimerStatus()

        val statusFilter = IntentFilter()
        statusFilter.addAction(TimerService.TIMER_STATUS)
        statusReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(p0: Context?, p1: Intent?) {
                val isRunning = p1?.getBooleanExtra(TimerService.IS_TIMER_RUNNING, false)!!
                isTimerRunning = isRunning
                val timeElapsed = p1.getIntExtra(TimerService.TIME_ELAPSED, 0)

                updateLayout(isTimerRunning)
                updateTimerLayout(timeElapsed)
            }
        }
        registerReceiver(statusReceiver, statusFilter)

        val timeFilter = IntentFilter()
        timeFilter.addAction(TimerService.TIMER_TICK)
        timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val timeElapsed = p1?.getIntExtra(TimerService.TIME_ELAPSED, 0)!!
                updateTimerLayout(timeElapsed)
            }
        }
        registerReceiver(timeReceiver, timeFilter)
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(statusReceiver)
        unregisterReceiver(timeReceiver)

        moveToForeground()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimerLayout(timeElapsed: Int) {
        val hours = timeElapsed % 86400 / 3600
        val minutes = timeElapsed % 86400 % 3600 / 60
        val seconds = timeElapsed % 86400 % 3600 % 60
        binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateLayout(isTimerRunning: Boolean) {
        if (isTimerRunning) {
            binding.btnStart.icon =
                ContextCompat.getDrawable(this, R.drawable.ic_pause)
            binding.btnReset.visibility = View.INVISIBLE
        } else {
            binding.btnStart.icon =
                ContextCompat.getDrawable(this, R.drawable.ic_play)
            binding.btnReset.visibility = View.VISIBLE
        }
    }

    private fun getTimerStatus() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.GET_STATUS)
        startService(timerService)
    }

    private fun startTimer() {
        isResetCheck = false
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.START)
        startService(timerService)
    }

    private fun pauseTimer() {
        isResetCheck = false
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.PAUSE)
        startService(timerService)
    }

    private fun resetTimer() {
        isResetCheck = true
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.RESET)
        startService(timerService)
    }

    private fun moveToForeground() {
        if (!isResetCheck) {
            val timerService = Intent(this, TimerService::class.java)
            timerService.putExtra(TimerService.TIMER_ACTION, TimerService.MOVE_TO_FOREGROUND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(timerService)
            } else {
                startService(timerService)
            }
        }
    }

    private fun moveToBackground() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.MOVE_TO_BACKGROUND)
        startService(timerService)
    }
}
