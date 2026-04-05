import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({
  gfm: true,
  breaks: true,
})

/**
 * Markdown → 安全 HTML（供 v-html）。空输入返回空串。
 */
export function renderMarkdownToSafeHtml(src) {
  if (src == null) return ''
  const s = typeof src === 'string' ? src : String(src)
  if (!s.trim()) return ''
  const rawHtml = marked.parse(s)
  return DOMPurify.sanitize(rawHtml, { USE_PROFILES: { html: true } })
}
