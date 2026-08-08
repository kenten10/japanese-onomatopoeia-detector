package com.kensukeyoshida.onomatopoeiadetector

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kensukeyoshida.onomatopoeiadetector.ui.AppViewModel
import com.kensukeyoshida.onomatopoeiadetector.ui.MainScaffold
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.MangaTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MangaTheme {
                val viewModel: AppViewModel = viewModel()
                MainScaffold(viewModel)
            }
        }
    }
}
