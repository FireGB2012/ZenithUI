package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AndroidClose
import com.gabriel.zenithui.data.AnimationPreset
import com.gabriel.zenithui.data.AnimationTuning
import com.gabriel.zenithui.data.CloseLog
import com.gabriel.zenithui.data.CloseRules
import com.gabriel.zenithui.data.LaunchMotion
import com.gabriel.zenithui.data.Tune
import com.gabriel.zenithui.data.TuneGroup
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.home.FlyIn
import com.gabriel.zenithui.ui.home.LiquidArrival
import com.gabriel.zenithui.ui.home.LiquidWave
import com.gabriel.zenithui.ui.home.springOf
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.corners
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Advanced animation settings: every number an app opens and closes on, a set of them under one
// name to start from, and a small screen at the top that plays whatever is set, so a change can
// be watched without leaving the page.

/** Where each group of numbers is found by a search: its Spot on the page. */
private fun TuneGroup.spot(): String = when (this) {
    TuneGroup.Open -> "motionOpen"
    TuneGroup.Home -> "motionHome"
    TuneGroup.Close -> "motionClose"
    TuneGroup.Arrive -> "motionArrive"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdvancedMotionOptions(settings: ZenithSettings, actions: SettingsActions) {
    val tuning = settings.animation
    val preset = AnimationPreset.byId(tuning.preset)
    fun set(next: AnimationTuning) = actions.change { it.copy(animation = next) }

    Box(Modifier.fillMaxWidth().riseIn(0), contentAlignment = Alignment.Center) {
        MotionPreview(tuning, closingPlain = settings.closeAnimation == CloseRules.PLAIN, liquid = settings.arrival == LiquidArrival.STYLE)
    }
    if (settings.motion == "reduced") {
        Note("Animations are set to Reduced, so apps open and close plainly whatever is chosen here. Set them to Full to see it.")
    }

    Group(rise = 1) {
        Spot("motionStyle") {
            Column(Modifier.padding(14.dp)) {
                Caption("Animation style")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationPreset.entries.forEach { option ->
                        StyleChip(option.label, selected = option == preset) { set(AnimationTuning.of(option)) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Note(if (tuning.custom) "${preset.detail} With your own changes on top." else preset.detail)
            }
        }
        GroupDivider(inset = 16.dp)
        Spot("motionSpeed") {
            TuneSlider(
                title = "Speed",
                value = tuning.pace,
                min = AnimationTuning.MIN_SPEED,
                max = AnimationTuning.MAX_SPEED,
                shown = "${hundredths(tuning.pace)}×",
                note = "Everything at once. 1.00× is as designed, 2.00× twice as quick.",
                onChange = { set(tuning.paced(it)) },
                onStep = { steps -> set(tuning.paced(tuning.pace + steps * AnimationTuning.SPEED_STEP)) },
            )
        }
    }

    // What Auto waits for Android's own close on this phone, and how long that last took.
    val context = androidx.compose.ui.platform.LocalContext.current
    val autoWait = remember(context) { AndroidClose.planHere(context, 0f) }
    val lastTook by AndroidClose.lastTook.collectAsState()
    // And what the last close of an app did, so one that looked wrong can be told from one that went as meant.
    val lastClose by CloseLog.last.collectAsState()
    TuneGroup.entries.forEachIndexed { index, group ->
        Group(rise = index + 2) {
            Spot(group.spot()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp)) {
                    Caption(group.title)
                    Spacer(Modifier.height(4.dp))
                    Note(group.note)
                }
                Tune.entries.filter { it.group == group }.forEach { tune ->
                    TuneSlider(
                        title = tune.title,
                        value = tuning[tune],
                        min = tune.min,
                        max = tune.max,
                        shown = tune.show(tuning[tune]),
                        note = if (tune == Tune.CloseWait) waitNote(autoWait, lastTook, CloseLog.describe(lastClose)) else tune.note,
                        onChange = { set(tuning.with(tune, it)) },
                        onStep = { steps -> set(tuning.nudged(tune, steps)) },
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    // The way back is only there while there is something to go back from: it opens out under
    // the page at the first change, and its arrow turns right round when it is taken.
    var resets by remember { mutableIntStateOf(0) }
    AnimatedVisibility(
        visible = tuning != AnimationTuning(),
        enter = unfoldIn(LocalMotionLevel.current.reduced),
        exit = unfoldOut(LocalMotionLevel.current.reduced),
    ) {
        Group {
            ActionRow(
                icon = Icons.Rounded.RestartAlt,
                title = "Back to Zenith's own",
                subtitle = "Undo every change on this page",
                spin = resets,
                onClick = {
                    set(AnimationTuning())
                    resets++
                },
            )
        }
    }
}

/** "1.25" from 1.25, with both places always there so the number does not jump about as it is dragged. */
private fun hundredths(value: Float): String {
    val all = Math.round(value * 100f)
    return "${all / 100}.${(all % 100).toString().padStart(2, '0')}"
}

/** One of the named sets of numbers. */
@Composable
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    // The colour moves from the style that was chosen to the one that is, rather than one chip
    // going out and another coming on.
    val fill by animateColorAsState(if (selected) palette.accent.copy(alpha = 0.9f) else Color.Transparent, tween(220), label = "styleFill")
    val rim by animateColorAsState(palette.onGlass.copy(alpha = if (selected) 0f else 0.18f), tween(220), label = "styleRim")
    val ink by animateColorAsState(
        if (selected) inkOnTint(palette.accent) else palette.onGlass,
        tween(220),
        label = "styleInk",
    )
    Box(
        Modifier
            .height(38.dp)
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .border(1.dp, rim, RoundedCornerShape(50))
            .clickable {
                if (!selected) tick()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * The wait for Android's own close, with what Auto comes to on this phone, what the last close
 * took, and what it did ([lastClose], from CloseLog).
 */
private fun waitNote(auto: AndroidClose.Plan?, lastTook: Long, lastClose: String = ""): String {
    val here = when {
        auto == null -> "Android's animations are off on this phone, so Auto does not wait."
        auto.listens -> "On this phone that is ${auto.atLeastMs} ms at least and ${auto.atMostMs} ms at the most."
        else -> "On this phone that is ${auto.atLeastMs} ms."
    }
    val last = if (lastTook > 0L) " Android's own close last took $lastTook ms." else ""
    val did = if (lastClose.isNotEmpty()) " $lastClose" else ""
    return "${Tune.CloseWait.note} $here$last$did"
}

/**
 * One number: a slider for getting near, a minus and a plus for getting it exact, a step at a
 * time, and a line saying what it changes.
 */
@Composable
private fun TuneSlider(
    title: String,
    value: Float,
    min: Float,
    max: Float,
    shown: String,
    note: String,
    onChange: (Float) -> Unit,
    onStep: (Int) -> Unit,
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillSlider(
                label = title,
                value = value,
                range = min..max,
                display = shown,
                modifier = Modifier.weight(1f),
                onChange = onChange,
            )
            Spacer(Modifier.width(8.dp))
            StepButton(Icons.Rounded.Remove, "A step less $title", enabled = value > min) { onStep(-1) }
            Spacer(Modifier.width(6.dp))
            StepButton(Icons.Rounded.Add, "A step more $title", enabled = value < max) { onStep(1) }
        }
        Spacer(Modifier.height(5.dp))
        Note(note)
    }
}

@Composable
private fun StepButton(icon: ImageVector, says: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    Box(
        Modifier
            .size(40.dp)
            .pressBounce(0.88f, enabled = enabled)
            .clip(CircleShape)
            .background(palette.onGlass.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled, onClickLabel = says) {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = says, tint = palette.onGlass.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(18.dp))
    }
}

/**
 * A small home screen that opens and closes an app with the numbers as they stand, on the same
 * sums and the same springs as the real one ([LaunchMotion], [AnimationTuning]). Tap it to open,
 * tap again to close. Distances are scaled to its size, so what is seen here is what happens on
 * the real screen, only smaller. The blur is left out: it would cost more than a picture is worth.
 * Closing Into its icon, as on a Galaxy, the card fills the screen again, speeds into the icon and
 * lands squashed while the home screen comes forward about its foot (CloseRules.bringsHome).
 * Closing Plain, the card (if one is asked for) flies back and the icon lands; coming home
 * [liquid], the icons rise on the wave out from the app's icon as it closes ([LiquidArrival]),
 * with a band of its light running out with it.
 */
@Composable
private fun MotionPreview(tuning: AnimationTuning, closingPlain: Boolean, liquid: Boolean) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val live by rememberUpdatedState(tuning)
    val screen = LocalConfiguration.current
    val tall = 300.dp
    val broad = tall * (screen.screenWidthDp.toFloat() / screen.screenHeightDp.coerceAtLeast(1))
    /** How much smaller than the real screen this is: what every distance in the tuning is scaled by. */
    val small = broad.value / screen.screenWidthDp.coerceAtLeast(1)

    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    val grow = remember { Animatable(0f) }
    /** The home screen on its way forward again, 1 behind the app to 0 at rest. */
    val sink = remember { Animatable(0f) }
    /** The icon landing, 0 as it arrives to 1 settled. */
    val land = remember { Animatable(1f) }
    /** A closing card landing squashed in its icon, springing back to 0, its shape. */
    val squash = remember { Animatable(0f) }
    /** Coming forward about the foot of the screen, behind a card going into its icon. */
    var fromFoot by remember { mutableStateOf(false) }
    val wave = remember { Animatable(0f) }
    /** The arrival clock coming home like liquid, at rest past its end. */
    val arrive = remember { Animatable(FlyIn.AT_REST) }
    /** The liquid wave as this little screen has it, worked out again only when the numbers or the size change. */
    val liquidWave = remember { arrayOfNulls<Pair<Any, LiquidWave>>(1) }
    var open by remember { mutableStateOf(false) }
    /** Coming home: the home screen follows [sink], not the card. */
    var returning by remember { mutableStateOf(false) }
    val job = remember { arrayOfNulls<Job>(1) }

    fun play() {
        job[0]?.cancel()
        open = !open
        val t = live
        job[0] = scope.launch {
            suspend fun fly(to: Float) = kotlinx.coroutines.coroutineScope {
                launch { x.animateTo(to, t.springOf(Tune.OpenXStiffness, Tune.OpenXDamping)) }
                launch { y.animateTo(to, t.springOf(Tune.OpenYStiffness, Tune.OpenYDamping)) }
                grow.animateTo(to, t.springOf(Tune.OpenSizeStiffness, Tune.OpenSizeDamping))
            }
            suspend fun rest(at: Float) {
                x.snapTo(at)
                y.snapTo(at)
                grow.snapTo(at)
            }
            if (open) {
                returning = false
                fromFoot = false
                squash.snapTo(0f)
                land.snapTo(1f)
                arrive.snapTo(FlyIn.AT_REST)
                fly(1f)
            } else {
                // On the phone Android takes the app's own window away. What Zenith draws is what
                // follows. Into its icon: the card fills the screen again and goes into the icon,
                // bringing the home screen forward with it once it is well on its way. Plain: the home
                // screen forward, the card going back into the icon if one is asked for, the landing,
                // and coming home like liquid everything else rising on the wave out from the icon.
                returning = true
                sink.snapTo(1f)
                val asked = t[Tune.CloseCard]
                val from = if (closingPlain && asked <= CloseRules.CARD_OFF) 0f else CloseRules.cardStart(asked)
                val intoIcon = !closingPlain && from > CloseRules.CARD_OFF
                fromFoot = intoIcon
                if (intoIcon) {
                    rest(from)
                    launch {
                        snapshotFlow { grow.value }.first { it <= CloseRules.HOME_GOES_AT }
                        sink.animateTo(0f, t.springOf(Tune.ReturnStiffness, Tune.ReturnDamping))
                    }
                    val into = tween<Float>(
                        durationMillis = Math.round(t.ms(Tune.CloseTime)).coerceAtLeast(1),
                        easing = androidx.compose.animation.core.Easing { CloseRules.intoIcon(it) },
                    )
                    launch { x.animateTo(0f, into) }
                    launch { y.animateTo(0f, into) }
                    grow.animateTo(0f, into)
                    val amount = t[Tune.CloseSquash]
                    if (amount > 0f) {
                        val stiffness = t.stiffness(Tune.LandStiffness)
                        val damping = t[Tune.LandDamping]
                        squash.snapTo(0f)
                        squash.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = damping, stiffness = stiffness, visibilityThreshold = CloseRules.SQUASH_REST),
                            initialVelocity = LaunchMotion.knockVelocity(amount, stiffness, damping),
                        )
                    }
                    // The card was the icon by the time it got there: nothing lands, nothing is knocked.
                    return@launch
                }
                launch { sink.animateTo(0f, t.springOf(Tune.ReturnStiffness, Tune.ReturnDamping)) }
                if (liquid) {
                    // How long it runs does not hang on where it starts or how far it has to go.
                    val total = LiquidArrival.wave(Offset.Zero, t, 1f) { 1f }.totalMs
                    launch {
                        arrive.snapTo(0f)
                        arrive.animateTo(total, tween(Math.round(total), easing = androidx.compose.animation.core.LinearEasing))
                        arrive.snapTo(FlyIn.AT_REST)
                    }
                }
                if (from > CloseRules.CARD_OFF) {
                    rest(from)
                    val flight = launch { fly(0f) }
                    snapshotFlow { grow.value }.first { it <= 0.03f }
                    flight.cancel()
                }
                rest(0f)
                launch {
                    val stiffness = t.stiffness(Tune.LandStiffness) * 0.6f
                    wave.snapTo(0f)
                    wave.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = t[Tune.LandDamping], stiffness = stiffness),
                        initialVelocity = LaunchMotion.knockVelocity(t[Tune.Ripple], stiffness, t[Tune.LandDamping]),
                    )
                }
                land.snapTo(0f)
                land.animateTo(1f, t.springOf(Tune.LandStiffness, Tune.LandDamping))
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(broad, tall)
                .clip(corners(22.dp))
                .background(Brush.verticalGradient(palette.backdrop))
                .border(1.dp, palette.onGlass.copy(alpha = 0.18f), corners(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Play the animation",
                ) { play() },
        ) {
            val t = live
            val dp = density * small
            val columns = 4
            val rows = 5
            val cell = size.width / columns
            val rowHeight = size.height * 0.78f / rows
            val icon = cell * 0.58f
            fun centreOf(col: Int, row: Int) = Offset(cell * (col + 0.5f), size.height * 0.07f + rowHeight * (row + 0.5f))
            val target = centreOf(1, 2)

            // The home screen: how far back it is, and so how far it has zoomed, about what point.
            val depth = if (returning) sink.value.coerceIn(-0.12f, 1f) else grow.value.coerceIn(0f, 1f)
            val zoom = 1f + (t[Tune.HomeScale] - 1f) * depth
            val towards = if (depth != 0f) t[Tune.HomePivot] else 0f
            val (footX, footY) = CloseRules.comeBackPivot()
            val pivot = if (fromFoot && returning) {
                Offset(size.width * footX, size.height * footY)
            } else {
                Offset(
                    size.width * LaunchMotion.pivot(target.x, size.width, towards),
                    size.height * LaunchMotion.pivot(target.y, size.height, towards),
                )
            }
            val dim = 1f - t[Tune.HomeDim] * depth.coerceIn(0f, 1f)
            val pushed = (if (returning) 0f else t[Tune.IconPush] * grow.value.coerceIn(0f, 1f)) + wave.value
            val reach = 150f * dp

            // Coming home like liquid: the wave out from the app's icon, reaching past the farthest
            // corner as the real one does, its band of light included.
            val clock = arrive.value
            val flowing = if (liquid && returning && clock < FlyIn.AT_REST) {
                val key = Triple(t, size, dp)
                liquidWave[0]?.takeIf { it.first == key }?.second ?: run {
                    val corner = kotlin.math.hypot(maxOf(target.x, size.width - target.x), maxOf(target.y, size.height - target.y))
                    LiquidArrival.wave(target, t, dp) { corner + 2f * PREVIEW_BAND_DP * dp }.also { liquidWave[0] = key to it }
                }
            } else {
                null
            }

            fun drawIcon(centre: Offset, colour: Color, scale: Float = 1f, alpha: Float = 1f) {
                var grown = scale
                var shown = alpha
                var moved = centre
                if (flowing != null && clock < flowing.totalMs) {
                    val away = centre - target
                    val distance = away.getDistance()
                    val out = flowing.out(clock, flowing.delayMs(distance))
                    grown *= flowing.scaleAt(out)
                    shown *= LiquidArrival.alphaAt(out)
                    if (distance > 0.5f) moved -= away / distance * flowing.pullAt(distance) * out
                }
                val (dx, dy) = LaunchMotion.push(moved.x - target.x, moved.y - target.y, pushed * dp, reach)
                val at = pivot + (moved + Offset(dx, dy) - pivot) * zoom
                val side = icon * zoom * grown
                drawRoundRect(
                    color = colour.copy(alpha = colour.alpha * shown * dim),
                    topLeft = Offset(at.x - side / 2f, at.y - side / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(side * 0.26f),
                )
            }

            // The band of light running out with the wave, as soft on the wallpaper as on the phone.
            if (flowing != null && clock < flowing.runMs) {
                val progress = clock / flowing.runMs
                val strength = flowing.light.strengthAt(progress)
                val front = flowing.front(clock)
                val band = (PREVIEW_BAND_DP * (0.55f + 0.45f * progress)) * dp
                val outer = front + 2f * band
                if (strength > 0.005f && outer > 1f) {
                    val inner = ((front - 2f * band) / outer).coerceIn(0f, 1f)
                    val middle = (front / outer).coerceIn(inner, 1f)
                    drawCircle(
                        Brush.radialGradient(
                            inner to Color.Transparent,
                            middle to Color.White.copy(alpha = PREVIEW_LIGHT * strength),
                            1f to Color.Transparent,
                            center = target,
                            radius = outer,
                        ),
                        radius = outer,
                        center = target,
                    )
                }
            }

            val plain = palette.onGlass.copy(alpha = 0.22f)
            for (row in 0 until rows) for (col in 0 until columns) {
                if (col == 1 && row == 2) continue
                drawIcon(centreOf(col, row), plain)
            }
            for (col in 0 until columns) drawIcon(Offset(cell * (col + 0.5f), size.height * 0.92f), plain)

            // The app's own icon: out of sight under a card, landing once the card has gone. A card
            // going into its icon is the icon by the time it gets there, squashed for a moment.
            val covered = grow.value > 0.002f || squash.isRunning
            if (!covered) {
                val landed = land.value
                val fade = t[Tune.LandFade]
                drawIcon(
                    target, palette.accent,
                    scale = t[Tune.LandScale] - (t[Tune.LandScale] - 1f) * landed,
                    alpha = ((1f - fade) + fade * 1.6f * landed).coerceIn(0f, 1f),
                )
            } else {
                // Bringing the home screen back with it, the card goes into the icon where the icon is
                // on the way, and moves with it as the home screen comes forward.
                val rest = Rect(target.x - icon / 2f, target.y - icon / 2f, target.x + icon / 2f, target.y + icon / 2f)
                val into = if (fromFoot && returning) CloseRules.fromFoot(rest, zoom, size.width, size.height) else rest
                val frame = LaunchMotion.frame(
                    into.left, into.top, into.right, into.bottom,
                    size.width, size.height,
                    x.value.coerceIn(-0.2f, 1.2f), y.value.coerceIn(-0.2f, 1.2f), grow.value.coerceIn(0f, 1.06f),
                    t[Tune.OpenArc] * dp,
                    into.width * 0.26f, t[Tune.OpenCorner] * dp,
                    squash = squash.value,
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(palette.accent, palette.accentAlt)),
                    topLeft = Offset(frame.left, frame.top),
                    size = Size(frame.width, frame.height),
                    cornerRadius = CornerRadius(frame.radius),
                )
                // The logo in the middle of it, growing more slowly than the card. Going into the icon
                // the card is the icon here, which has none.
                val logo = icon * 0.5f + (t[Tune.OpenLogo] * dp - icon * 0.5f) * grow.value.coerceIn(0f, 1f)
                if (!fromFoot || !returning) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(frame.centreX - logo / 2f, frame.centreY - logo / 2f),
                        size = Size(logo, logo),
                        cornerRadius = CornerRadius(logo * 0.26f),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Note(if (open) "Tap to close the app" else "Tap to open an app with these settings")
    }
}

/** How wide the light of a liquid wave is in the preview, in the real screen's dp, and how bright on its wallpaper. */
private const val PREVIEW_BAND_DP = 36f
private const val PREVIEW_LIGHT = 0.22f
