import { requestJson } from './http'

const base = '/plugins/state-machine/admin/state-machines/order'

export const fetchWorkspace = () => requestJson(base)
export const saveDefinition = definition => requestJson(base, { method: 'POST', body: definition })
export const publishDefinition = () => requestJson(`${base}/publish`, { method: 'POST' })
export const resetDebug = () => requestJson(`${base}/debug/reset`, { method: 'POST' })
export const dispatchDebugEvent = eventCode => requestJson(`${base}/debug/events`, { method: 'POST', body: { eventCode } })
