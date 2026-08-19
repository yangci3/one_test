/**
 * Pure HTML section helpers for collab documents.
 * Sections are <div data-sec-id="sec-N">...</div> blocks.
 */

const SEC_ID_ATTR = /data-sec-id\s*=\s*["']([^"']+)["']/gi
const SEC_DIV_OPEN = /<div\b[^>]*\bdata-sec-id\s*=\s*["']([^"']+)["'][^>]*>/i
const DIV_OPEN = /<div\b/gi
const DIV_CLOSE = /<\/div\s*>/gi

export function parseScopeJson(scopeJson) {
  if (scopeJson == null || scopeJson === '') {
    return []
  }
  let root = scopeJson
  if (typeof scopeJson === 'string') {
    try {
      root = JSON.parse(scopeJson)
    } catch (e) {
      return []
    }
  }
  const sectionIds = root && root.sectionIds
  if (!Array.isArray(sectionIds)) {
    return []
  }
  return sectionIds
    .filter(id => id != null && String(id).trim() !== '')
    .map(id => String(id))
}

export function listSectionIds(html) {
  const ids = []
  if (html == null || html === '') {
    return ids
  }
  const seen = new Set()
  SEC_ID_ATTR.lastIndex = 0
  let match
  while ((match = SEC_ID_ATTR.exec(html)) !== null) {
    const id = match[1]
    if (!seen.has(id)) {
      seen.add(id)
      ids.push(id)
    }
  }
  return ids
}

export function extractEditableHtml(fullHtml, sectionIds) {
  if (fullHtml == null || fullHtml === '') {
    return fullHtml == null ? '' : fullHtml
  }
  if (!sectionIds || sectionIds.length === 0) {
    return ''
  }
  const wanted = new Set(sectionIds)
  const parts = []
  let index = 0
  while (index < fullHtml.length) {
    const block = findNextSectionBlock(fullHtml, index)
    if (!block) {
      break
    }
    if (wanted.has(block.sectionId)) {
      parts.push(block.fullHtml)
    }
    index = block.end
  }
  return parts.join('')
}

function findNextSectionBlock(html, from) {
  const slice = html.slice(from)
  const openMatch = slice.match(SEC_DIV_OPEN)
  if (!openMatch) {
    return null
  }
  const start = from + openMatch.index
  const openEnd = start + openMatch[0].length
  const sectionId = openMatch[1]
  const closeEnd = findClosingDivEnd(html, openEnd)
  const closeStart = findClosingDivStart(html, openEnd, closeEnd)
  const openTag = html.slice(start, openEnd)
  const closeTag = html.slice(closeStart, closeEnd)
  const fullHtml = html.slice(start, closeEnd)
  return { start, end: closeEnd, sectionId, openTag, closeTag, fullHtml }
}

function findClosingDivEnd(html, searchFrom) {
  let depth = 1
  let pos = searchFrom
  while (pos < html.length && depth > 0) {
    DIV_OPEN.lastIndex = pos
    DIV_CLOSE.lastIndex = pos
    const openMatch = DIV_OPEN.exec(html)
    const closeMatch = DIV_CLOSE.exec(html)
    if (!closeMatch) {
      return html.length
    }
    if (openMatch && openMatch.index < closeMatch.index) {
      depth++
      pos = openMatch.index + openMatch[0].length
    } else {
      depth--
      if (depth === 0) {
        return closeMatch.index + closeMatch[0].length
      }
      pos = closeMatch.index + closeMatch[0].length
    }
  }
  return html.length
}

function findClosingDivStart(html, innerStart, closeEnd) {
  DIV_CLOSE.lastIndex = innerStart
  let closeStart = innerStart
  let match
  while ((match = DIV_CLOSE.exec(html)) !== null) {
    if (match.index >= closeEnd) {
      break
    }
    closeStart = match.index
  }
  return closeStart
}
