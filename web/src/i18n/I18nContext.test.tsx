import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { I18nProvider, resolvedLanguage, useI18n } from './I18nContext'

function Title() { return <span>{useI18n().t('app.title')}</span> }

describe('localization', () => {
  it('defaults to English for a new user', () => {
    localStorage.clear()
    render(<I18nProvider><Title /></I18nProvider>)
    expect(screen.getByText('Onomatopoeia Detector')).toBeInTheDocument()
  })

  it('resolves system Japanese', () => {
    Object.defineProperty(navigator, 'language', { value: 'ja-JP', configurable: true })
    expect(resolvedLanguage('system')).toBe('ja')
  })
})

