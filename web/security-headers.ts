/**
 * 配信時に付けるセキュリティヘッダー。
 *
 * ここを唯一の定義とし、`npm run preview` の応答と、ホスティングが読む
 * `dist/_headers` の両方をこの値から作る。二重に書くと、片方だけ直したときに
 * 本番の配信内容が静かにずれてしまう。
 */
export const securityHeaders: Record<string, string> = {
  'Content-Security-Policy': [
    "default-src 'self'",
    "base-uri 'self'",
    "connect-src 'self'",
    "font-src 'self'",
    "form-action 'none'",
    "frame-ancestors 'none'",
    "img-src 'self' data:",
    "manifest-src 'self'",
    "media-src 'self'",
    "object-src 'none'",
    "script-src 'self'",
    "style-src 'self'",
    "worker-src 'self'"
  ].join('; '),
  'Cross-Origin-Opener-Policy': 'same-origin',
  'Cross-Origin-Resource-Policy': 'same-origin',
  'Permissions-Policy': 'camera=(), geolocation=(), microphone=(self), payment=(), usb=()',
  'Referrer-Policy': 'no-referrer',
  'Strict-Transport-Security': 'max-age=31536000',
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'X-Permitted-Cross-Domain-Policies': 'none'
}

/**
 * Cloudflare Pages / Netlify が読む `_headers` の中身を組み立てる。
 *
 * Service Worker は毎回取り直させる。ホスティング既定のキャッシュに載ると、
 * 新しい版を出しても最大 24 時間ほど利用者に届かないことがある。
 */
export function buildHeadersFile(): string {
  const lines = ['/*']
  for (const [name, value] of Object.entries(securityHeaders)) {
    lines.push(`  ${name}: ${value}`)
  }
  lines.push('', '/sw.js', '  Cache-Control: no-cache')
  return lines.join('\n') + '\n'
}
