import { registerSW } from 'virtual:pwa-register'

type Listener = (available: boolean) => void
const listeners = new Set<Listener>()
let updateAvailable = false
let updateServiceWorker: ((reloadPage?: boolean) => Promise<void>) | undefined

function publish(available: boolean) {
  updateAvailable = available
  listeners.forEach((listener) => listener(available))
}

/** 更新の確認間隔。ホーム画面から起動した PWA は開きっぱなしになりやすい。 */
const UPDATE_CHECK_INTERVAL_MS = 60 * 60 * 1000

export function initializePwaUpdates(): void {
  updateServiceWorker = registerSW({
    immediate: true,
    onNeedRefresh: () => publish(true),
    onRegisteredSW: (_url, registration) => {
      if (!registration) return

      // 更新の検出はページ読み込み時だけでは足りない。ホーム画面に追加した PWA は
      // プロセスが生き続けるため、読み込みが起きず新しい版に気付けない。
      const check = () => {
        if (document.visibilityState === 'visible') void registration.update()
      }
      window.setInterval(check, UPDATE_CHECK_INTERVAL_MS)
      // 背面から戻ってきたときにも確認する
      document.addEventListener('visibilitychange', check)
    }
  })
}

export function subscribeToPwaUpdates(listener: Listener): () => void {
  listeners.add(listener)
  listener(updateAvailable)
  return () => listeners.delete(listener)
}

export async function applyPwaUpdate(): Promise<void> {
  await updateServiceWorker?.(true)
  publish(false)
}

