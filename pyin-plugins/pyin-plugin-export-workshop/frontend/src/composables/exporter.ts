import { materializeWorkbook } from './workbook'

export function exportWorkbook(snapshot: any, changedCells: any[]): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('../workers/export.worker.ts', import.meta.url), { type: 'module' })
    worker.onmessage = event => {
      worker.terminate()
      if (!event.data.ok) reject(new Error(event.data.error))
      else resolve(new Blob([event.data.buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    }
    worker.onerror = () => { worker.terminate(); reject(new Error('导出 Worker 启动失败')) }
    worker.postMessage({ snapshot: materializeWorkbook(snapshot, changedCells) })
  })
}
