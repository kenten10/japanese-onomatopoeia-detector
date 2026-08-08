package com.kensukeyoshida.onomatopoeiadetector.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Ink
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaHeading

/**
 * 押し込みでオフセット影が縮むマンガ調のボタン。
 */
@Composable
fun MangaButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val inkColor = Ink.ink
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed) 3.dp else 0.dp, tween(80), label = "shift")
    val shadow by animateDpAsState(if (pressed) 1.dp else 4.dp, tween(80), label = "shadow")
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .offset(shift, shift)
            .drawBehind {
                drawRoundRect(
                    color = inkColor,
                    topLeft = Offset(shadow.toPx(), shadow.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .clip(shape)
            .background(background)
            .border(2.5.dp, inkColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick
            )
            .defaultMinSize(minHeight = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * アイコンとラベルを横に並べる（SwiftUI の `Label` 相当）。
 */
@Composable
fun IconLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    style: androidx.compose.ui.text.TextStyle = mangaHeading(16.sp),
    icon: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(text = text, style = style, color = color)
    }
}

/**
 * リスト行として使う、余白と最小高さを揃えたタップ可能な行。
 */
@Composable
fun FormRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 50.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}
