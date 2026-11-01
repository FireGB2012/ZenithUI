package com.gabriel.zenithui.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What is playing, as the Now playing widget shows it. */
@Immutable
data class Playing(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    /** Where it was, in milliseconds, at [positionAt] (the phone's uptime). */
    val position: Long,
    val positionAt: Long,
    val speed: Float,
    /** How long it is, in milliseconds; 0 when the app does not say. */
    val duration: Long,
    val canSkip: Boolean,
    val canGoBack: Boolean,
) {
    /** Where it is now, worked out from where it was and how long it has been playing since. */
    fun positionNow(now: Long = SystemClock.elapsedRealtime()): Long {
        val moved = if (playing) ((now - positionAt) * speed).toLong() else 0L
        val at = position + moved
        return if (duration > 0) at.coerceIn(0, duration) else at.coerceAtLeast(0)
    }
}

/**
 * What is playing on the phone, for the Now playing widget. Android only tells an app that has
 * notification access — the access the dots already use — and only while it is given; without it
 * there is simply nothing playing as far as Zenith knows. One app's music at a time: the one that
 * is playing, or else the last one that was.
 */
object NowPlaying {
    private val _now = MutableStateFlow<Playing?>(null)
    val now: StateFlow<Playing?> = _now.asStateFlow()

    @Volatile private var manager: MediaSessionManager? = null
    @Volatile private var controller: MediaController? = null
    private val main = Handler(Looper.getMainLooper())

    private val follow = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = choose(manager?.let(::sessions).orEmpty())
    }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        choose(controllers.orEmpty())
    }

    private var appContext: Context? = null

    private fun sessions(manager: MediaSessionManager): List<MediaController> = runCatching {
        manager.getActiveSessions(NotificationDots.component(appContext ?: return emptyList()))
    }.getOrNull().orEmpty()

    /** Starts following what is playing, once notification access has been given. Safe to call often. */
    fun watch(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (manager != null || !NotificationDots.allowed(app)) return
        val sessionManager = app.getSystemService(MediaSessionManager::class.java) ?: return
        val started = runCatching {
            sessionManager.addOnActiveSessionsChangedListener(sessionsChanged, NotificationDots.component(app), main)
        }.isSuccess
        if (!started) return
        manager = sessionManager
        choose(sessions(sessionManager))
    }

    private fun choose(controllers: List<MediaController>) {
        val chosen = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            runCatching { controller?.unregisterCallback(follow) }
            controller = chosen
            runCatching { chosen?.registerCallback(follow, main) }
        }
        publish()
    }

    private fun publish() {
        val playing = controller ?: run {
            _now.value = null
            return
        }
        val metadata = playing.metadata ?: run {
            _now.value = null
            return
        }
        val state = playing.playbackState
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (title.isNullOrBlank()) {
            _now.value = null
            return
        }
        val actions = state?.actions ?: 0L
        val previous = _now.value
        _now.value = Playing(
            packageName = playing.packageName,
            title = title,
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
            // The same song keeps the picture it had, rather than making a new one each tick.
            art = if (previous?.title == title && previous.packageName == playing.packageName && previous.art != null) previous.art else artOf(metadata),
            playing = state?.state == PlaybackState.STATE_PLAYING,
            position = state?.position ?: 0L,
            positionAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
            canSkip = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canGoBack = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
        )
    }

    /** The album's picture, small enough to hold: no widget is bigger than a few hundred pixels. */
    private fun artOf(metadata: MediaMetadata): ImageBitmap? {
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: return null
        val side = maxOf(bitmap.width, bitmap.height)
        val scaled = if (side > 384) {
            val scale = 384f / side
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            bitmap
        }
        return scaled.asImageBitmap()
    }

    fun playPause() {
        val playing = controller ?: return
        runCatching {
            if (playing.playbackState?.state == PlaybackState.STATE_PLAYING) playing.transportControls.pause()
            else playing.transportControls.play()
        }
    }

    fun next() {
        runCatching { controller?.transportControls?.skipToNext() }
    }

    fun previous() {
        runCatching { controller?.transportControls?.skipToPrevious() }
    }
}
