package com.kensukeyoshida.onomatopoeiadetector

import android.app.Application
import com.kensukeyoshida.onomatopoeiadetector.data.LanguageSettings

class OnomatopoeiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // UI 構築前に言語を適用する（既定は英語＝英語学習者向け）
        LanguageSettings.applyStoredOrDefault(this)
    }
}
