package com.example.videoplayer

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.util.*

class VideoNotificationManager(
    context: Context,
    channelId: String,
    notificationId: Int,
    mediaDescriptionAdapter: MediaDescriptionAdapter,
    notificationListener: NotificationListener,
    customActionReceiver: CustomActionReceiver
) : PlayerNotificationManager(
    context,
    channelId,
    notificationId,
    mediaDescriptionAdapter,
    notificationListener,
    customActionReceiver,
    com.google.android.exoplayer2.R.drawable.exo_ic_play_circle_filled,
    com.google.android.exoplayer2.R.drawable.exo_controls_play,
    com.google.android.exoplayer2.R.drawable.exo_controls_pause,
    com.google.android.exoplayer2.R.drawable.exo_icon_stop,
    com.google.android.exoplayer2.ui.R.drawable.exo_controls_rewind,
    com.google.android.exoplayer2.R.drawable.exo_controls_fastforward,
    com.google.android.exoplayer2.R.drawable.exo_controls_previous,
    com.google.android.exoplayer2.R.drawable.exo_controls_next,
    null
) {
    override fun getActionIndicesForCompactView(
        actionNames: MutableList<String>,
        player: Player
    ): IntArray {
        val pauseActionIndex = actionNames.indexOf(ACTION_PAUSE)
        val playActionIndex = actionNames.indexOf(ACTION_PLAY)
        val leftSideActionIndex = actionNames.indexOf(ACTION_PREVIOUS)
        val rightSideActionIndex = actionNames.indexOf("fav")


        val actionIndices = IntArray(3)
        var actionCounter = 0
        if (leftSideActionIndex != -1) {
            actionIndices[actionCounter++] = leftSideActionIndex
        }
        if (pauseActionIndex != -1 ) {
            actionIndices[actionCounter++] = pauseActionIndex
        } else if (playActionIndex != -1) {
            actionIndices[actionCounter++] = playActionIndex
        }
        if (rightSideActionIndex != -1) {
            actionIndices[actionCounter++] = rightSideActionIndex
        }
        return Arrays.copyOf(actionIndices, actionCounter)
    }
}