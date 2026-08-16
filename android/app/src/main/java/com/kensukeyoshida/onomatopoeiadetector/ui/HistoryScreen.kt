package com.kensukeyoshida.onomatopoeiadetector.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.model.HistoryItem
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Ink
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SecondaryOpacity
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaHeading
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaPanel
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.monoLabel
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.sfx

@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.paper)
            .statusBarsPadding()
    ) {
        ScreenHeader(title = stringResource(R.string.history_title)) {
            if (history.isNotEmpty()) {
                TextButton(onClick = { showClearConfirm = true }) {
                    Text(
                        text = stringResource(R.string.history_clear_all),
                        color = Ink.vermilionText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (history.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryRow(
                        item = item,
                        onDelete = { viewModel.deleteHistoryItem(item) }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Ink.panel,
            titleContentColor = Ink.ink,
            title = {
                Text(stringResource(R.string.history_clear_confirm), style = mangaHeading(17.sp))
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAllHistory()
                }) {
                    Text(stringResource(R.string.history_clear_yes), color = Ink.vermilionText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.history_clear_cancel), color = Ink.ink)
                }
            }
        )
    }
}

/** 画面上部のタイトル行。iOS の large title に相当する。 */
@Composable
fun ScreenHeader(
    title: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text = title, style = mangaHeading(34.sp), color = Ink.ink)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

// MARK: - Empty state

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // マンガで「閑散・空っぽ」を表す描き文字
        Text(
            text = "がらーん",
            style = sfx(52.sp),
            color = Ink.ink.copy(alpha = 0.16f),
            modifier = Modifier
                .rotate(-5f)
                .clearAndSetSemantics { }
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.history_empty),
            style = mangaHeading(16.sp),
            color = Ink.ink.copy(alpha = SecondaryOpacity)
        )
    }
}

// MARK: - HistoryRow

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(item: HistoryItem, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // 表示言語を切り替えたときに日時表記も追従させる
    val timestamp = remember(item.date, LocalConfiguration.current) {
        DateUtils.formatDateTime(
            context,
            item.date,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME
        )
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mangaPanel(radius = 8.dp, offset = 3.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // スコアバッジ
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Ink.score(item.score), CircleShape)
                    .border(2.dp, Ink.ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.score.toString(),
                    style = sfx(20.sp),
                    color = Ink.paper
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = item.inputText,
                    style = mangaHeading(18.sp),
                    color = Ink.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(
                        score = item.score,
                        shown = item.score,
                        starSize = 11.dp,
                        spacing = 3.dp
                    )
                    Text(
                        text = timestamp,
                        style = monoLabel(11.sp),
                        color = Ink.ink.copy(alpha = SecondaryOpacity),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = Ink.panel
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.history_delete), color = Ink.vermilionText)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = Ink.vermilionText
                    )
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}
