package com.kensukeyoshida.onomatopoeiadetector.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.kensukeyoshida.onomatopoeiadetector.model.AppLanguage

/**
 * 表示言語の保存と適用。未設定なら既定（英語）を使う。
 */
object LanguageSettings {

    private const val PREFS = "onomatopoeia-settings"
    private const val KEY = "appLanguage"

    fun stored(context: Context): AppLanguage =
        AppLanguage.from(prefs(context).getString(KEY, null))

    /** 選択を保存し、アプリ内ロケールへ即時反映する。 */
    fun persist(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY, language.code).apply()
        apply(language)
    }

    /** 起動時に呼ぶ。保存済み設定（なければ既定＝英語）を UI 構築前に適用する。 */
    fun applyStoredOrDefault(context: Context) {
        apply(stored(context))
    }

    private fun apply(language: AppLanguage) {
        val locales = when (language) {
            // 端末の言語設定に従う
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(language.code)
        }
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
