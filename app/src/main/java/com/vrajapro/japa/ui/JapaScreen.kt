package com.vrajapro.japa.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vrajapro.japa.JapaState
import com.vrajapro.japa.JapaViewModel
import com.vrajapro.japa.Pace
import com.vrajapro.japa.R
import com.vrajapro.japa.VoiceStatus
import com.vrajapro.japa.data.Session
import java.text.DateFormat
import java.util.Date

private enum class WordType { HARE, KRISHNA, RAMA }

private val MANTRA = listOf(
    WordType.HARE, WordType.KRISHNA, WordType.HARE, WordType.KRISHNA,
    WordType.KRISHNA, WordType.KRISHNA, WordType.HARE, WordType.HARE,
    WordType.HARE, WordType.RAMA, WordType.HARE, WordType.RAMA,
    WordType.RAMA, WordType.RAMA, WordType.HARE, WordType.HARE,
)

private val NumberFont = FontFamily.Serif

@Composable
fun JapaScreen(vm: JapaViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Keep the screen awake while a session is running.
    val view = LocalView.current
    LaunchedEffect(state.running) { view.keepScreenOn = state.running }

    // A gentle tick for every counted mantra — useful with eyes closed.
    // (rememberSaveable so a language switch / rotation doesn't replay the last tick.)
    var hapticTick by rememberSaveable { mutableIntStateOf(state.countTick) }
    LaunchedEffect(state.countTick) {
        if (state.countTick != hapticTick) {
            hapticTick = state.countTick
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setVoiceEnabled(true) else vm.onMicPermissionDenied()
    }
    val onVoiceToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            vm.setVoiceEnabled(false)
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            vm.setVoiceEnabled(true)
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JapaColors.Surface, JapaColors.Bg, JapaColors.Bg)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Header()
                Spacer(Modifier.height(8.dp))
                ProgressRing(state)
                Spacer(Modifier.height(16.dp))
                MantraWords(state)
                Spacer(Modifier.height(16.dp))
                SessionControls(state, onEnd = vm::endSession, onReset = vm::reset)
                Spacer(Modifier.height(20.dp))
                ManualCard(state, onCount = vm::countManual, onLock = vm::setLockTap)
                Spacer(Modifier.height(12.dp))
                VoiceCard(state, onToggle = onVoiceToggle)
                Spacer(Modifier.height(12.dp))
                GuidedCard(state, onPace = vm::setPace, onPlay = vm::setGuidedPlaying)
                Spacer(Modifier.height(20.dp))
                HistoryCard(state.history)
                Spacer(Modifier.height(24.dp))
            }
        }

        if (state.lockTap && !state.showSessionDialog) {
            LockOverlay(onTap = vm::countManual, onUnlock = { vm.setLockTap(false) })
        }
    }

    if (state.showSessionDialog) {
        SessionDialog(
            state = state,
            onSave = vm::saveSession,
            onDismiss = vm::dismissSessionDialog,
            onDiscard = vm::discardSession,
            onInteraction = vm::onSessionDialogInteraction,
        )
    }
}

// ─── Header + language ──────────────────────────────────────────────────────

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LanguageSwitch()
        }
        Text("ॐ", fontSize = 36.sp, color = JapaColors.Saffron, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.title),
            fontFamily = NumberFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = JapaColors.Text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LanguageSwitch() {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val systemHebrew = java.util.Locale.getDefault().language.let { it == "he" || it == "iw" }
    val isHebrew = if (current.isEmpty()) systemHebrew else current.startsWith("he") || current.startsWith("iw")

    Row(verticalAlignment = Alignment.CenterVertically) {
        LangChip(stringResource(R.string.lang_english), selected = !isHebrew) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        Spacer(Modifier.width(4.dp))
        LangChip(stringResource(R.string.lang_hebrew), selected = isHebrew) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("he"))
        }
    }
}

@Composable
private fun LangChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = { if (!selected) onClick() },
        shape = RoundedCornerShape(50),
        color = if (selected) JapaColors.Saffron else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) JapaColors.Saffron else JapaColors.Border),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = if (selected) Color.White else JapaColors.TextDim,
        )
    }
}

// ─── Ring ───────────────────────────────────────────────────────────────────

@Composable
private fun ProgressRing(state: JapaState) {
    val progress by animateFloatAsState(state.roundProgress, tween(300), label = "ring")
    val bump = remember { Animatable(1f) }
    var bumpTick by rememberSaveable { mutableIntStateOf(state.countTick) }
    LaunchedEffect(state.countTick) {
        if (state.countTick != bumpTick) {
            bumpTick = state.countTick
            bump.snapTo(1.18f)
            bump.animateTo(1f, tween(200))
        }
    }

    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(color = JapaColors.Surface2, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
            drawArc(
                brush = Brush.linearGradient(listOf(JapaColors.GradStart, JapaColors.GradEnd)),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                state.mantrasInRound.toString(),
                modifier = Modifier.scale(bump.value),
                fontFamily = NumberFont,
                fontSize = 64.sp,
                fontWeight = FontWeight.SemiBold,
                color = JapaColors.Text,
            )
            Text(stringResource(R.string.mantras_label), fontSize = 13.sp, color = JapaColors.TextDim, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(state.rounds.toString(), fontFamily = NumberFont, fontSize = 22.sp, color = JapaColors.Saffron, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.stat_rounds), fontSize = 12.sp, color = JapaColors.TextDim, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(formatTime(state.elapsedSec), fontFamily = NumberFont, fontSize = 18.sp, color = JapaColors.TextDim)
        }
    }
}

// ─── Mantra words ───────────────────────────────────────────────────────────

@Composable
private fun MantraWords(state: JapaState) {
    val names = mapOf(
        WordType.HARE to stringResource(R.string.word_hare),
        WordType.KRISHNA to stringResource(R.string.word_krishna),
        WordType.RAMA to stringResource(R.string.word_rama),
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MANTRA.chunked(4).forEachIndexed { line, words ->
            Row(horizontalArrangement = Arrangement.Center) {
                words.forEachIndexed { j, type ->
                    val i = line * 4 + j
                    val lit = i < state.streak
                    val next = state.guidedPlaying && state.running && i == state.streak
                    val color = when (type) {
                        WordType.HARE -> JapaColors.Saffron
                        WordType.KRISHNA -> JapaColors.Terra
                        WordType.RAMA -> JapaColors.Amber
                    }
                    val alpha = when {
                        lit -> 1f
                        next -> pulse
                        state.guidedPlaying -> 0.6f
                        else -> 1f
                    }
                    Text(
                        names.getValue(type),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp).alpha(alpha),
                        fontFamily = NumberFont,
                        fontStyle = if (lit) FontStyle.Normal else FontStyle.Italic,
                        fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 19.sp,
                        color = if (lit || next) color else JapaColors.TextDim.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

// ─── Controls ───────────────────────────────────────────────────────────────

@Composable
private fun SessionControls(state: JapaState, onEnd: () -> Unit, onReset: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onEnd,
            enabled = state.running,
            border = BorderStroke(1.dp, if (state.running) JapaColors.Saffron else JapaColors.Border),
        ) { Text(stringResource(R.string.end_session), color = if (state.running) JapaColors.SaffronDeep else JapaColors.Border) }
        TextButton(onClick = onReset) { Text(stringResource(R.string.reset), color = JapaColors.TextDim) }
    }
}

@Composable
private fun ModeCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = JapaColors.Bg2,
        border = BorderStroke(1.dp, JapaColors.Surface2),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title.uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
                color = JapaColors.Saffron,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ManualCard(state: JapaState, onCount: () -> Unit, onLock: (Boolean) -> Unit) {
    ModeCard(stringResource(R.string.mode_manual)) {
        Button(
            onClick = onCount,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JapaColors.Saffron),
        ) { Text(stringResource(R.string.count_mantra), fontSize = 18.sp) }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.volume_hint), fontSize = 12.sp, color = JapaColors.TextDim)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = JapaColors.Surface2)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.lock_mode_title), fontWeight = FontWeight.Medium, color = JapaColors.Text)
                Text(stringResource(R.string.lock_mode_help), fontSize = 12.sp, color = JapaColors.TextDim)
            }
            Spacer(Modifier.width(12.dp))
            JapaSwitch(checked = state.lockTap, onCheckedChange = onLock)
        }
    }
}

@Composable
private fun VoiceCard(state: JapaState, onToggle: (Boolean) -> Unit) {
    ModeCard(stringResource(R.string.mode_auto)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.enable_voice), modifier = Modifier.weight(1f), color = JapaColors.Text)
            JapaSwitch(checked = state.voiceEnabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(8.dp))
        val (text, dotColor) = when (state.voiceStatus) {
            VoiceStatus.LISTENING -> stringResource(R.string.listening) to JapaColors.Saffron
            VoiceStatus.DETECTED -> stringResource(R.string.detected) to Color(0xFF3E8E41)
            VoiceStatus.UNAVAILABLE -> stringResource(R.string.no_support) to JapaColors.Border
            VoiceStatus.PERMISSION_DENIED -> stringResource(R.string.mic_denied) to JapaColors.Border
            VoiceStatus.NETWORK_ERROR -> stringResource(R.string.voice_network) to JapaColors.Border
            VoiceStatus.ERROR -> stringResource(R.string.voice_error) to JapaColors.Border
            VoiceStatus.OFF -> stringResource(R.string.voice_off_status) to JapaColors.Border
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseDot(dotColor, animate = state.voiceStatus == VoiceStatus.LISTENING)
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, color = JapaColors.TextDim)
        }
        if (!state.voiceEnabled && state.voiceStatus == VoiceStatus.OFF) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.mic_rationale), fontSize = 11.sp, color = JapaColors.TextDim.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun PulseDot(color: Color, animate: Boolean) {
    val a by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dotAlpha",
    )
    Box(
        Modifier
            .size(10.dp)
            .alpha(if (animate) a else 1f)
            .background(color, CircleShape)
    )
}

@Composable
private fun GuidedCard(state: JapaState, onPace: (Pace) -> Unit, onPlay: (Boolean) -> Unit) {
    ModeCard(stringResource(R.string.mode_guided)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Pace.SLOW to R.string.slow,
                Pace.MEDIUM to R.string.medium,
                Pace.FAST to R.string.fast,
            ).forEach { (pace, label) ->
                val selected = state.pace == pace
                Surface(
                    onClick = { onPace(pace) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) JapaColors.Surface else Color.Transparent,
                    border = BorderStroke(1.dp, if (selected) JapaColors.Saffron else JapaColors.Border),
                ) {
                    Text(
                        stringResource(label),
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        color = if (selected) JapaColors.SaffronDeep else JapaColors.TextDim,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onPlay(true) },
                enabled = !state.guidedPlaying,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = JapaColors.Saffron),
            ) { Text(stringResource(R.string.play)) }
            OutlinedButton(
                onClick = { onPlay(false) },
                enabled = state.guidedPlaying,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.pause), color = if (state.guidedPlaying) JapaColors.SaffronDeep else JapaColors.Border) }
        }
    }
}

@Composable
private fun JapaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = JapaColors.Saffron,
            uncheckedTrackColor = JapaColors.Surface,
            uncheckedBorderColor = JapaColors.Border,
            uncheckedThumbColor = JapaColors.Border,
        ),
    )
}

// ─── History ────────────────────────────────────────────────────────────────

@Composable
private fun HistoryCard(history: List<Session>) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.history_title),
            fontFamily = NumberFont,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = JapaColors.Text,
        )
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text(stringResource(R.string.history_empty), fontSize = 14.sp, fontStyle = FontStyle.Italic, color = JapaColors.TextDim)
        } else {
            val dateFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
            history.forEach { s ->
                HistoryRow(
                    title = stringResource(R.string.session_label),
                    subtitle = dateFormat.format(Date(s.timestamp)),
                    note = if (s.autoSaved) stringResource(R.string.auto_saved_inactivity) else s.notes,
                    mantras = s.mantraCount,
                    rounds = s.rounds,
                    seconds = s.durationSec,
                )
                HorizontalDivider(color = JapaColors.Surface2)
            }
            HistoryRow(
                title = stringResource(R.string.total),
                subtitle = null,
                note = "",
                mantras = history.sumOf { it.mantraCount },
                rounds = history.sumOf { it.rounds },
                seconds = history.sumOf { it.durationSec },
                emphasize = true,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    title: String,
    subtitle: String?,
    note: String,
    mantras: Int,
    rounds: Int,
    seconds: Int,
    emphasize: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = JapaColors.Text, fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = JapaColors.TextDim)
            if (note.isNotEmpty()) Text(note, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = JapaColors.TextDim)
        }
        Stat(mantras.toString(), stringResource(R.string.mantras_label))
        Stat(rounds.toString(), stringResource(R.string.stat_rounds))
        Stat(formatTime(seconds), stringResource(R.string.duration))
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(Modifier.widthIn(min = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = NumberFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = JapaColors.Saffron)
        Text(label, fontSize = 10.sp, color = JapaColors.TextDim)
    }
}

// ─── Lock overlay ───────────────────────────────────────────────────────────

@Composable
private fun LockOverlay(onTap: () -> Unit, onUnlock: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(JapaColors.Text.copy(alpha = 0.04f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(50),
            color = JapaColors.Bg2,
            border = BorderStroke(1.dp, JapaColors.Saffron),
            shadowElevation = 4.dp,
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔒 " + stringResource(R.string.lock_mode_active), fontSize = 13.sp, color = JapaColors.Text)
                Spacer(Modifier.width(8.dp))
                JapaSwitch(checked = true, onCheckedChange = { if (!it) onUnlock() })
            }
        }
    }
}

internal fun formatTime(totalSec: Int): String =
    String.format(java.util.Locale.ROOT, "%02d:%02d", totalSec / 60, totalSec % 60)
