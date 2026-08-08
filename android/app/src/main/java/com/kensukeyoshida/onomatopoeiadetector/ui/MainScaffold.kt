package com.kensukeyoshida.onomatopoeiadetector.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kensukeyoshida.onomatopoeiadetector.R
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.Ink
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.SecondaryOpacity
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.mangaHeading

private enum class MainTab(val labelRes: Int) {
    HOME(R.string.home_tab),
    HISTORY(R.string.history_title),
    SETTINGS(R.string.settings_title)
}

@Composable
fun MainScaffold(viewModel: AppViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    val persistenceError by viewModel.persistenceErrorMessage.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        containerColor = Ink.paper,
        contentColor = Ink.ink,
        bottomBar = {
            NavigationBar(
                containerColor = Ink.paper,
                contentColor = Ink.ink,
                tonalElevation = 0.dp
            ) {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MainTab.HOME -> Icons.Filled.GraphicEq
                                    MainTab.HISTORY -> Icons.AutoMirrored.Filled.MenuBook
                                    MainTab.SETTINGS -> Icons.Filled.Settings
                                },
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(text = stringResource(tab.labelRes), fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink.vermilionText,
                            selectedTextColor = Ink.vermilionText,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = Ink.ink.copy(alpha = SecondaryOpacity),
                            unselectedTextColor = Ink.ink.copy(alpha = SecondaryOpacity)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // 上部の余白は各画面が自分で扱う（iOS の NavigationStack と同じ考え方）
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            top = 0.dp,
            bottom = innerPadding.calculateBottomPadding()
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = contentPadding.calculateBottomPadding())
        ) {
            when (MainTab.entries[selectedTab]) {
                MainTab.HOME -> HomeScreen(viewModel)
                MainTab.HISTORY -> HistoryScreen(viewModel)
                MainTab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }

    persistenceError?.let { messageRes ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPersistenceError,
            containerColor = Ink.panel,
            titleContentColor = Ink.ink,
            textContentColor = Ink.ink,
            title = { Text(stringResource(R.string.error_title), style = mangaHeading(17.sp)) },
            text = { Text(stringResource(messageRes)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPersistenceError) {
                    Text(stringResource(R.string.error_ok), color = Ink.vermilionText)
                }
            }
        )
    }
}
