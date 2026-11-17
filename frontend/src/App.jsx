import { useCallback, useEffect, useRef, useState } from 'react'
import {
  ArrowDownToLine, ArrowLeft, ArrowRight, Check, ChevronDown, FileSpreadsheet,
  LayoutList, Mail, MapPin, Menu, MoreHorizontal, Pencil, Phone,
  Plus, RefreshCw, Search, Star, Trash2, Upload, UserRound, Users, X,
} from 'lucide-react'
import {
  createContact, deleteContact, downloadExcel, getContact, importContacts,
  listContacts, setFavorite, updateContact,
} from './api.js'

const TYPES = [
  ['PHONE', '电话'], ['EMAIL', '邮箱'], ['WECHAT', '微信'], ['QQ', 'QQ'],
  ['SOCIAL', '社交账号'], ['ADDRESS', '地址'], ['OTHER', '其他'],
]
const TYPE_LABELS = Object.fromEntries(TYPES)
const INITIAL = { name: '', company: '', jobTitle: '', note: '', isFavorite: false, methods: [{ methodType: 'PHONE', methodValue: '', customLabel: '' }] }

function initials(name = '') {
  return [...name.trim()].slice(0, 2).join('') || '?'
}

function ContactForm({ contact, onClose, onSaved }) {
  const [draft, setDraft] = useState(() => contact ? {
    name: contact.name || '', company: contact.company || '', jobTitle: contact.jobTitle || '',
    note: contact.note || '', isFavorite: contact.isFavorite,
    methods: contact.methods?.map(({ methodType, methodValue, customLabel }) => ({ methodType, methodValue, customLabel: customLabel || '' })) || [],
  } : INITIAL)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const inputRef = useRef(null)

  useEffect(() => inputRef.current?.focus(), [])
  useEffect(() => {
    function onKey(event) { if (event.key === 'Escape' && !saving) onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, saving])

  function field(key, value) { setDraft(current => ({ ...current, [key]: value })) }
  function method(index, key, value) {
    setDraft(current => ({ ...current, methods: current.methods.map((entry, position) => position === index ? { ...entry, [key]: value } : entry) }))
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    if (!draft.name.trim()) { setError('请填写联系人姓名。'); return }
    if (draft.methods.some(entry => !entry.methodValue.trim())) { setError('请填写或删除空白的联系方式。'); return }
    const body = {
      ...draft, name: draft.name.trim(), company: draft.company.trim(), jobTitle: draft.jobTitle.trim(), note: draft.note.trim(),
      methods: draft.methods.map((entry, index) => ({
        methodType: entry.methodType, methodValue: entry.methodValue.trim(),
        customLabel: entry.customLabel.trim() || null, sortOrder: index,
      })),
    }
    setSaving(true)
    try {
      const saved = contact ? await updateContact(contact.id, body) : await createContact(body)
      onSaved(saved)
    } catch (failure) { setError(failure.message) }
    finally { setSaving(false) }
  }

  return (
    <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !saving) onClose() }}>
      <section className="editor" role="dialog" aria-modal="true" aria-labelledby="editor-title">
        <header className="editor-head">
          <div><span className="eyebrow">CONTACT DETAILS</span><h2 id="editor-title">{contact ? '编辑联系人' : '新建联系人'}</h2></div>
          <button className="icon-button" title="关闭" aria-label="关闭" onClick={onClose} disabled={saving}><X size={20} /></button>
        </header>
        <form onSubmit={submit} className="editor-form">
          <div className="field-grid">
            <label className="field full name-field">姓名 <span className="required">*</span><input ref={inputRef} value={draft.name} maxLength={100} onChange={event => field('name', event.target.value)} placeholder="输入姓名" required /></label>
            <label className="field">公司<input value={draft.company} maxLength={150} onChange={event => field('company', event.target.value)} placeholder="公司或组织" /></label>
            <label className="field">职位<input value={draft.jobTitle} maxLength={100} onChange={event => field('jobTitle', event.target.value)} placeholder="职位名称" /></label>
          </div>
          <div className="section-heading"><h3>联系方式</h3><span>最多 50 条</span></div>
          <div className="method-list">
            {draft.methods.map((entry, index) => (
              <div className="method-entry" key={index}>
                <div className="select-wrap"><select aria-label={`第 ${index + 1} 条联系方式类型`} value={entry.methodType} onChange={event => method(index, 'methodType', event.target.value)}>{TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><ChevronDown size={15} /></div>
                {(entry.methodType === 'SOCIAL' || entry.methodType === 'OTHER') && <input className="method-label" aria-label="平台或自定义名称" placeholder="平台名称" maxLength={32} value={entry.customLabel} onChange={event => method(index, 'customLabel', event.target.value)} />}
                <input aria-label={`第 ${index + 1} 条联系方式`} type={entry.methodType === 'EMAIL' ? 'email' : 'text'} value={entry.methodValue} maxLength={500} onChange={event => method(index, 'methodValue', event.target.value)} placeholder="填写联系方式" required />
                <button type="button" className="icon-button subtle" aria-label="删除此联系方式" title="删除此联系方式" onClick={() => field('methods', draft.methods.filter((_, position) => position !== index))}><X size={17} /></button>
              </div>
            ))}
          </div>
          <button type="button" className="text-button" disabled={draft.methods.length >= 50} onClick={() => field('methods', [...draft.methods, { methodType: 'PHONE', methodValue: '', customLabel: '' }])}><Plus size={16} /> 添加联系方式</button>
          <label className="field note-field">备注<textarea value={draft.note} maxLength={500} rows={3} onChange={event => field('note', event.target.value)} placeholder="关于这位联系人的备注" /></label>
          <label className="checkbox-line"><input type="checkbox" checked={draft.isFavorite} onChange={event => field('isFavorite', event.target.checked)} /><Star size={17} /> 设为收藏联系人</label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <footer className="editor-actions"><button type="button" className="button secondary" onClick={onClose} disabled={saving}>取消</button><button className="button primary" type="submit" disabled={saving}>{saving ? '保存中…' : '保存联系人'}</button></footer>
        </form>
      </section>
    </div>
  )
}

function MethodIcon({ type }) {
  if (type === 'PHONE') return <Phone size={18} />
  if (type === 'EMAIL') return <Mail size={18} />
  if (type === 'ADDRESS') return <MapPin size={18} />
  return <MoreHorizontal size={18} />
}

export default function App() {
  const [view, setView] = useState('all')
  const [search, setSearch] = useState('')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [list, setList] = useState({ items: [], total: 0, totalPages: 0 })
  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [editing, setEditing] = useState(undefined)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [importResult, setImportResult] = useState(null)
  const [mobileMenu, setMobileMenu] = useState(false)
  const fileInput = useRef(null)

  useEffect(() => {
    const timer = setTimeout(() => { setPage(1); setKeyword(search) }, 300)
    return () => clearTimeout(timer)
  }, [search])

  const refresh = useCallback(async () => {
    const result = await listContacts({ keyword, favorite: view === 'favorites', page })
    setList(result)
    if (page > 1 && result.items.length === 0) setPage(page - 1)
    return result
  }, [keyword, view, page])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    listContacts({ keyword, favorite: view === 'favorites', page })
      .then(result => { if (active) { setList(result); if (page > 1 && !result.items.length) setPage(page - 1) } })
      .catch(failure => { if (active) setError(failure.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [keyword, view, page])

  useEffect(() => {
    if (selectedId === null) { setDetail(null); return }
    let active = true
    setDetail(null)
    setDetailLoading(true)
    getContact(selectedId)
      .then(contact => { if (active) setDetail(contact) })
      .catch(failure => { if (active) { setError(failure.message); setSelectedId(null) } })
      .finally(() => { if (active) setDetailLoading(false) })
    return () => { active = false }
  }, [selectedId])

  function switchView(next) { setView(next); setPage(1); setSelectedId(null); setMobileMenu(false) }
  async function run(action, successMessage) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); if (successMessage) setNotice(successMessage) }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }

  function favorite(contact) {
    if (busy) return
    run(async () => {
      const updated = await setFavorite(contact.id, !contact.isFavorite)
      setDetail(current => current?.id === updated.id ? updated : current)
      await refresh()
    }, contact.isFavorite ? '已取消收藏' : '已添加到收藏')
  }

  function saved(contact) {
    setEditing(undefined)
    setSelectedId(contact.id)
    setDetail(contact)
    setNotice('联系人已保存')
    refresh().catch(failure => setError(failure.message))
  }

  function remove(contact) {
    if (!window.confirm(`确定删除「${contact.name}」吗？此操作无法撤销。`)) return
    run(async () => {
      await deleteContact(contact.id)
      setSelectedId(null); setDetail(null)
      await refresh()
    }, '联系人已删除')
  }

  function chooseFile(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.name.toLowerCase().endsWith('.xlsx') || file.size > 5 * 1024 * 1024) {
      setError('请选择不超过 5 MB 的 .xlsx 文件。'); return
    }
    run(async () => {
      const result = await importContacts(file)
      setImportResult(result)
      await refresh()
    })
  }

  const active = detail && detail.id === selectedId ? detail : null
  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileMenu ? 'open' : ''}`}>
        <div className="brand"><span className="brand-mark"><Users size={21} strokeWidth={2.4} /></span><div><strong>contact book</strong><small>通讯录工作台</small></div></div>
        <div className="sidebar-section-label">工作空间</div>
        <nav aria-label="联系人分类">
          <button className={`nav-item ${view === 'all' ? 'active' : ''}`} onClick={() => switchView('all')}><LayoutList size={19} /> 全部联系人</button>
          <button className={`nav-item ${view === 'favorites' ? 'active' : ''}`} onClick={() => switchView('favorites')}><Star size={19} /> 我的收藏</button>
        </nav>
        <div className="sidebar-bottom"><div className="side-icon"><FileSpreadsheet size={19} /></div><div><strong>Excel 数据管理</strong><span>批量导入与导出联系人</span></div></div>
      </aside>

      <main className="workspace">
        <header className="topbar"><button className="icon-button mobile-toggle" aria-label="打开菜单" onClick={() => setMobileMenu(!mobileMenu)}><Menu size={21} /></button><div className="breadcrumb">工作空间 <span>/</span> <strong>{view === 'all' ? '全部联系人' : '我的收藏'}</strong></div><div className="topbar-right"><span className="status-dot" /> Contact Book</div></header>
        <div className="content">
          <div className="page-heading"><div><span className="eyebrow">ADDRESS BOOK</span><h1>{view === 'all' ? '全部联系人' : '我的收藏'}</h1><p>轻松管理每一位重要联系人及其联系方式。</p></div><button className="button primary add-main" onClick={() => setEditing(null)}><Plus size={18} /> 新建联系人</button></div>

          <div className="toolbar"><div className="search-box"><Search size={19} /><input aria-label="搜索联系人" placeholder="搜索姓名、公司或联系方式..." value={search} onChange={event => setSearch(event.target.value)} />{search && <button className="icon-button subtle" title="清除搜索" aria-label="清除搜索" onClick={() => setSearch('')}><X size={16} /></button>}</div><div className="toolbar-actions"><button className="button secondary" onClick={() => fileInput.current?.click()} disabled={busy}><Upload size={17} /> 导入</button><input ref={fileInput} type="file" accept=".xlsx" hidden onChange={chooseFile} /><button className="button secondary" onClick={() => run(() => downloadExcel('/api/contacts/import-template', '联系人导入模板.xlsx'))} disabled={busy} title="下载 Excel 导入模板"><FileSpreadsheet size={17} /> 模板</button><button className="button secondary" onClick={() => run(() => downloadExcel(`/api/contacts/export${view === 'favorites' || keyword ? `?${new URLSearchParams({ ...(keyword ? { keyword } : {}), ...(view === 'favorites' ? { isFavorite: 'true' } : {}) })}` : ''}`, '联系人导出.xlsx'), '已开始导出')} disabled={busy}><ArrowDownToLine size={17} /> 导出</button></div></div>

          {error && <div className="alert error" role="alert"><span>{error}</span><button aria-label="关闭错误提示" title="关闭" onClick={() => setError('')}><X size={17} /></button></div>}
          {notice && <div className="alert success" role="status"><Check size={17} /><span>{notice}</span><button aria-label="关闭提示" title="关闭" onClick={() => setNotice('')}><X size={17} /></button></div>}

          <div className="list-header"><div><strong>联系人列表</strong><span>{loading ? '加载中' : `共 ${list.total} 位`}</span></div><button className="icon-button subtle" title="刷新列表" aria-label="刷新列表" onClick={() => run(refresh)} disabled={busy}><RefreshCw size={17} /></button></div>
          <div className="contact-layout">
            <section className="list-panel" aria-label="联系人列表">
              {loading ? <div className="empty-state"><RefreshCw size={26} className="spin" /><h3>正在加载联系人</h3></div> : list.items.length === 0 ? <div className="empty-state"><div className="empty-icon"><UserRound size={26} /></div><h3>{error ? '无法加载联系人' : keyword ? '没有找到匹配的联系人' : view === 'favorites' ? '还没有收藏联系人' : '还没有联系人'}</h3><p>{error ? '启动后端后点击刷新重试。' : keyword ? '尝试其他关键词。' : view === 'favorites' ? '在联系人列表中点击星标即可收藏。' : '点击右上角新建第一个联系人。'}</p>{error && <button className="button secondary" onClick={() => run(refresh)}>重试</button>}</div> : list.items.map(contact => (
                <div className={`contact-row ${selectedId === contact.id ? 'selected' : ''}`} key={contact.id}>
                  <button className="row-main" onClick={() => setSelectedId(contact.id)} aria-label={`查看 ${contact.name}`}><span className="avatar">{initials(contact.name)}</span><span className="row-copy"><strong>{contact.name}</strong><small>{contact.company || contact.methods?.[0]?.methodValue || '暂无附加信息'}</small></span></button>
                  <button className={`icon-button favorite ${contact.isFavorite ? 'is-favorite' : ''}`} title={contact.isFavorite ? '取消收藏' : '收藏'} aria-label={`${contact.isFavorite ? '取消收藏' : '收藏'} ${contact.name}`} onClick={() => favorite(contact)} disabled={busy}><Star size={19} fill={contact.isFavorite ? 'currentColor' : 'none'} /></button>
                </div>
              ))}
              {!loading && list.totalPages > 1 && <div className="pagination"><span>第 {page} / {list.totalPages} 页</span><div><button className="icon-button" title="上一页" aria-label="上一页" disabled={page <= 1} onClick={() => setPage(page - 1)}><ArrowLeft size={17} /></button><button className="icon-button" title="下一页" aria-label="下一页" disabled={page >= list.totalPages} onClick={() => setPage(page + 1)}><ArrowRight size={17} /></button></div></div>}
            </section>

            <section className={`detail-panel ${selectedId !== null ? 'has-selection' : ''}`} aria-label="联系人详情">
              {selectedId === null ? <div className="detail-placeholder"><div className="placeholder-icon"><UserRound size={31} /></div><h2>选择一位联系人</h2><p>查看详细信息与所有联系方式</p></div> : detailLoading || !active ? <div className="detail-placeholder"><RefreshCw size={25} className="spin" /><p>正在加载详情</p></div> : <>
                <div className="detail-top"><button className="icon-button back-button" title="返回列表" aria-label="返回列表" onClick={() => setSelectedId(null)}><ArrowLeft size={19} /></button><span>联系人详情</span><div className="detail-top-actions"><button className="icon-button" title="编辑联系人" aria-label="编辑联系人" onClick={() => setEditing(active)}><Pencil size={18} /></button><button className="icon-button danger" title="删除联系人" aria-label="删除联系人" onClick={() => remove(active)} disabled={busy}><Trash2 size={18} /></button></div></div>
                <div className="profile"><div className="profile-avatar">{initials(active.name)}</div><h2>{active.name}</h2><p>{[active.jobTitle, active.company].filter(Boolean).join(' · ') || '联系人'}</p><button className={`favorite-label ${active.isFavorite ? 'active' : ''}`} onClick={() => favorite(active)} disabled={busy}><Star size={16} fill={active.isFavorite ? 'currentColor' : 'none'} /> {active.isFavorite ? '已收藏' : '添加收藏'}</button></div>
                <div className="detail-section"><div className="section-heading"><h3>联系方式</h3><span>{active.methods?.length || 0} 项</span></div>{active.methods?.length ? active.methods.map(entry => <div className="detail-method" key={entry.id}><span className="method-icon"><MethodIcon type={entry.methodType} /></span><div><small>{entry.customLabel || TYPE_LABELS[entry.methodType] || entry.methodType}</small><strong>{entry.methodValue}</strong></div></div>) : <p className="muted">暂无联系方式</p>}</div>
                {active.note && <div className="detail-section"><div className="section-heading"><h3>备注</h3></div><p className="note-text">{active.note}</p></div>}
                <div className="detail-footer">创建于 {active.createdAt ? active.createdAt.replace('T', ' ').slice(0, 16) : '未知'}</div>
              </>}
            </section>
          </div>
        </div>
      </main>
      {editing !== undefined && <ContactForm contact={editing} onClose={() => setEditing(undefined)} onSaved={saved} />}
      {importResult && <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) setImportResult(null) }}><section className="result-dialog" role="dialog" aria-modal="true" aria-labelledby="import-title"><div className="result-heading"><div className="result-symbol"><FileSpreadsheet size={24} /></div><button className="icon-button" title="关闭" aria-label="关闭" onClick={() => setImportResult(null)}><X size={20} /></button></div><h2 id="import-title">导入完成</h2><p>共处理 {importResult.total} 行，成功 {importResult.success} 行，失败 {importResult.failed} 行。</p>{importResult.errors?.length > 0 && <div className="import-errors"><strong>失败明细</strong>{importResult.errors.map((item, index) => <p key={index}>第 {item.row} 行：{item.message}</p>)}</div>}<button className="button primary" onClick={() => setImportResult(null)}>完成</button></section></div>}
    </div>
  )
}
