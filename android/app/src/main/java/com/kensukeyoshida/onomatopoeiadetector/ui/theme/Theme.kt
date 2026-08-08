package com.kensukeyoshida.onomatopoeiadetector.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kensukeyoshida.onomatopoeiadetector.R
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// MARK: - Manga sound-effect (描き文字) design system
//
// オノマトペが最も生きる世界＝マンガの効果音。
// 白い紙・黒インク・朱色のアクセント・網点（スクリーントーン）を基調に、
// 認識した言葉を効果音のように大きく弾けさせる。

// MARK: Palette

data class MangaColors(
    /** 紙。ライトは温かみのある白、ダークは墨色の紙。 */
    val paper: Color,
    /** インク。線と文字の主色。 */
    val ink: Color,
    /** 朱。効果音のエネルギーを担う唯一のアクセント。 */
    val vermilion: Color,
    /** パネルの下地（紙よりわずかに浮いた面）。 */
    val panel: Color
)

private val LightColors = MangaColors(
    paper = Color(0xFFFBFAF6),
    ink = Color(0xFF141414),
    vermilion = Color(0xFFE8412E),
    panel = Color(0xFFFFFFFE)
)

private val DarkColors = MangaColors(
    paper = Color(0xFF17150F),
    ink = Color(0xFFF2EEE4),
    vermilion = Color(0xFFE8412E),
    panel = Color(0xFF211E19)
)

val LocalMangaColors = staticCompositionLocalOf { LightColors }

object Ink {
    val paper: Color @Composable @ReadOnlyComposable get() = LocalMangaColors.current.paper
    val ink: Color @Composable @ReadOnlyComposable get() = LocalMangaColors.current.ink
    val vermilion: Color @Composable @ReadOnlyComposable get() = LocalMangaColors.current.vermilion
    val panel: Color @Composable @ReadOnlyComposable get() = LocalMangaColors.current.panel

    /** スコアに応じたインクの「熱量」。高いほど朱に近づき、低いほど淡い墨になる。 */
    @Composable
    @ReadOnlyComposable
    fun score(value: Int): Color {
        val colors = LocalMangaColors.current
        return when (value) {
            5 -> colors.vermilion
            4 -> colors.ink
            3 -> colors.ink.copy(alpha = 0.70f)
            2 -> colors.ink.copy(alpha = 0.48f)
            else -> colors.ink.copy(alpha = 0.32f)
        }
    }
}

// MARK: Type roles

val MangaFontFamily = FontFamily(
    Font(R.font.mplus_rounded1c_bold, FontWeight.Bold),
    Font(R.font.mplus_rounded1c_extrabold, FontWeight.ExtraBold),
    Font(R.font.mplus_rounded1c_black, FontWeight.Black)
)

/** 描き文字。認識した言葉を効果音として弾けさせる用。 */
fun sfx(size: TextUnit): TextStyle = TextStyle(
    fontFamily = MangaFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = size,
    lineHeight = size * 1.1f,
    letterSpacing = (-0.02).em
)

/** 見出し。太いラウンド。 */
fun mangaHeading(size: TextUnit): TextStyle = TextStyle(
    fontFamily = MangaFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = size,
    lineHeight = size * 1.3f
)

/** 版面の小口ラベル。等幅・字間広め・大文字。 */
fun monoLabel(size: TextUnit, tracking: TextUnit = 0.sp): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    letterSpacing = tracking
)

// MARK: Halftone (網点 / スクリーントーン)

@Composable
fun Halftone(
    modifier: Modifier = Modifier,
    color: Color = Ink.ink,
    dot: Dp = 2.4.dp,
    spacing: Dp = 11.dp
) {
    Canvas(modifier) {
        val step = spacing.toPx()
        val diameter = dot.toPx()
        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = col * step + if (row % 2 == 0) 0f else step / 2
                val y = row * step
                drawOval(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(diameter, diameter)
                )
            }
        }
    }
}

// MARK: Speed lines (集中線)

@Composable
fun SpeedLines(
    modifier: Modifier = Modifier,
    color: Color = Ink.ink,
    count: Int = 56,
    innerRatio: Float = 0.30f
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = max(size.width, size.height)
        val inner = maxRadius * innerRatio
        for (i in 0 until count) {
            val angle = (i.toDouble() / count) * 2 * Math.PI
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            drawLine(
                color = color,
                start = Offset(center.x + dx * inner, center.y + dy * inner),
                end = Offset(center.x + dx * maxRadius, center.y + dy * maxRadius),
                strokeWidth = if (i % 2 == 0) 2.dp.toPx() else 0.8.dp.toPx()
            )
        }
    }
}

// MARK: Manga panel (コマ枠)

/** マンガのコマ枠。太いインク線とハードなオフセット影で「印刷された紙」の質感を出す。 */
@Composable
fun Modifier.mangaPanel(
    radius: Dp = 6.dp,
    border: Dp = 2.5.dp,
    offset: Dp = 4.dp
): Modifier {
    val inkColor = Ink.ink
    val panelColor = Ink.panel
    return drawWithCache {
        val corner = CornerRadius(radius.toPx())
        val shift = offset.toPx()
        val stroke = Stroke(width = border.toPx())
        onDrawBehind {
            // 印刷のズレを思わせるハードなオフセット影
            drawRoundRect(
                color = inkColor,
                topLeft = Offset(shift, shift),
                size = size,
                cornerRadius = corner
            )
            drawRoundRect(color = panelColor, size = size, cornerRadius = corner)
            drawRoundRect(color = inkColor, size = size, cornerRadius = corner, style = stroke)
        }
    }
}

// MARK: Theme

@Composable
fun MangaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.vermilion,
            onPrimary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceVariant = colors.panel,
            onSurfaceVariant = colors.ink,
            error = colors.vermilion
        )
    } else {
        lightColorScheme(
            primary = colors.vermilion,
            onPrimary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceVariant = colors.panel,
            onSurfaceVariant = colors.ink,
            error = colors.vermilion
        )
    }

    CompositionLocalProvider(LocalMangaColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
