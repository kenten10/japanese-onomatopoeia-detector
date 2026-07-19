import { beforeEach, describe, expect, it } from 'vitest'
import { HistoryRepository } from './historyRepository'

describe('HistoryRepository', () => {
  const repository = new HistoryRepository()
  beforeEach(async () => repository.clear())

  it('adds, fetches and deletes local history', async () => {
    await repository.add('ふわふわ', 5)
    await repository.add('さらさら', 4)
    const history = await repository.fetch()
    expect(history).toHaveLength(2)
    expect(history.map((item) => item.score).sort()).toEqual([4, 5])
    await repository.delete(history[0].id)
    expect(await repository.fetch()).toHaveLength(1)
  })

  it('prunes local history to one hundred entries', async () => {
    for (let index = 0; index < 105; index += 1) await repository.add(`word-${index}`, 3)
    expect(await repository.fetch()).toHaveLength(100)
    expect(await rawHistoryCount()).toBe(100)
  })
})

function rawHistoryCount(): Promise<number> {
  return new Promise((resolve, reject) => {
    const open = indexedDB.open('OnomatopoeiaDetector', 1)
    open.onerror = () => reject(open.error)
    open.onsuccess = () => {
      const database = open.result
      const request = database.transaction('history').objectStore('history').count()
      request.onerror = () => reject(request.error)
      request.onsuccess = () => { database.close(); resolve(request.result) }
    }
  })
}
