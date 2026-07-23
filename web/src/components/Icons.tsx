import type { SVGProps } from 'react'

type Props = SVGProps<SVGSVGElement>
const base = { viewBox: '0 0 24 24', fill: 'currentColor', 'aria-hidden': true } as const

export const MicIcon = (props: Props) => <svg {...base} {...props}><path d="M12 15a4 4 0 0 0 4-4V5a4 4 0 1 0-8 0v6a4 4 0 0 0 4 4Zm7-4a1 1 0 1 0-2 0 5 5 0 0 1-10 0 1 1 0 1 0-2 0 7 7 0 0 0 6 6.92V21H8a1 1 0 1 0 0 2h8a1 1 0 1 0 0-2h-3v-3.08A7 7 0 0 0 19 11Z" /></svg>
export const StopIcon = (props: Props) => <svg {...base} {...props}><rect x="6" y="6" width="12" height="12" rx="1" /></svg>
export const WaveIcon = (props: Props) => <svg {...base} {...props}><path d="M2 10h2v4H2v-4Zm4-5h2v14H6V5Zm4 3h2v8h-2V8Zm4-6h2v20h-2V2Zm4 6h2v8h-2V8Zm4 2h2v4h-2v-4Z" /></svg>
export const BookIcon = (props: Props) => <svg {...base} {...props}><path d="M4 3a3 3 0 0 1 3-3h13v19H7a1 1 0 0 0 0 2h14v3H7a4 4 0 0 1-4-4V4a1 1 0 0 1 1-1Zm3-1a1 1 0 0 0-1 1v14.17c.31-.11.65-.17 1-.17h11V2H7Z" /></svg>
export const GearIcon = (props: Props) => <svg {...base} {...props}><path d="M21.3 13.7a7.6 7.6 0 0 0 0-3.4l2-1.5-2-3.5-2.5 1a8 8 0 0 0-3-1.7L15.5 2h-4l-.4 2.6a8 8 0 0 0-3 1.7l-2.4-1-2 3.5 2.1 1.5a7.6 7.6 0 0 0 0 3.4l-2.1 1.5 2 3.5 2.5-1a8 8 0 0 0 3 1.7l.3 2.6h4l.4-2.6a8 8 0 0 0 3-1.7l2.4 1 2-3.5-2-1.5ZM13.5 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8Z" /></svg>
export const StarIcon = ({ filled = false, ...props }: Props & { filled?: boolean }) => <svg {...base} {...props} fill={filled ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8"><path d="m12 2.2 3 6.1 6.7 1-4.9 4.7 1.2 6.7-6-3.2-6 3.2 1.2-6.7-4.9-4.7 6.7-1 3-6.1Z" /></svg>
export const CloseIcon = (props: Props) => <svg {...base} {...props} fill="none" stroke="currentColor" strokeWidth="3"><path d="m6 6 12 12M18 6 6 18" /></svg>
export const BookmarkIcon = (props: Props) => <svg {...base} {...props}><path d="M5 2h14v21l-7-4-7 4V2Z" /></svg>
export const TrashIcon = (props: Props) => <svg {...base} {...props}><path d="M8 2h8l1 2h5v2H2V4h5l1-2Zm-3 6h14l-1 14H6L5 8Zm4 2v9h2v-9H9Zm4 0v9h2v-9h-2Z" /></svg>
export const ShieldIcon = (props: Props) => <svg {...base} {...props}><path d="M12 1 3 5v6c0 5.8 3.8 10.7 9 12 5.2-1.3 9-6.2 9-12V5l-9-4Zm0 3.2 6 2.7V11c0 4.1-2.4 7.8-6 9-3.6-1.2-6-4.9-6-9V6.9l6-2.7Z" /></svg>

