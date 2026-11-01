package com.gabriel.zenithui.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zenith's notification dots. Once Android has been told Zenith may see notifications, it tells
 * this listener what each app has showing, and the dots are counted from that — what they say,
 * never what is in them. Nothing is kept or sent anywhere. The same access is what lets the Now
 * playing widget see which app is playing.
 */
class NotificationDots : NotificationListenerService() {

    override fun onListenerConnected() {
        _connected.value = true
        recount()
        known.clear()
        runCatching { activeNotifications }.getOrNull()?.forEach { known.add(it.key) }
        // Whatever is already running — a call, a timer — is found at once, not at its next update.
        _live.value = runCatching { activeNotifications }.getOrNull().orEmpty().mapNotNull(::liveOf).associateBy { it.key }
    }

    override fun onListenerDisconnected() {
        _connected.value = false
        _counts.value = emptyMap()
        _live.value = emptyMap()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        recount()
        tell(sbn, null)
        track(sbn)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        recount()
        tell(sbn, rankingMap)
        track(sbn)
    }

    /** A notification with a running clock in it, kept for the island; one without, let go. */
    private fun track(sbn: StatusBarNotification?) {
        sbn ?: return
        val live = liveOf(sbn)
        _live.value = if (live != null) _live.value + (sbn.key to live) else _live.value - sbn.key
    }

    /**
     * What an ongoing notification has running, if it has a clock: a call, a timer, a stopwatch,
     * a recording. Its clock is Android's own when it shows one; else a time in its words that the
     * app keeps up to date itself. Null for anything else, music included.
     */
    private fun liveOf(sbn: StatusBarNotification): Live? {
        val n = sbn.notification
        if (n.flags and Notification.FLAG_ONGOING_EVENT == 0 || sbn.packageName == packageName) return null
        val extras = n.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val words = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val clock = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true && n.`when` > 0L
        val shown = listOf(title, words).map { it.trim() }.firstOrNull { TIME.matches(it) }
        if (!clock && shown == null) return null
        return Live(
            key = sbn.key,
            packageName = sbn.packageName,
            title = title,
            base = if (clock) n.`when` else 0L,
            countDown = clock && extras?.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) == true,
            call = n.category == Notification.CATEGORY_CALL,
            shown = if (clock) null else shown,
            postedAt = sbn.postTime,
            open = n.contentIntent,
        )
    }

    /**
     * A notification just come in, told to the island: only one Android itself would bring to your
     * attention — not a silent or minimised one, not one Do Not Disturb is holding back, not one
     * that is always there (music, a download, an app saying it is running), not a group's summary,
     * not Zenith's own, and not an update to one already showing that does not alert again.
     */
    private fun tell(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        sbn ?: return
        val n = sbn.notification
        val again = !known.add(sbn.key)
        if (n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_GROUP_SUMMARY) != 0) return
        if (again && n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0) return
        if (sbn.packageName == packageName || n.category in QUIET) return
        val ranking = Ranking()
        val ranked = runCatching { (rankingMap ?: currentRanking)?.getRanking(sbn.key, ranking) == true }.getOrDefault(false)
        if (ranked && (ranking.importance < android.app.NotificationManager.IMPORTANCE_DEFAULT || !ranking.matchesInterruptionFilter() || ranking.isSuspended)) return
        val extras = n.extras
        // A chat's newest message and who sent it, rather than "3 new messages"; else the whole
        // text where the app gives it, and its short text where not.
        val chat = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }.getOrNull()
        val last = chat?.messages?.lastOrNull()
        val title = last?.person?.name?.toString()?.takeIf { it.isNotBlank() }
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val words = last?.text?.toString()?.takeIf { it.isNotBlank() }
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        _posted.tryEmit(Posted(sbn.packageName, title.trim(), words.trim(), n.contentIntent))
    }

    /** Every notification showing now, to tell a new one from an update to one already there. */
    private val known = HashSet<String>()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        recount()
        sbn?.let { forget(it.key) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        recount()
        sbn?.let { forget(it.key) }
    }

    private fun forget(key: String) {
        _live.value = _live.value - key
        known.remove(key)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = recount()

    private fun recount() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val ranking = runCatching { currentRanking }.getOrNull()
        val one = Ranking()
        _counts.value = Badges.tally(
            active.map { sbn ->
                val flags = sbn.notification.flags
                Badges.Seen(
                    packageName = sbn.packageName,
                    ongoing = flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0,
                    summary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
                    // Android's own word on whether this one is a badge, where it has one.
                    canBadge = !(ranking?.getRanking(sbn.key, one) == true && !one.canShowBadge()),
                    number = sbn.notification.number,
                )
            }
        )
    }

    /** A notification just come in: whose, what it says, and what tapping it opens. */
    class Posted(val packageName: String, val title: String, val text: String, val open: android.app.PendingIntent?)

    /**
     * Something running with a clock, from an app's ongoing notification: a call ([call]), a timer
     * counting [countDown] to [base], a stopwatch or recording counting up from it. [shown] is the
     * time in the app's own words when it keeps its clock itself.
     */
    class Live(
        val key: String,
        val packageName: String,
        val title: String,
        val base: Long,
        val countDown: Boolean,
        val call: Boolean,
        val shown: String?,
        val postedAt: Long,
        val open: android.app.PendingIntent?,
    )

    companion object {
        private val _posted = kotlinx.coroutines.flow.MutableSharedFlow<Posted>(extraBufferCapacity = 4)

        /** Notifications as they come in, for the island. Nothing is kept. */
        val posted: kotlinx.coroutines.flow.SharedFlow<Posted> = _posted

        private val _live = MutableStateFlow<Map<String, Live>>(emptyMap())

        /** What is running with a clock right now, by notification. */
        val live: StateFlow<Map<String, Live>> = _live.asStateFlow()

        /** Kinds of notification that are never news: an app at work, a download, music, the system. */
        private val QUIET = setOf(
            Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SYSTEM,
        )

        /** A time as an app writes one: 4:32, 01:02:09. */
        private val TIME = Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")

        private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())

        /** How many each app has, by package. Empty until Android connects the listener. */
        val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        fun component(context: Context) = ComponentName(context, NotificationDots::class.java)

        /** Whether notification access has been given to Zenith. */
        fun allowed(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Android sometimes drops a listener whose app it has stopped; this asks for it back. */
        fun reconnect(context: Context) {
            if (!allowed(context) || _connected.value) return
            runCatching { requestRebind(component(context)) }
        }

        /** Opens Android's page for giving Zenith notification access. */
        fun ask(context: Context) {
            val detail = if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component(context).flattenToString())
            } else {
                null
            }
            val opened = detail != null && runCatching {
                context.startActivity(detail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (!opened) {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }
}
