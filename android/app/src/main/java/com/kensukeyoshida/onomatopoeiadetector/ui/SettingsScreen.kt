package com.kensukeyoshida.onomatopoeiadetector.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kensukeyoshida.onomatopoeiadetector.BuildConfig
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.model.AppLanguage
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Ink
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SecondaryOpacity
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaHeading
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.monoLabel

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    // プライバシーポリシーから戻ったときにスクロール位置を保つ
    val scrollState = rememberScrollState()

    if (showPrivacy) {
        BackHandler { showPrivacy = false }
        PrivacyPolicyScreen(onBack = { showPrivacy = false })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.paper)
            .statusBarsPadding()
    ) {
        ScreenHeader(title = stringResource(R.string.settings_title))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Language
            SectionHeader(stringResource(R.string.settings_language))
            SettingsSection {
                Box {
                    FormRow(onClick = { showLanguageMenu = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_language), color = Ink.ink)
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = stringResource(language.displayNameRes()),
                                color = Ink.vermilionText
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        containerColor = Ink.panel
                    ) {
                        AppLanguage.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(option.displayNameRes()),
                                        color = if (option == language) Ink.vermilionText else Ink.ink
                                    )
                                },
                                onClick = {
                                    showLanguageMenu = false
                                    viewModel.setLanguage(option)
                                }
                            )
                        }
                    }
                }
            }

            // History
            SectionHeader(stringResource(R.string.history_title))
            SettingsSection {
                FormRow(onClick = { showClearConfirm = true }) {
                    IconLabel(
                        text = stringResource(R.string.settings_history_clear),
                        color = Ink.vermilionText,
                        spacing = 10.dp,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 17.sp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = Ink.vermilionText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // About
            SectionHeader(stringResource(R.string.settings_about))
            SettingsSection {
                FormRow {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_version), color = Ink.ink)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = monoLabel(17.sp),
                            color = Ink.ink.copy(alpha = SecondaryOpacity)
                        )
                    }
                }

                HorizontalDivider(color = Ink.ink.copy(alpha = 0.12f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about),
                        style = mangaHeading(15.sp),
                        color = Ink.ink
                    )
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        fontSize = 12.sp,
                        color = Ink.ink.copy(alpha = SecondaryOpacity)
                    )
                }

                HorizontalDivider(color = Ink.ink.copy(alpha = 0.12f))

                FormRow(onClick = { showPrivacy = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = Ink.ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.settings_privacy), color = Ink.ink)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Ink.ink.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            Spacer(Modifier.size(40.dp))
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

private fun AppLanguage.displayNameRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_auto
    AppLanguage.JAPANESE -> R.string.settings_language_ja
    AppLanguage.ENGLISH -> R.string.settings_language_en
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = monoLabel(11.sp, 1.5.sp),
        color = Ink.ink.copy(alpha = SecondaryOpacity),
        modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSection(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val dark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(lerp(Ink.panel, Ink.ink, 0.04f))
            .then(
                if (dark) Modifier.border(1.dp, Ink.ink.copy(alpha = 0.22f), shape) else Modifier
            )
    ) {
        content()
    }
}

// MARK: - Privacy policy

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.paper)
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Ink.ink
                )
            }
            Text(
                text = stringResource(R.string.settings_privacy),
                style = mangaHeading(17.sp),
                color = Ink.ink,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        HorizontalDivider(color = Ink.ink.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PrivacySection(body = stringResource(R.string.privacy_summary))
            PrivacySection(
                title = stringResource(R.string.privacy_audio_title),
                body = stringResource(R.string.privacy_audio_body)
            )
            PrivacySection(
                title = stringResource(R.string.privacy_storage_title),
                body = stringResource(R.string.privacy_storage_body)
            )
            PrivacySection(
                title = stringResource(R.string.privacy_control_title),
                body = stringResource(R.string.privacy_control_body)
            )
            Spacer(Modifier.size(32.dp))
        }
    }
}

/** 設定画面と同じセクション表現。iOS 版の `List` + `Section` に対応する。 */
@Composable
private fun PrivacySection(title: String? = null, body: String) {
    Column(Modifier.fillMaxWidth()) {
        title?.let { SectionHeader(it) }
        SettingsSection {
            Text(
                text = body,
                fontSize = 17.sp,
                color = Ink.ink,
                lineHeight = 25.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
