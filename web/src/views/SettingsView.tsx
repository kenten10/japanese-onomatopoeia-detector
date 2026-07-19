import { useState } from 'react'
import { useApp } from '../AppContext'
import { useI18n } from '../i18n/I18nContext'
import type { AppLanguage } from '../types'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ShieldIcon, TrashIcon } from '../components/Icons'

export function PrivacyPolicyView({ onBack }: { onBack(): void }) {
  const { t } = useI18n()
  return <main className="screen settings-screen privacy-screen">
    <header className="navigation-header centered"><button className="back-button" onClick={onBack}>‹</button><h1>{t('settings.privacy')}</h1></header>
    <div className="form-list privacy-list"><section><p>{t('privacy.summary')}</p></section>
      {(['audio', 'storage', 'control'] as const).map((section) => <section key={section}><h2>{t(`privacy.${section}.title`)}</h2><p>{t(`privacy.${section}.body`)}</p></section>)}
    </div>
  </main>
}

export function SettingsView() {
  const { t, language, setLanguage } = useI18n()
  const { clearHistory } = useApp()
  const [confirmClear, setConfirmClear] = useState(false)
  const [privacy, setPrivacy] = useState(false)
  if (privacy) return <PrivacyPolicyView onBack={() => setPrivacy(false)} />

  return <main className="screen settings-screen">
    <header className="navigation-header"><h1>{t('settings.title')}</h1></header>
    <div className="form-list">
      <h2>{t('settings.language').toUpperCase()}</h2>
      <section><label><span>{t('settings.language')}</span><select value={language} onChange={(event) => setLanguage(event.target.value as AppLanguage)}>
        <option value="system">{t('settings.language.auto')}</option><option value="ja">{t('settings.language.ja')}</option><option value="en">{t('settings.language.en')}</option>
      </select></label></section>
      <h2>{t('history.title').toUpperCase()}</h2>
      <section><button className="form-row destructive" onClick={() => setConfirmClear(true)}><TrashIcon />{t('settings.history.clear')}</button></section>
      <h2>{t('settings.about').toUpperCase()}</h2>
      <section>
        <div className="form-row"><span>{t('settings.version')}</span><code>1.0</code></div>
        <div className="about-row"><strong>{t('settings.about')}</strong><p>{t('settings.about.desc')}</p></div>
        <button className="form-row privacy-link" onClick={() => setPrivacy(true)}><ShieldIcon /><span>{t('settings.privacy')}</span><b>›</b></button>
      </section>
    </div>
    <ConfirmDialog open={confirmClear} onCancel={() => setConfirmClear(false)} onConfirm={() => { setConfirmClear(false); void clearHistory() }} />
  </main>
}

