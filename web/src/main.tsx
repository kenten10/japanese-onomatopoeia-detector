import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { AppProvider } from './AppContext'
import { I18nProvider } from './i18n/I18nContext'
import './styles.css'
import { initializePwaUpdates } from './services/pwaUpdate'

initializePwaUpdates()

createRoot(document.getElementById('root')!).render(
  <StrictMode><I18nProvider><AppProvider><App /></AppProvider></I18nProvider></StrictMode>
)
