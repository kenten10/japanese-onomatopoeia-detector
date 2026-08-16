/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/client" />


interface ImportMetaEnv {
  /** 不具合の送信先。設定しなければ報告は行わない。 */
  readonly VITE_SENTRY_DSN?: string
  /** kuromoji の辞書の場所。テストでのみ差し替える。 */
  readonly VITE_KUROMOJI_DICT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

/** ご意見フォームの URL。ビルド時に埋め込む。空なら導線を出さない。 */
declare const __FEEDBACK_FORM_URL__: string
