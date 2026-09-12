/**
 * 通用格式化工具
 */

/** 字节数转可读体积 */
export function formatFileSize(bytes) {
  if (bytes === null || bytes === undefined) return '-'
  const size = Number(bytes)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

/** 后端已按 yyyy-MM-dd HH:mm:ss 返回，取日期部分 */
export function toDate(value) {
  return value ? String(value).slice(0, 10) : '-'
}

/** 空值占位 */
export function orDash(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}

/** 从响应头解析下载文件名（后端返回 Content-Disposition） */
export function parseFileName(disposition, fallback) {
  if (!disposition) return fallback
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition)
  if (!match) return fallback
  try {
    return decodeURIComponent(match[1])
  } catch (e) {
    return match[1]
  }
}
