package com.gabriel.zenithui.data

import android.content.ComponentName
import android.content.Intent
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Parcelable
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CompletableDeferred

/**
 * Android's handshake for closing an app into its icon, as Launcher3 speaks it
 * (`GestureNavContract`, Android 11 and later). With gesture navigation on a phone whose recents
 * come from AOSP's Quickstep, going home from an app brings the home app a question with the home
 * intent: where is this app's icon? Answered, Android springs the app's real window into that
 * spot instead of letting it fall to the foot of the screen, and says when it has got there.
 *
 * It is the only way a launcher that is not built into the phone has of moving another app's
 * window, and it needs no permission. With buttons nobody is asked: there, Zenith's own picture of
 * the app is used ([AppPictures]), or the card it opened on. Samsung's One UI asks, and then closes
 * the app into the middle of the screen whatever it is told, never saying the window got anywhere
 * (seen on a Galaxy A55, 2026-11-17), so there it is told there is no icon and Zenith closes the
 * app its own way after (AndroidClose.followsHandshake).
 */
class GestureHandshake private constructor(
    /** The app being closed. */
    val packageName: String,
    val className: String,
    /** In the profile Zenith runs in, rather than the work profile. */
    val personal: Boolean,
    private val callback: Message,
) {
    /** The key of the app being closed, as Zenith writes it. */
    val key: String get() = CloseRules.keyOf(packageName, className, work = !personal)

    /**
     * Tells Android where the icon is, on the whole screen, or with null that there is none, so
     * that it does its own. [arrived] is run on the main thread once Android says the window has
     * got there.
     */
    fun answer(at: Rect?, arrived: () -> Unit) {
        val result = Bundle()
        result.putParcelable(EXTRA_ICON_POSITION, at?.let { RectF(it.left, it.top, it.right, it.bottom) } ?: RectF())
        // Launcher3 hands Android a surface with the icon on it, to move with the window. Zenith
        // does not yet: the window goes into the icon itself.
        result.putParcelable(EXTRA_ICON_SURFACE, null)
        result.putParcelable(EXTRA_ON_FINISH_CALLBACK, Arrivals.waitFor(arrived))
        val reply = Message.obtain()
        reply.copyFrom(callback)
        reply.data = result
        runCatching { callback.replyTo?.send(reply) }
    }

    /** What Zenith answered, for the home screen to close the app by. */
    data class Answer(
        /** The app, as Zenith writes it. */
        val key: String,
        val at: Long,
        /** The icon it was sent into, in Zenith's own coordinates; null when there was none. */
        val target: Rect?,
        /** Whether that is a folder holding the app rather than the app's own icon. */
        val folder: Boolean = false,
        /** Done when Android says the window has got there. */
        val arrived: CompletableDeferred<Unit> = CompletableDeferred(),
    ) {
        /** Whether it still decides how the app closes at [now]: only for a moment after it was given. */
        fun fresh(now: Long): Boolean = now >= at && now - at <= CloseRules.HANDSHAKE_FRESH_MS
    }

    companion object {
        // Launcher3's own names, which Android listens for.
        const val EXTRA_CONTRACT = "gesture_nav_contract_v1"
        const val EXTRA_ICON_POSITION = "gesture_nav_contract_icon_position"
        const val EXTRA_ICON_SURFACE = "gesture_nav_contract_surface_control"
        const val EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK"
        const val EXTRA_ON_FINISH_CALLBACK = "gesture_nav_contract_finish_callback"

        /**
         * Answers the question, set by the home screen while it is on: Zenith's lifecycle brings
         * the question in before the home screen has seen the intent, and it has to be answered
         * at once, while Android is still deciding where the window goes.
         */
        var answerer: ((GestureHandshake) -> Unit)? = null

        /** Android's question, if [intent] holds one, taken out of it so it is only answered once. */
        fun from(intent: Intent?): GestureHandshake? {
            if (intent == null) return null
            val extras = runCatching { intent.getBundleExtra(EXTRA_CONTRACT) }.getOrNull() ?: return null
            intent.removeExtra(EXTRA_CONTRACT)
            return runCatching {
                val component = extras.parcel(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
                val user = extras.parcel(Intent.EXTRA_USER, UserHandle::class.java)
                val callback = extras.parcel(EXTRA_REMOTE_CALLBACK, Message::class.java)
                if (component == null || user == null || callback?.replyTo == null) {
                    null
                } else {
                    GestureHandshake(component.packageName, component.className, user == Process.myUserHandle(), callback)
                }
            }.getOrNull()
        }

        /** [bounds], in Zenith's own coordinates, on the whole screen when Zenith's window starts at [windowLeft], [windowTop]. */
        fun onScreen(bounds: Rect, windowLeft: Int, windowTop: Int): Rect = bounds.translate(windowLeft.toFloat(), windowTop.toFloat())

        @Suppress("DEPRECATION")
        private fun <T : Parcelable> Bundle.parcel(key: String, type: Class<T>): T? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelable(key, type) else getParcelable(key)
    }

    /**
     * Where Android says the window has got there. One messenger for all of them, as in
     * Launcher3, so a launcher closing apps all day does not hand Android a new binder each time;
     * only the latest answer is waited for.
     */
    private object Arrivals : Handler.Callback {
        private const val ARRIVED = 1

        private val messenger by lazy { Messenger(Handler(Looper.getMainLooper(), this)) }
        private var waiting = 0
        private var then: (() -> Unit)? = null

        fun waitFor(arrived: () -> Unit): Message {
            waiting++
            then = arrived
            return Message.obtain().apply {
                what = ARRIVED
                arg1 = waiting
                replyTo = messenger
            }
        }

        override fun handleMessage(message: Message): Boolean {
            if (message.what != ARRIVED) return false
            if (message.arg1 == waiting) {
                val run = then
                then = null
                run?.invoke()
            }
            return true
        }
    }
}
