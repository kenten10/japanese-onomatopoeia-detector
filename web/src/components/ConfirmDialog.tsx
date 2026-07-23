import { useI18n } from '../i18n/I18nContext'
import { ModalPortal } from './ModalPortal'

export function ConfirmDialog({ open, onConfirm, onCancel }: { open: boolean; onConfirm(): void; onCancel(): void }) {
  const { t } = useI18n()
  if (!open) return null
  return <ModalPortal onClose={onCancel} dismissOnBackdrop>
    <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title">
      <h2 id="confirm-title">{t('history.clear.confirm')}</h2>
      <button className="dialog-action destructive" onClick={onConfirm}>{t('history.clear.yes')}</button>
      <button data-autofocus className="dialog-action" onClick={onCancel}>{t('history.clear.cancel')}</button>
    </section>
  </ModalPortal>
}
