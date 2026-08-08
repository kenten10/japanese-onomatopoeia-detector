package com.kensukeyoshida.onomatopoeiadetector.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.model.RecordingState
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Halftone
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Ink
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SecondaryOpacity
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SpeedLines
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaHeading
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.monoLabel
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.sfx
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val state by viewModel.recordingState.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isRecording = state is RecordingState.Recording
    val isProcessing = state is RecordingState.Recognizing || state is RecordingState.Evaluating
    val error = state as? RecordingState.Error

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording() else viewModel.showPermissionDenied()
    }

    // 画面が背面に回ったらマイクを掴んだままにしない
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stopRecordingOnBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.paper)
    ) {
        // 上部にだけ薄く網点を敷き、紙の質感を出す
        Halftone(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            color = Ink.ink.copy(alpha = 0.06f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppHeader()
            Spacer(Modifier.weight(1f))
            CenterContent(state = state, partialText = partialText)
            Spacer(Modifier.weight(1f))
            MicButton(
                isRecording = isRecording,
                isProcessing = isProcessing,
                onClick = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else if (viewModel.speech.hasMicrophonePermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
            Spacer(Modifier.height(44.dp))
        }
    }

    (state as? RecordingState.Result)?.let { resultState ->
        ResultSheet(
            result = resultState.result,
            viewModel = viewModel,
            onDismiss = viewModel::resetToIdle
        )
    }

    error?.let { errorState ->
        AlertDialog(
            onDismissRequest = viewModel::resetToIdle,
            containerColor = Ink.panel,
            titleContentColor = Ink.ink,
            textContentColor = Ink.ink,
            title = { Text(stringResource(R.string.error_title), style = mangaHeading(17.sp)) },
            text = { Text(stringResource(errorState.messageRes)) },
            confirmButton = {
                if (errorState.permissionsDenied) {
                    TextButton(onClick = {
                        viewModel.resetToIdle()
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }) {
                        Text(stringResource(R.string.permission_mic_open_settings), color = Ink.vermilion)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::resetToIdle) {
                    Text(stringResource(R.string.error_ok), color = Ink.vermilion)
                }
            }
        )
    }
}

// MARK: - Header

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier.padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 見出しの幅にそろえるため、下線は文字幅を基準に敷く
        Box(
            modifier = Modifier.width(IntrinsicSize.Max),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(R.string.app_title),
                style = mangaHeading(30.sp),
                color = Ink.ink,
                maxLines = 1
            )
            // 見出しの下に朱色の力線
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Ink.vermilion)
            )
        }

        Spacer(Modifier.height(13.dp))

        Text(
            text = stringResource(R.string.app_subtitle).uppercase(),
            style = monoLabel(11.sp, 2.sp),
            color = Ink.ink.copy(alpha = SecondaryOpacity)
        )
    }
}

// MARK: - Center

@Composable
private fun CenterContent(state: RecordingState, partialText: String) {
    when (state) {
        is RecordingState.Recording -> RecordingContent(partialText)
        is RecordingState.Recognizing, is RecordingState.Evaluating -> ProcessingContent()
        else -> IdleContent()
    }
}

@Composable
private fun IdleContent() {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // マンガで「静寂」を表す描き文字。無音状態そのものを主役に。
        Text(
            text = "シーン…",
            style = sfx(64.sp),
            color = Ink.ink.copy(alpha = 0.16f),
            modifier = Modifier
                .rotate(-6f)
                .clearAndSetSemantics { }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_no_result),
                style = mangaHeading(22.sp),
                color = Ink.ink
            )
            Text(
                text = stringResource(R.string.home_no_result_sub),
                fontSize = 15.sp,
                color = Ink.ink.copy(alpha = SecondaryOpacity),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecordingContent(partialText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        WaveformView(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 40.dp)
        )

        AutoShrinkText(
            text = partialText.ifEmpty { stringResource(R.string.recording_listening) },
            style = sfx(if (partialText.isEmpty()) 24.sp else 40.sp),
            color = if (partialText.isEmpty()) Ink.ink.copy(alpha = 0.5f) else Ink.ink,
            minScale = 0.5f,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Text(
            text = stringResource(R.string.recording_tap_stop),
            style = monoLabel(12.sp, 1.sp),
            color = Ink.vermilionText
        )
    }
}

@Composable
private fun ProcessingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Ink.vermilion,
            strokeWidth = 3.dp
        )
        Text(
            text = stringResource(R.string.recording_recognizing),
            style = monoLabel(13.sp, 1.sp),
            color = Ink.ink.copy(alpha = SecondaryOpacity)
        )
    }
}

// MARK: - Mic button

@Composable
private fun MicButton(
    isRecording: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(
        if (isRecording) R.string.recording_tap_stop else R.string.home_record_button
    )
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            // 録音中だけ集中線のぶんまで領域を広げる（iOS の ZStack と同じ）
            .size(if (isRecording) 168.dp else 90.dp)
            .alpha(if (isProcessing) 0.4f else 1f)
            .clickable(
                enabled = !isProcessing,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        // 録音中は集中線を放射
        if (isRecording) {
            SpeedLines(
                modifier = Modifier
                    .size(168.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha),
                color = Ink.vermilion.copy(alpha = 0.55f),
                count = 40
            )
        }

        // ハードなオフセット影
        Box(
            Modifier
                .size(86.dp)
                .offset(4.dp, 4.dp)
                .background(Ink.ink, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(86.dp)
                .background(if (isRecording) Ink.ink else Ink.vermilion, CircleShape)
                .border(3.dp, Ink.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = null,
                tint = Ink.paper,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

// MARK: - WaveformView

@Composable
fun WaveformView(modifier: Modifier = Modifier) {
    val bars = remember { mutableStateListOf(*Array(13) { Random.nextFloat() * 0.8f + 0.2f }) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            for (index in bars.indices) bars[index] = Random.nextFloat() * 0.85f + 0.15f
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEachIndexed { index, target ->
            val height by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "bar$index"
            )
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .scale(scaleX = 1f, scaleY = height)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (index % 3 == 0) Ink.vermilion else Ink.ink)
            )
        }
    }
}
