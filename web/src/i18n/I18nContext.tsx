import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { messages, type MessageKey } from './messages'
import type { AppLanguage } from '../types'

const STORAGE_KEY = 'appLanguage'

export function storedLanguage(): AppLanguage {
  const value = localStorage.getItem(STORAGE_KEY)
  return value === 'system' || value === 'ja' || value === 'en' ? value : 'en'
}

export function resolvedLanguage(language: AppLanguage): 'ja' | 'en' {
  if (language !== 'system') return language
  return navigator.language.toLowerCase().startsWith('ja') ? 'ja' : 'en'
}

interface I18nValue {
  language: AppLanguage
  locale: 'ja' | 'en'
  setLanguage(language: AppLanguage): void
  t(key: MessageKey): string
}

const I18nContext = createContext<I18nValue | null>(null)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, updateLanguage] = useState<AppLanguage>(storedLanguage)
  const locale = resolvedLanguage(language)
  const value = useMemo<I18nValue>(() => ({
    language,
    locale,
    setLanguage(next) {
      localStorage.setItem(STORAGE_KEY, next)
      updateLanguage(next)
    },
    t: (key) => messages[locale][key]
  }), [language, locale])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n(): I18nValue {
  const value = useContext(I18nContext)
  if (!value) throw new Error('useI18n must be used inside I18nProvider')
  return value
}

