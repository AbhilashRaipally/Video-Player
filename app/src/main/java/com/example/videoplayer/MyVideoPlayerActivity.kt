package com.example.videoplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
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

    private var videoPosition:Long = 0L
    var isInPipMode:Boolean = false
    var isPIPModeeEnabled:Boolean = true //Has the user disabled PIP mode in AppOpps?

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

    //Called when the user touches the Home or Recents button to leave the app.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPIPMode()
    }

    override fun onBackPressed(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && isPIPModeeEnabled) {
            enterPIPMode()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if(newConfig !=null){
            videoPosition = player?.currentPosition?:0
            isInPipMode = !isInPictureInPictureMode
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun checkPIPPermission(){
        isPIPModeeEnabled = isInPictureInPictureMode
        if(!isInPictureInPictureMode){
            onBackPressed()
        }
    }

    @Suppress("DEPRECATION")
    fun enterPIPMode(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            videoPosition = player?.currentPosition?:0
            playerView.useController = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
            /* We need to check this because the system permission check is publically hidden for integers for non-manufacturer-built apps
               https://github.com/aosp-mirror/platform_frameworks_base/blob/studio-3.1.2/core/java/android/app/AppOpsManager.java#L1640

               ********* If we didn't have that problem *********
                val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                if(appOpsManager.checkOpNoThrow(AppOpManager.OP_PICTURE_IN_PICTURE, packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid, packageName) == AppOpsManager.MODE_ALLOWED)

                30MS window in even a restricted memory device (756mb+) is more than enough time to check, but also not have the system complain about holding an action hostage.
             */
            Handler().postDelayed({checkPIPPermission()}, 30)
        }
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
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
