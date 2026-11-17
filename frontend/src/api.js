const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, options)
  } catch {
    throw new ApiError('无法连接服务器，请确认后端已启动。', 0)
  }
  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.code !== 0) {
    throw new ApiError(payload?.message || (response.status === 500 ? '服务器暂不可用，请确认后端已启动。' : `请求失败 (${response.status})`), response.status)
  }
  return payload.data
}

function jsonOptions(method, body) {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }
}

export function listContacts({ keyword = '', favorite = false, page = 1, pageSize = 20 } = {}) {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })
  if (keyword.trim()) params.set('keyword', keyword.trim())
  if (favorite) params.set('isFavorite', 'true')
  return request(`/api/contacts?${params}`)
}

export const getContact = (id) => request(`/api/contacts/${id}`)
export const createContact = (body) => request('/api/contacts', jsonOptions('POST', body))
export const updateContact = (id, body) => request(`/api/contacts/${id}`, jsonOptions('PUT', body))
export const deleteContact = (id) => request(`/api/contacts/${id}`, { method: 'DELETE' })
export const setFavorite = (id, isFavorite) => request(`/api/contacts/${id}/favorite`, jsonOptions('PATCH', { isFavorite }))

export async function importContacts(file) {
  const form = new FormData()
  form.append('file', file)
  return request('/api/contacts/import', { method: 'POST', body: form })
}

export async function downloadExcel(path, fallbackName) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`)
  } catch {
    throw new ApiError('无法连接服务器，请确认后端已启动。', 0)
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new ApiError(payload?.message || '下载失败', response.status)
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fallbackName
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
