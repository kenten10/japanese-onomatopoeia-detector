import { useRef, useState, type PointerEvent } from 'react'
import { useApp } from '../AppContext'
import { useI18n } from '../i18n/I18nContext'
import type { HistoryItem } from '../types'
import { StarIcon, TrashIcon } from '../components/Icons'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ModalPortal } from '../components/ModalPortal'

function HistoryRow({ item, onDelete, locale }: { item: HistoryItem; onDelete(): void; locale: string }) {
  const timer = useRef<number | undefined>(undefined)
  const { t } = useI18n()
  function beginLongPress(event: PointerEvent) {
    if (event.pointerType !== 'mouse') timer.current = window.setTimeout(onDelete, 600)
  }
  function cancelLongPress() { if (timer.current) window.clearTimeout(timer.current) }
  return <article className="manga-panel history-row" onContextMenu={(event) => { event.preventDefault(); onDelete() }} onPointerDown={beginLongPress} onPointerUp={cancelLongPress} onPointerCancel={cancelLongPress}>
    <div className={`score-badge score-${item.score}`}>{item.score}</div>
    <div className="history-copy"><strong>{item.inputText}</strong><div className={`mini-stars score-${item.score}`}>
      {[1, 2, 3, 4, 5].map((star) => <StarIcon key={star} filled={star <= item.score} />)}
      <time>{new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(item.date)}</time>
    </div></div>
    <button className="row-delete" onClick={onDelete} aria-label={t('history.delete')}><TrashIcon /></button>
  </article>
}

export function HistoryView() {
  const { t, locale } = useI18n()
  const { history, deleteHistory, clearHistory } = useApp()
  const [confirmClear, setConfirmClear] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<HistoryItem>()

  return <main className="screen list-screen">
    <header className="navigation-header"><h1>{t('history.title')}</h1>{history.length > 0 && <button onClick={() => setConfirmClear(true)}>{t('history.clear.all')}</button>}</header>
    {history.length === 0 ? <div className="empty-history"><div>がらーん</div><p>{t('history.empty')}</p></div> : <div className="history-list">
      {history.map((item) => <HistoryRow key={item.id} item={item} locale={locale} onDelete={() => setDeleteTarget(item)} />)}
    </div>}
    <ConfirmDialog open={confirmClear} onCancel={() => setConfirmClear(false)} onConfirm={() => { setConfirmClear(false); void clearHistory() }} />
    {deleteTarget && <ModalPortal onClose={() => setDeleteTarget(undefined)} dismissOnBackdrop><section className="context-dialog" role="dialog" aria-modal="true">
      <button data-autofocus className="destructive" onClick={() => { void deleteHistory(deleteTarget.id); setDeleteTarget(undefined) }}><TrashIcon />{t('history.delete')}</button>
      <button onClick={() => setDeleteTarget(undefined)}>{t('history.clear.cancel')}</button>
    </section></ModalPortal>}
  </main>
}
