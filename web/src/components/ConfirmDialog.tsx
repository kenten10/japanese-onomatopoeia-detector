import { useEffect, useRef } from 'react'
import { useI18n } from '../i18n/I18nContext'

export function ConfirmDialog({ open, onConfirm, onCancel }: { open: boolean; onConfirm(): void; onCancel(): void }) {
  const { t } = useI18n()
  const cancelRef = useRef<HTMLButtonElement>(null)
  useEffect(() => { if (open) cancelRef.current?.focus() }, [open])
  if (!open) return null
  return <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onCancel()}>
    <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title">
      <h2 id="confirm-title">{t('history.clear.confirm')}</h2>
      <button className="dialog-action destructive" onClick={onConfirm}>{t('history.clear.yes')}</button>
      <button ref={cancelRef} className="dialog-action" onClick={onCancel}>{t('history.clear.cancel')}</button>
    </section>
  </div>
}

