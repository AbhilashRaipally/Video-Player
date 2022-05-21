package com.example.videoplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.example.videoplayer.VideoUtils.NOTIFICATION_ID
import com.example.videoplayer.VideoUtils.PLAYBACK_CHANNEL_ID
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource


class PlayerBkgService : Service() {
    companion object {
        private const val MEDIA_SESSION_TAG = "mediaSession"
        private const val TAG = "AudioPlayerService"
    }

    private val binder = LocalBinder()

    private lateinit var mediaSessionConnector: MediaSessionConnector
    private var player: Player? = null

    val playerInstance: Player?
        get() {
            return player
        }

    private lateinit var videoSamples: List<Video>
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSessionCompat: MediaSessionCompat

    inner class LocalBinder : Binder() {
        val bkgService: PlayerBkgService
            get() = this@PlayerBkgService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "On Bind")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        val context: Context = this

        //Get sample videos from json file
        videoSamples = VideoUtils.getSampleVideos(context)

        //Configure player
        configurePlayer(context)

        //Configure foreground notification
        playerNotificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID, //Identifies Notification
            PLAYBACK_CHANNEL_ID, //Identifies notification channel
        ).setNotificationListener(
            notificationListener()
        ).setMediaDescriptionAdapter(
            mediaDescriptor(this) //Gives description about currently played item
        ).build()

        playerNotificationManager.setUseRewindAction(false)
        playerNotificationManager.setUseFastForwardAction(false)
        playerNotificationManager.setUseNextActionInCompactView(true)
        playerNotificationManager.setUsePreviousActionInCompactView(true)
        playerNotificationManager.setPlayer(player)

        //Media session
        // -- allows communicate with other media controllers like google voice
        // -- Shows the notification in quick settings panel
        mediaSessionCompat = MediaSessionCompat(context, MEDIA_SESSION_TAG)
        mediaSessionCompat.isActive = true

        playerNotificationManager.setMediaSessionToken(mediaSessionCompat.sessionToken)
        mediaSessionConnector = MediaSessionConnector(mediaSessionCompat)

        //Queue navigator helps in ensuring remote controls work and also helps us to get callbacks for events
        //https://github.com/google/ExoPlayer/issues/3559
        mediaSessionConnector.setQueueNavigator(object :TimelineQueueNavigator(mediaSessionCompat){
            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                return MediaDescriptionCompat.Builder()
                    .setDescription(videoSamples[windowIndex].description)
                    .build()
            }

            override fun onSkipToNext(player: Player) {
                Log.d("Media:","on skip to next")
                super.onSkipToNext(player)
            }
        })
        mediaSessionConnector.setPlayer(player)

    }

    private fun configurePlayer(context: Context) {
        // Create a data source factory.
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()

        //TO create playlist, add media sources as shown here
        val concatenatingMediaSource = ConcatenatingMediaSource()

        videoSamples.forEach {
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(it.url)))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        //If its for single video, we can set media item directly
        // player?.setMediaItem(mediaItem)
        val exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer.setMediaSource(concatenatingMediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        player = exoPlayer
    }

    override fun onDestroy() {
        mediaSessionCompat.release()
        mediaSessionConnector.setPlayer(null)
        playerNotificationManager.setPlayer(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action.equals("STOP_FOREGROUND_ACTION")) {
            //your end servce code
            stopForeground(true);
            stopSelfResult(startId);
        }
        return START_STICKY
    }

    //Callbacks for lifecycle of our notification
    private fun notificationListener() = object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            onGoing: Boolean
        ) {
            Log.d(TAG, "onNotificationPosted")
            startForeground(notificationId, notification)
        }

        override fun onNotificationCancelled(
            notificationId: Int,
            dismissedByUser: Boolean
        ) {
            Log.d(TAG, "onNotificationCancelled")
            stopSelf()
            stopForeground(true)
        }
    }

    //Callbacks to get the media information
    private fun mediaDescriptor(context: Context) =
        object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                val index = player.currentMediaItemIndex //Picks the item in the playlist
                return videoSamples[index].title
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                // fired when user taps on notification
                val intent = Intent(context, MyVideoPlayerActivity::class.java);
                return PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            override fun getCurrentContentText(player: Player): CharSequence {
                val index = player.currentMediaItemIndex //Picks the item in the playlist
                return videoSamples[index].description
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                Log.d(TAG, "getCurrentLargeIcon")
                return AppCompatResources.getDrawable(context, R.drawable.ic_ap)?.toBitmap()
            }

        }
}