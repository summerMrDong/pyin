import { request } from './http'

const base = '/plugins/export-workshop/admin'

export const workshopApi = {
  tree: () => request<any[]>(`${base}/templates/tree`),
  template: (id: number) => request<any>(`${base}/templates/${id}`),
  templateDownloadUrl: (templateId: string) => `${base}/templates/download/${encodeURIComponent(templateId)}`,
  createBlank: (body: { directoryId?: number; id?: string; name: string }) => request<any>(`${base}/templates/blank`, { method: 'POST', body }),
  createFolder: (body: { parentId?: number; name: string }) => request<any>(`${base}/folders`, { method: 'POST', body }),
  rename: (nodeId: string, name: string) => request<any>(`${base}/nodes/${nodeId}/name`, { method: 'PUT', body: { name } }),
  remove: (nodeId: string) => request<void>(`${base}/nodes/${nodeId}`, { method: 'DELETE' }),
  save: (id: number, body: any) => request<any>(`${base}/templates/${id}/workbook`, { method: 'PUT', body }),
  fork: (id: number) => request<any>(`${base}/templates/${id}/fork`, { method: 'POST' }),
  debug: (body: any) => request<any>(`${base}/debug/render`, { method: 'POST', body }),
  roots: () => request<string[]>(`${base}/sources/local-roots`),
  mountNetwork: (body: any) => request<any>(`${base}/templates/mount/network`, { method: 'POST', body }),
  mountDirectory: (body: any) => request<any>(`${base}/templates/mount/local-directory`, { method: 'POST', body }),
  upload: (directoryId: number | null, id: string | undefined, file: File) => {
    const form = new FormData(); form.append('file', file)
    const params = new URLSearchParams()
    if (directoryId) params.set('directoryId', String(directoryId))
    if (id) params.set('id', id)
    const query = params.size ? `?${params.toString()}` : ''
    return request<any>(`${base}/templates/import${query}`, { method: 'POST', body: form })
  },
  createExport: (templateId: number, body: any) => request<any>(`${base}/templates/${templateId}/exports`, { method: 'POST', body }),
  uploadExport: (taskId: string, file: Blob) => {
    const form = new FormData(); form.append('file', file, 'export.xlsx')
    return request<any>(`${base}/exports/${taskId}/file`, { method: 'POST', body: form })
  }
}
