package com.example.videoplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.videoplayer.VideoUtils.PLAYBACK_CHANNEL_ID
import com.example.videoplayer.databinding.ActivityMainBinding
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.Util

class MyVideoPlayerActivity: AppCompatActivity() {
    private var mBkgService: PlayerBkgService? = null
    private var mBound = false

    private var player: Player? = null
    private lateinit var playerView: StyledPlayerView
    private lateinit var binding: ActivityMainBinding

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.d(TAG, "onServiceConnected")
            val binder = iBinder as PlayerBkgService.LocalBinder
            mBkgService = binder.bkgService
            mBound = true
            initializePlayer()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "onServiceDisconnected")
            mBound = false
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        val intent = Intent(this, PlayerBkgService::class.java)
        intent.action = "START_FOREGROUND_ACTION"
        Util.startForegroundService(this, intent)

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        initializePlayer()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        if (mBound) {
            unbindService(serviceConnection)
            mBound = false
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        playerView = binding.playerView
        createNotification()

        setContentView(binding.root)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        val stopIntent = Intent(this, PlayerBkgService::class.java)
        stopIntent.action = "STOP_FOREGROUND_ACTION"
        startService(stopIntent)
        super.onDestroy()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(PLAYBACK_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun initializePlayer() {
        Log.d(TAG, "initializePlayer")
        if (mBound) {
            Log.d(TAG, "is Bound, setting the player view")
            player = mBkgService?.playerInstance
            // Bind the player to the view.
            playerView.player = player
        }
    }

    companion object {
        private const val TAG = "MyVideoPlayerActivity"
    }
}