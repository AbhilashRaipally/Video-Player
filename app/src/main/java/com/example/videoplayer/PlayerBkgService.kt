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
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.videoplayer.VideoUtils.NOTIFICATION_ID
import com.example.videoplayer.VideoUtils.PLAYBACK_CHANNEL_ID
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
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
        /*playerNotificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID, //Identifies Notification
            PLAYBACK_CHANNEL_ID, //Identifies notification channel
        ).setNotificationListener(
            notificationListener()
        ).setMediaDescriptionAdapter(
            mediaDescriptor(this) //Gives description about currently played item
        ).setCustomActionReceiver(customAction())
            .build()*/

        playerNotificationManager = VideoNotificationManager(
            context,
            PLAYBACK_CHANNEL_ID,
            NOTIFICATION_ID,
            mediaDescriptor(this),
            notificationListener(),
            customAction()
        )



        playerNotificationManager.setPlayer(player)
        playerNotificationManager.setUsePreviousActionInCompactView(true)
        playerNotificationManager.setUseNextActionInCompactView(true)
        playerNotificationManager.setUseFastForwardAction(false)
        playerNotificationManager.setUseRewindAction(false)
        playerNotificationManager.setUseNextAction(false)

        //Media session
        // -- allows communicate with other media controllers like google voice
        // -- Shows the notification in quick settings panel
        mediaSessionCompat = MediaSessionCompat(context, MEDIA_SESSION_TAG)
        mediaSessionCompat.isActive = true
        playerNotificationManager.setMediaSessionToken(mediaSessionCompat.sessionToken)
        mediaSessionConnector = MediaSessionConnector(mediaSessionCompat)
       /* mediaSessionConnector.setQueueNavigator(object :TimelineQueueNavigator(mediaSessionCompat){
            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                val video = videoSamples[windowIndex]
                val icon = AppCompatResources.getDrawable(context, R.drawable.ic_ap)?.toBitmap()
                return MediaDescriptionCompat.Builder()
                    .setDescription(video.description)
                    .setTitle(video.title)
                    .setIconBitmap(icon)
                    .setMediaUri(Uri.parse(video.url))
                    .build()

            }

        })*/
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

    private fun customAction() = object : PlayerNotificationManager.CustomActionReceiver{
        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): MutableMap<String, NotificationCompat.Action> {
            val intent: Intent = Intent("fav").setPackage(context.packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                context, instanceId, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            val action1 = NotificationCompat.Action(
                com.google.android.exoplayer2.R.drawable.exo_icon_next,
                "fav",
                pendingIntent)
            val actionMap: MutableMap<String, NotificationCompat.Action> = HashMap()
            actionMap["fav"] = action1

            return actionMap
        }

        override fun getCustomActions(player: Player): MutableList<String> {
            val stringActions: MutableList<String> = ArrayList()
            /*stringActions.add(PlayerNotificationManager.ACTION_PREVIOUS)
            stringActions.add(PlayerNotificationManager.ACTION_PAUSE)
            stringActions.add(PlayerNotificationManager.ACTION_PLAY)
            stringActions.add(PlayerNotificationManager.ACTION_NEXT)*/
            stringActions.add("fav")

            return stringActions
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            Log.e("onCustomAction","action: "+ intent.action + action);
            when(action){
               "fav" -> player.seekToNext()
            }
        }

    }
}