import { request } from './http'

const base = '/plugins/export-workshop/admin'

export const workshopApi = {
  tree: () => request<any[]>(`${base}/templates/tree`),
  template: (id: number) => request<any>(`${base}/templates/${id}`),
  createBlank: (body: { directoryId?: number; name: string }) => request<any>(`${base}/templates/blank`, { method: 'POST', body }),
  createFolder: (body: { parentId?: number; name: string }) => request<any>(`${base}/folders`, { method: 'POST', body }),
  rename: (nodeId: string, name: string) => request<any>(`${base}/nodes/${nodeId}/name`, { method: 'PUT', body: { name } }),
  remove: (nodeId: string) => request<void>(`${base}/nodes/${nodeId}`, { method: 'DELETE' }),
  save: (id: number, body: any) => request<any>(`${base}/templates/${id}/workbook`, { method: 'PUT', body }),
  debug: (body: any) => request<any>(`${base}/debug/render`, { method: 'POST', body }),
  roots: () => request<string[]>(`${base}/sources/local-roots`),
  mountNetwork: (body: any) => request<any>(`${base}/templates/mount/network`, { method: 'POST', body }),
  mountDirectory: (body: any) => request<any>(`${base}/templates/mount/local-directory`, { method: 'POST', body }),
  upload: (directoryId: number | null, file: File) => {
    const form = new FormData(); form.append('file', file)
    return request<any>(`${base}/templates/import${directoryId ? `?directoryId=${directoryId}` : ''}`, { method: 'POST', body: form })
  },
  createExport: (templateId: number, body: any) => request<any>(`${base}/templates/${templateId}/exports`, { method: 'POST', body }),
  uploadExport: (taskId: string, file: Blob) => {
    const form = new FormData(); form.append('file', file, 'export.xlsx')
    return request<any>(`${base}/exports/${taskId}/file`, { method: 'POST', body: form })
  }
}
