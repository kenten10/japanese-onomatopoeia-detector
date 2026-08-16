import { useEffect, useState } from 'react'
import { useApp } from './AppContext'
import { useI18n } from './i18n/I18nContext'
import { HomeView } from './views/HomeView'
import { HistoryView } from './views/HistoryView'
import { SettingsView } from './views/SettingsView'
import { ResultView } from './views/ResultView'
import { BookIcon, GearIcon, WaveIcon } from './components/Icons'
import { ModalPortal } from './components/ModalPortal'
import { applyPwaUpdate, subscribeToPwaUpdates } from './services/pwaUpdate'

type Tab = 'home' | 'history' | 'settings'

export default function App() {
  const { t, locale } = useI18n()
  const { recordingState, resetToIdle, persistenceError, dismissPersistenceError } = useApp()
  const [tab, setTab] = useState<Tab>('home')
  const [updateAvailable, setUpdateAvailable] = useState(false)
  useEffect(() => { document.documentElement.lang = locale }, [locale])
  useEffect(() => subscribeToPwaUpdates(setUpdateAvailable), [])

  return <div className="app-shell">
    <div className="tab-content">
      {tab === 'home' && <HomeView />}
      {tab === 'history' && <HistoryView />}
      {tab === 'settings' && <SettingsView />}
    </div>
    <nav className="tab-bar" aria-label={t('navigation.main')}>
      <button aria-current={tab === 'home' ? 'page' : undefined} className={tab === 'home' ? 'active' : ''} onClick={() => setTab('home')}><WaveIcon /><span>{t('home.tab')}</span></button>
      <button aria-current={tab === 'history' ? 'page' : undefined} className={tab === 'history' ? 'active' : ''} onClick={() => setTab('history')}><BookIcon /><span>{t('history.title')}</span></button>
      <button aria-current={tab === 'settings' ? 'page' : undefined} className={tab === 'settings' ? 'active' : ''} onClick={() => setTab('settings')}><GearIcon /><span>{t('settings.title')}</span></button>
    </nav>
    {updateAvailable && recordingState.kind === 'idle' && <aside className="update-banner" role="status">
      <span>{t('update.available')}</span><button onClick={() => void applyPwaUpdate()}>{t('update.action')}</button>
    </aside>}
    {recordingState.kind === 'result' && <ResultView result={recordingState.result} onDismiss={resetToIdle} />}
    {recordingState.kind === 'error' && <ModalPortal onClose={resetToIdle}><section className="alert-dialog" role="alertdialog" aria-modal="true" aria-labelledby="error-title">
      <h2 id="error-title">{t('error.title')}</h2><p>{recordingState.message}</p>
      {recordingState.code === 'standalone-unsupported' && <a className="alert-action" href={window.location.href} target="_blank" rel="noreferrer">{t('permission.open.safari')}</a>}
      <button data-autofocus onClick={resetToIdle}>{t('error.ok')}</button>
    </section></ModalPortal>}
    {persistenceError && <ModalPortal onClose={dismissPersistenceError}><section className="alert-dialog" role="alertdialog" aria-modal="true" aria-labelledby="persistence-error-title">
      <h2 id="persistence-error-title">{t('error.title')}</h2><p>{persistenceError}</p><button data-autofocus onClick={dismissPersistenceError}>{t('error.ok')}</button>
    </section></ModalPortal>}
  </div>
}
