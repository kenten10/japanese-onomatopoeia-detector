import { useEffect, useState } from 'react'
import { useApp } from './AppContext'
import { useI18n } from './i18n/I18nContext'
import { HomeView } from './views/HomeView'
import { HistoryView } from './views/HistoryView'
import { SettingsView } from './views/SettingsView'
import { ResultView } from './views/ResultView'
import { BookIcon, GearIcon, WaveIcon } from './components/Icons'

type Tab = 'home' | 'history' | 'settings'

export default function App() {
  const { t, locale } = useI18n()
  const { recordingState, resetToIdle, persistenceError, dismissPersistenceError } = useApp()
  const [tab, setTab] = useState<Tab>('home')
  useEffect(() => { document.documentElement.lang = locale }, [locale])

  return <div className="app-shell">
    <div className="tab-content">
      {tab === 'home' && <HomeView />}
      {tab === 'history' && <HistoryView />}
      {tab === 'settings' && <SettingsView />}
    </div>
    <nav className="tab-bar" aria-label="Main navigation">
      <button className={tab === 'home' ? 'active' : ''} onClick={() => setTab('home')}><WaveIcon /><span>{t('home.tab')}</span></button>
      <button className={tab === 'history' ? 'active' : ''} onClick={() => setTab('history')}><BookIcon /><span>{t('history.title')}</span></button>
      <button className={tab === 'settings' ? 'active' : ''} onClick={() => setTab('settings')}><GearIcon /><span>{t('settings.title')}</span></button>
    </nav>
    {recordingState.kind === 'result' && <ResultView result={recordingState.result} onDismiss={resetToIdle} />}
    {recordingState.kind === 'error' && <div className="dialog-backdrop"><section className="alert-dialog" role="alertdialog" aria-modal="true">
      <h2>{t('error.title')}</h2><p>{recordingState.message}</p>
      <button onClick={resetToIdle}>{t('error.ok')}</button>
    </section></div>}
    {persistenceError && <div className="dialog-backdrop"><section className="alert-dialog" role="alertdialog" aria-modal="true">
      <h2>{t('error.title')}</h2><p>{persistenceError}</p><button onClick={dismissPersistenceError}>{t('error.ok')}</button>
    </section></div>}
  </div>
}

