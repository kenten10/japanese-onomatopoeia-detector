import * as Sentry from '@sentry/browser'

/**
 * 不具合の報告。
 *
 * 送るのは「アプリが落ちた・失敗した」ことを直すために要る情報だけに絞る。
 * 音声、認識したことば、判定履歴は送らない。DSN を設定しなければ何も送らない。
 */

/** 送信先。ビルド時に埋め込む。未設定なら報告そのものを行わない。 */
const dsn = import.meta.env.VITE_SENTRY_DSN

export function initializeErrorReporting(): void {
  if (!dsn) return

  Sentry.init({
    dsn,
    // 既定で付く IP アドレスやユーザー情報を送らない
    sendDefaultPii: false,
    // 画面の操作記録は本文をなぞってしまうので採らない
    integrations: (defaults) => defaults.filter((integration) => integration.name !== 'Breadcrumbs'),
    // 性能計測は行わない
    tracesSampleRate: 0,
    beforeSend: scrubEvent
  })
}

/**
 * 送る直前に、本文が混じりうる場所を落とす。
 *
 * 認識したことばは例外メッセージやリクエスト URL に紛れ込みうるため、
 * 想定していない経路から漏れないよう、ここでまとめて取り除く。
 */
function scrubEvent(event: Sentry.ErrorEvent): Sentry.ErrorEvent {
  delete event.user
  delete event.request
  delete event.breadcrumbs
  if (event.contexts) delete event.contexts.device
  return event
}

/** 捕捉した失敗を報告する。DSN 未設定なら何もしない。 */
export function reportError(error: unknown, context: string): void {
  if (!dsn) return
  Sentry.captureException(error, { tags: { context } })
}
