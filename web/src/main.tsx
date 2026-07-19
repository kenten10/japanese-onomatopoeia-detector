import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import App from './App'
import { AppProvider } from './AppContext'
import { I18nProvider } from './i18n/I18nContext'
import './styles.css'

registerSW({ immediate: true })

createRoot(document.getElementById('root')!).render(
  <StrictMode><I18nProvider><AppProvider><App /></AppProvider></I18nProvider></StrictMode>
)

