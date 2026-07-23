import { useApp } from '../AppContext'
import { useI18n } from '../i18n/I18nContext'
import { MicIcon, StopIcon } from '../components/Icons'

function Waveform() {
  return <div className="waveform" aria-hidden="true">
    {Array.from({ length: 13 }, (_, index) => <i key={index} style={{ '--i': index } as React.CSSProperties} />)}
  </div>
}

export function HomeView() {
  const { t } = useI18n()
  const { recordingState, partialText, startRecording, stopRecording } = useApp()
  const recording = recordingState.kind === 'recording'
  const processing = recordingState.kind === 'recognizing' || recordingState.kind === 'evaluating'

  return <main className="home-view screen">
    <div className="halftone home-halftone" aria-hidden="true" />
    <header className="app-header">
      <h1>{t('app.title')}</h1>
      <p>{t('app.subtitle').toUpperCase()}</p>
    </header>

    <section className="home-center" aria-live="polite">
      {recording ? <div className="recording-content">
        <Waveform />
        <div className={`partial-text ${partialText ? 'has-text' : ''}`}>{partialText || t('recording.listening')}</div>
        <div className="recording-hint">{t('recording.tap.stop')}</div>
      </div> : processing ? <div className="processing-content">
        <span className="spinner" aria-hidden="true" />
        <strong>{t('recording.recognizing')}</strong>
      </div> : <div className="idle-content">
        <div className="quiet-sfx">シーン…</div>
        <h2>{t('home.no.result')}</h2>
        <p>{t('home.no.result.sub')}</p>
      </div>}
    </section>

    <button
      className={`mic-button ${recording ? 'is-recording' : ''}`}
      disabled={processing}
      aria-label={recording ? t('recording.tap.stop') : t('home.record.button')}
      onClick={() => recording ? stopRecording() : void startRecording()}
    >
      {recording && <span className="mic-rays" aria-hidden="true" />}
      <span className="mic-shadow" /><span className="mic-face">{recording ? <StopIcon /> : <MicIcon />}</span>
    </button>
  </main>
}

