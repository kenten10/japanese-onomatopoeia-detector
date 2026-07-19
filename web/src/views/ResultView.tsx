import { useEffect, useState } from 'react'
import { useApp } from '../AppContext'
import { useI18n } from '../i18n/I18nContext'
import type { EvaluationResult, OnomatopoeiaEntry } from '../types'
import { BookmarkIcon, CloseIcon, StarIcon } from '../components/Icons'

function SimilarCard({ entry }: { entry: OnomatopoeiaEntry }) {
  const { t } = useI18n()
  return <article className="manga-panel similar-card">
    <div className="similar-title"><strong>{entry.word}</strong><span>（{entry.reading}）</span><em>{entry.category}</em></div>
    <hr />
    <p>🇯🇵 <span>{entry.meaning_ja}</span></p><p>🇬🇧 <span>{entry.meaning_en}</span></p>
    <div className="examples"><b>▱ {t('result.similar.example')}</b><span>・{entry.example_ja}</span><span>・{entry.example_en}</span></div>
  </article>
}

export function ResultView({ result, onDismiss }: { result: EvaluationResult; onDismiss(): void }) {
  const { t } = useI18n()
  const { saveCurrentResult } = useApp()
  const [shownStars, setShownStars] = useState(0)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    const timers = Array.from({ length: result.score }, (_, index) => window.setTimeout(() => setShownStars(index + 1), (index + 1) * 120))
    return () => timers.forEach(window.clearTimeout)
  }, [result.score])

  async function save() {
    if (await saveCurrentResult()) {
      setSaved(true)
      window.setTimeout(() => setSaved(false), 1500)
    }
  }

  return <div className="result-backdrop">
    <section className="result-sheet" role="dialog" aria-modal="true" aria-labelledby="result-heading">
      <header className="sheet-header"><h2 id="result-heading">{t('result.title')}</h2><button onClick={onDismiss} aria-label={t('result.again')}><CloseIcon /></button></header>
      <div className="result-scroll">
        <div className="manga-panel sfx-burst"><span className="speed-lines" aria-hidden="true" /><span className="halftone" aria-hidden="true" /><b>{t('result.recognized').toUpperCase()}</b><strong>{result.inputText}</strong></div>
        <section className="manga-panel score-card">
          <h3>{t('result.score.label').toUpperCase()}</h3>
          <div className={`stars score-${result.score}`} aria-label={`${result.score} / 5`}>
            {[1, 2, 3, 4, 5].map((star) => <StarIcon key={star} filled={star <= shownStars} className={star <= result.score ? 'earned' : ''} />)}
          </div>
          <div className={`numeric-score score-${result.score}`}><strong>{result.score}</strong><span>/ 5</span></div>
          <p>{t(`result.comment.${result.score}` as 'result.comment.1')}</p>
        </section>
        {result.score >= 3 && result.similarEntries.length > 0 && <section className="similar-section">
          <h3>✦ {t('result.similar.title')}</h3>
          {result.similarEntries.map(({ entry }, index) => <SimilarCard key={`${entry.word}-${index}`} entry={entry} />)}
        </section>}
        <div className="result-actions">
          <button className={`manga-button primary ${saved ? 'saved' : ''}`} onClick={() => void save()}><BookmarkIcon />{saved ? t('result.saved') : t('result.save')}</button>
          <button className="manga-button secondary" onClick={onDismiss}>{t('result.again')}</button>
        </div>
      </div>
    </section>
  </div>
}

