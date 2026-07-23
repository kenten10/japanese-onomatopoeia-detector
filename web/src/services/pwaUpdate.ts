import { registerSW } from 'virtual:pwa-register'

type Listener = (available: boolean) => void
const listeners = new Set<Listener>()
let updateAvailable = false
let updateServiceWorker: ((reloadPage?: boolean) => Promise<void>) | undefined

function publish(available: boolean) {
  updateAvailable = available
  listeners.forEach((listener) => listener(available))
}

export function initializePwaUpdates(): void {
  updateServiceWorker = registerSW({
    immediate: true,
    onNeedRefresh: () => publish(true)
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

