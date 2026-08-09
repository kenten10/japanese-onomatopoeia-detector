import type { HistoryItem } from '../types'

const DB_NAME = 'OnomatopoeiaDetector'
const STORE_NAME = 'history'
const DATE_INDEX = 'date'
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
        store.createIndex(DATE_INDEX, 'date')
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
  /** 接続は開いたまま使い回す。操作ごとに開閉すると、失敗時に閉じ損ねて接続が残る。 */
  private connection?: Promise<IDBDatabase>

  /** 直近に発行した保存時刻。同じミリ秒に並ぶのを避けるために覚えておく。 */
  private lastIssued = 0

  /**
   * 保存時刻を単調増加で発行する。
   *
   * `Date.now()` はミリ秒までしか刻めず、続けて保存すると同じ値になる。日付が同値だと
   * 索引の並びは主キー（ランダムな UUID）順になり、新しい順に並べることも、
   * 古い方から間引くこともできなくなる。
   */
  private nextTimestamp(): number {
    const now = Date.now()
    this.lastIssued = now > this.lastIssued ? now : this.lastIssued + 1
    return this.lastIssued
  }

  private database(): Promise<IDBDatabase> {
    this.connection ??= openDatabase()
      .then((database) => {
        // 別のタブがアップグレードしたら、この接続は手放して次回開き直す
        database.onversionchange = () => {
          database.close()
          this.connection = undefined
        }
        database.onclose = () => { this.connection = undefined }
        return database
      })
      .catch((error: unknown) => {
        this.connection = undefined
        throw error
      })
    return this.connection
  }

  /** 新しい順に最大 100 件。日付インデックスを逆順にたどるので全件は読まない。 */
  async fetch(): Promise<HistoryItem[]> {
    const database = await this.database()
    const transaction = database.transaction(STORE_NAME)
    const request = transaction.objectStore(STORE_NAME).index(DATE_INDEX).openCursor(null, 'prev')
    const items: HistoryItem[] = []

    await new Promise<void>((resolve, reject) => {
      request.onsuccess = () => {
        const cursor = request.result
        if (!cursor || items.length >= MAX_ITEMS) { resolve(); return }
        const stored = cursor.value as StoredHistoryItem
        items.push({ ...stored, date: new Date(stored.date) })
        cursor.continue()
      }
      request.onerror = () => reject(request.error)
    })

    return items
  }

  /** 追加と間引きは同じトランザクションで行う（間引きだけ失敗して保存済みを見失わないため）。 */
  async add(inputText: string, score: number): Promise<void> {
    const database = await this.database()
    const transaction = database.transaction(STORE_NAME, 'readwrite')
    const store = transaction.objectStore(STORE_NAME)

    store.put({
      id: crypto.randomUUID(),
      inputText,
      score,
      date: new Date(this.nextTimestamp()).toISOString()
    } satisfies StoredHistoryItem)

    const cursorRequest = store.index(DATE_INDEX).openCursor(null, 'prev')
    let seen = 0
    cursorRequest.onsuccess = () => {
      const cursor = cursorRequest.result
      if (!cursor) return
      seen += 1
      if (seen > MAX_ITEMS) cursor.delete()
      cursor.continue()
    }

    await complete(transaction)
  }

  async delete(id: string): Promise<void> {
    const database = await this.database()
    const transaction = database.transaction(STORE_NAME, 'readwrite')
    transaction.objectStore(STORE_NAME).delete(id)
    await complete(transaction)
  }

  async clear(): Promise<void> {
    const database = await this.database()
    const transaction = database.transaction(STORE_NAME, 'readwrite')
    transaction.objectStore(STORE_NAME).clear()
    await complete(transaction)
  }
}
