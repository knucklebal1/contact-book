import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, importContacts, listContacts, setFavorite } from './api.js'

afterEach(() => vi.unstubAllGlobals())

describe('contact API', () => {
  it('sends search, favorite and page filters', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ code: 0, data: { items: [] } }) })
    vi.stubGlobal('fetch', fetch)
    await listContacts({ keyword: ' 张 ', favorite: true, page: 2 })
    expect(fetch.mock.calls[0][0]).toContain('keyword=%E5%BC%A0')
    expect(fetch.mock.calls[0][0]).toContain('isFavorite=true')
    expect(fetch.mock.calls[0][0]).toContain('page=2')
  })

  it('sends an explicit favorite state', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ code: 0, data: {} }) })
    vi.stubGlobal('fetch', fetch)
    await setFavorite(3, false)
    expect(fetch.mock.calls[0][1]).toMatchObject({ method: 'PATCH', body: '{"isFavorite":false}' })
  })

  it('uploads the file under the backend field name', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ code: 0, data: { success: 1 } }) })
    vi.stubGlobal('fetch', fetch)
    await importContacts(new Blob(['test']))
    expect(fetch.mock.calls[0][1].body.has('file')).toBe(true)
  })

  it('shows the server validation message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 400, json: async () => ({ code: 400, message: '姓名不能为空' }) }))
    await expect(listContacts()).rejects.toEqual(new ApiError('姓名不能为空', 400))
  })
})
