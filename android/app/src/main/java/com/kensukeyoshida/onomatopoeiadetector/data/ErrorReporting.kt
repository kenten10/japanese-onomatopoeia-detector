package com.kensukeyoshida.onomatopoeiadetector.data

import android.content.Context
import com.kensukeyoshida.onomatopoeiadetector.BuildConfig
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

/**
 * 不具合の報告。
 *
 * 送るのは「アプリが落ちた・失敗した」ことを直すために要る情報だけに絞る。
 * 音声、認識したことば、判定履歴は送らない。DSN を設定しなければ何も送らない。
 */
object ErrorReporting {

    val isEnabled: Boolean get() = BuildConfig.SENTRY_DSN.isNotEmpty()

    fun initialize(context: Context) {
        if (!isEnabled) return

        SentryAndroid.init(context) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}"

            // 既定で付く IP アドレスや利用者情報を送らない
            options.isSendDefaultPii = false
            // 画面の操作記録は入力内容をなぞってしまうので採らない
            options.isEnableUserInteractionBreadcrumbs = false
            options.isEnableUserInteractionTracing = false
            // 画面の写しは認識したことばが写り込むため採らない
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            // 性能計測は行わない
            options.tracesSampleRate = 0.0

            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ -> scrub(event) }
        }
    }

    /** 送る直前に、本文が混じりうる場所を落とす。 */
    private fun scrub(event: SentryEvent): SentryEvent {
        event.user = null
        event.request = null
        event.breadcrumbs = null
        return event
    }
}
