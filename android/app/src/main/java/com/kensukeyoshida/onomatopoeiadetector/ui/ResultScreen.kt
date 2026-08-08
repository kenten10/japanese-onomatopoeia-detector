package com.kensukeyoshida.onomatopoeiadetector.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.model.EvaluationResult
import com.kensukeyoshida.onomatopoeiadetector.model.OnomatopoeiaEntry
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Halftone
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Ink
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SecondaryOpacity
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SpeedLines
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaHeading
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaPanel
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.monoLabel
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.sfx
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/** スコアに対応する講評文。 */
fun scoreCommentRes(score: Int): Int = when (score) {
    5 -> R.string.result_comment_5
    4 -> R.string.result_comment_4
    3 -> R.string.result_comment_3
    2 -> R.string.result_comment_2
    else -> R.string.result_comment_1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    result: EvaluationResult,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ink.paper,
        contentColor = Ink.ink,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
        // ステータスバーの下から画面下端までを占める（iOS の sheet と同じ収まり）
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxHeight()
    ) {
        Column(Modifier.fillMaxSize()) {
            SheetHeader(onDismiss)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                SfxBurst(result)
                ScoreCard(result)
                if (result.score >= 3 && result.similarEntries.isNotEmpty()) {
                    SimilarSection(result)
                }
                ActionButtons(viewModel = viewModel, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(
            text = stringResource(R.string.result_title),
            style = mangaHeading(17.sp),
            color = Ink.ink,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.result_close),
                tint = Ink.ink,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    HorizontalDivider(color = Ink.ink.copy(alpha = 0.12f))
}

// MARK: - SFX burst (signature)

@Composable
private fun SfxBurst(result: EvaluationResult) {
    var appeared by remember(result.id) { mutableStateOf(false) }
    val burst by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "burst"
    )
    val burstAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "burstAlpha"
    )
    LaunchedEffect(result.id) { appeared = true }

    val fontSize = min(52.0, 220.0 / max(result.inputText.length, 3) + 20).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .mangaPanel(radius = 10.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        SpeedLines(Modifier.fillMaxSize(), color = Ink.ink.copy(alpha = 0.18f))
        Halftone(Modifier.fillMaxSize(), color = Ink.vermilion.copy(alpha = 0.10f))

        AutoShrinkText(
            text = result.inputText,
            style = sfx(fontSize),
            color = Ink.ink,
            minScale = 0.4f,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .rotate(-4f)
                .scale(burst)
                .alpha(burstAlpha)
        )

        Text(
            text = stringResource(R.string.result_recognized).uppercase(),
            style = monoLabel(10.sp, 1.5.sp),
            color = Ink.paper,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Ink.ink)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// MARK: - Score card

@Composable
private fun ScoreCard(result: EvaluationResult) {
    var starsShown by remember { mutableIntStateOf(0) }
    LaunchedEffect(result.id) {
        starsShown = 0
        for (index in 1..result.score) {
            delay(120)
            starsShown = index
        }
    }

    val starsLabel = stringResource(R.string.score_stars, result.score)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .mangaPanel(radius = 10.dp)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.result_score_label).uppercase(),
            style = monoLabel(11.sp, 2.sp),
            color = Ink.ink.copy(alpha = SecondaryOpacity)
        )

        StarRow(
            score = result.score,
            shown = starsShown,
            starSize = 34.dp,
            spacing = 6.dp,
            contentDescription = starsLabel
        )

        Row {
            Text(
                text = result.score.toString(),
                style = sfx(48.sp),
                color = Ink.score(result.score),
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = "/ 5",
                style = mangaHeading(22.sp),
                color = Ink.ink.copy(alpha = 0.5f),
                modifier = Modifier.alignByBaseline()
            )
        }

        Text(
            text = stringResource(scoreCommentRes(result.score)),
            style = mangaHeading(16.sp),
            color = Ink.ink,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StarRow(
    score: Int,
    shown: Int,
    starSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 1..5) {
            val earned = index <= score
            val revealed = index <= shown
            val scale by animateFloatAsState(
                targetValue = if (revealed) 1f else 0.7f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "star$index"
            )
            Icon(
                imageVector = if (revealed) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (index == 1) contentDescription else null,
                tint = if (earned) Ink.score(score) else Ink.ink.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(starSize)
                    .scale(scale)
            )
        }
    }
}

// MARK: - Similar

@Composable
private fun SimilarSection(result: EvaluationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconLabel(
            text = stringResource(R.string.result_similar_title),
            color = Ink.ink,
            style = mangaHeading(17.sp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Ink.ink,
                modifier = Modifier.size(18.dp)
            )
        }

        result.similarEntries.forEach { similar ->
            SimilarEntryCard(similar.entry)
        }
    }
}

@Composable
private fun SimilarEntryCard(entry: OnomatopoeiaEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .mangaPanel(radius = 8.dp, offset = 3.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row {
            Text(
                text = entry.word,
                style = sfx(24.sp),
                color = Ink.ink,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = "（${entry.reading}）",
                fontSize = 12.sp,
                color = Ink.ink.copy(alpha = SecondaryOpacity),
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = entry.category,
                style = monoLabel(11.sp),
                color = Ink.paper,
                modifier = Modifier
                    .alignByBaseline()
                    .clip(CircleShape)
                    .background(Ink.vermilionText)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        HorizontalDivider(color = Ink.ink.copy(alpha = 0.15f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MeaningRow(flag = "🇯🇵", text = entry.meaningJa)
            MeaningRow(flag = "🇬🇧", text = entry.meaningEn)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconLabel(
                text = stringResource(R.string.result_similar_example),
                color = Ink.ink.copy(alpha = SecondaryOpacity),
                spacing = 6.dp,
                style = monoLabel(11.sp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Ink.ink.copy(alpha = SecondaryOpacity),
                    modifier = Modifier.size(13.dp)
                )
            }
            Text("・${entry.exampleJa}", fontSize = 12.sp, color = Ink.ink.copy(alpha = 0.7f))
            Text("・${entry.exampleEn}", fontSize = 12.sp, color = Ink.ink.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun MeaningRow(flag: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = flag, fontSize = 17.sp)
        Text(text = text, fontSize = 15.sp, color = Ink.ink)
    }
}

// MARK: - Actions

@Composable
private fun ActionButtons(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val saved by viewModel.savedFeedback.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MangaButton(
            onClick = viewModel::saveCurrentResult,
            background = if (saved) Ink.score(5) else Ink.ink,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconLabel(
                text = stringResource(if (saved) R.string.result_saved else R.string.result_save),
                color = Ink.paper,
                style = mangaHeading(16.sp)
            ) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Check else Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = Ink.paper,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        MangaButton(
            onClick = onDismiss,
            background = Ink.panel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.result_again),
                style = mangaHeading(16.sp),
                color = Ink.ink,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
