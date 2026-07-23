import type { HistoryItem } from '../types'

const DB_NAME = 'OnomatopoeiaDetector'
const STORE_NAME = 'history'
const DB_VERSION = 1
const MAX_ITEMS = 100

interface StoredHistoryItem extends Omit<HistoryItem, 'date'> { date: string }

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const database = request.result
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        const store = database.createObjectStore(STORE_NAME, { keyPath: 'id' })
        store.createIndex('date', 'date')
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

function complete(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error)
    transaction.onabort = () => reject(transaction.error)
  })
}

export class HistoryRepository {
  async fetch(): Promise<HistoryItem[]> {
    const database = await openDatabase()
    const request = database.transaction(STORE_NAME).objectStore(STORE_NAME).getAll()
    const stored = await new Promise<StoredHistoryItem[]>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
    database.close()
    return stored.sort((a, b) => b.date.localeCompare(a.date)).slice(0, MAX_ITEMS)
      .map((item) => ({ ...item, date: new Date(item.date) }))
  }

  async add(inputText: string, score: number): Promise<void> {
    const database = await openDatabase()
    const transaction = database.transaction(STORE_NAME, 'readwrite')
    const store = transaction.objectStore(STORE_NAME)
    store.put({ id: crypto.randomUUID(), inputText, score, date: new Date().toISOString() } satisfies StoredHistoryItem)
    await complete(transaction)
    database.close()
    await this.prune()
  }

  async delete(id: string): Promise<void> {
    const database = await openDatabase()
    const transaction = database.transaction(STORE_NAME, 'readwrite')
    transaction.objectStore(STORE_NAME).delete(id)
    await complete(transaction)
    database.close()
  }

  async clear(): Promise<void> {
    const database = await openDatabase()
    const transaction = database.transaction(STORE_NAME, 'readwrite')
    transaction.objectStore(STORE_NAME).clear()
    await complete(transaction)
    database.close()
  }

  private async prune(): Promise<void> {
    const items = await this.fetch()
    if (items.length < MAX_ITEMS) return
    const database = await openDatabase()
    const request = database.transaction(STORE_NAME).objectStore(STORE_NAME).getAll()
    const stored = await new Promise<StoredHistoryItem[]>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
    database.close()
    const excess = stored.sort((a, b) => b.date.localeCompare(a.date)).slice(MAX_ITEMS)
    if (excess.length === 0) return
    const writable = await openDatabase()
    const transaction = writable.transaction(STORE_NAME, 'readwrite')
    excess.forEach((item) => transaction.objectStore(STORE_NAME).delete(item.id))
    await complete(transaction)
    writable.close()
  }
}

