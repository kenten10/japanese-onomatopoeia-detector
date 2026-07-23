import { useEffect, useRef, type KeyboardEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

const focusableSelector = [
  'button:not([disabled])', 'a[href]', 'input:not([disabled])',
  'select:not([disabled])', 'textarea:not([disabled])', '[tabindex]:not([tabindex="-1"])'
].join(',')

export function ModalPortal({
  children, onClose, backdropClassName = 'dialog-backdrop', dismissOnBackdrop = false
}: {
  children: ReactNode
  onClose(): void
  backdropClassName?: string
  dismissOnBackdrop?: boolean
}) {
  const backdropRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const background = [...document.querySelectorAll<HTMLElement>('.tab-content, .tab-bar, .update-banner')]
    const previousInert = background.map((element) => element.inert)
    background.forEach((element) => { element.inert = true })
    const frame = requestAnimationFrame(() => {
      const first = backdropRef.current?.querySelector<HTMLElement>('[data-autofocus], ' + focusableSelector)
      ;(first ?? backdropRef.current)?.focus()
    })
    return () => {
      cancelAnimationFrame(frame)
      background.forEach((element, index) => { element.inert = previousInert[index] })
      previouslyFocused?.focus()
    }
  }, [])

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') { event.preventDefault(); onClose(); return }
    if (event.key !== 'Tab' || !backdropRef.current) return
    const focusable = [...backdropRef.current.querySelectorAll<HTMLElement>(focusableSelector)]
      .filter((element) => !element.hidden && element.getClientRects().length > 0)
    if (focusable.length === 0) { event.preventDefault(); backdropRef.current.focus(); return }
    const first = focusable[0], last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
  }

  return createPortal(
    <div
      ref={backdropRef}
      className={backdropClassName}
      tabIndex={-1}
      onKeyDown={handleKeyDown}
      onMouseDown={(event) => dismissOnBackdrop && event.target === event.currentTarget && onClose()}
    >{children}</div>,
    document.body
  )
}
