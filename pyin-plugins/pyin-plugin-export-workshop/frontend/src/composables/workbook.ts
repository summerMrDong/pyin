export function cloneWorkbook<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T }

/**
 * 将旧版/导入的工作簿快照补齐为 Univer Sheets 可接受的最小结构。
 * 历史版本曾把 resources 持久化为对象，而 Univer 要求资源列表为数组。
 */
export function normalizeWorkbookForUniver(snapshot: any) {
  const workbook = cloneWorkbook(snapshot || {}) as any
  if (!workbook.id) workbook.id = `workbook-${crypto.randomUUID?.() || Date.now()}`
  if (!workbook.name) workbook.name = '未命名模板'
  if (!workbook.sheets || typeof workbook.sheets !== 'object' || Array.isArray(workbook.sheets)) workbook.sheets = {}
  if (Object.keys(workbook.sheets).length === 0) {
    workbook.sheets['sheet-1'] = { id: 'sheet-1', name: 'Sheet1', cellData: {} }
  }
  for (const [sheetId, sheet] of Object.entries(workbook.sheets)) {
    const target = sheet as any
    target.id ||= sheetId
    target.name ||= 'Sheet1'
    if (!target.cellData || typeof target.cellData !== 'object' || Array.isArray(target.cellData)) target.cellData = {}
  }
  if (!Array.isArray(workbook.sheetOrder) || workbook.sheetOrder.length === 0) workbook.sheetOrder = Object.keys(workbook.sheets)
  workbook.sheetOrder = workbook.sheetOrder.filter((sheetId: string) => workbook.sheets[sheetId])
  if (workbook.sheetOrder.length === 0) workbook.sheetOrder = Object.keys(workbook.sheets)
  if (!Array.isArray(workbook.resources)) workbook.resources = []
  return workbook
}

export function cellValue(snapshot: any, row: number, column: number): string {
  const cell = snapshot?.sheets?.['sheet-1']?.cellData?.[row]?.[column]
  return cell?.f || cell?.v || ''
}

export function setCellValue(snapshot: any, row: number, column: number, value: string) {
  const sheet = snapshot.sheets['sheet-1']
  sheet.cellData ||= {}; sheet.cellData[row] ||= {}
  sheet.cellData[row][column] = value.startsWith('=') ? { f: value } : { v: value }
}

export function parseCellAddress(address: string) {
  const match = /^([A-Z]+)([1-9]\d*)$/i.exec(address.trim())
  if (!match) return undefined
  let column = 0
  for (const character of match[1].toUpperCase()) column = column * 26 + character.charCodeAt(0) - 64
  return { row: Number(match[2]) - 1, column: column - 1 }
}

export function materializeWorkbook(snapshot: any, changedCells: any[] = []) {
  const rendered = normalizeWorkbookForUniver(snapshot)
  for (const change of changedCells) {
    const position = parseCellAddress(String(change?.cellAddress || ''))
    if (!position) continue
    const sheetId = String(change?.sheetId || rendered?.sheetOrder?.[0] || 'sheet-1')
    const sheet = rendered?.sheets?.[sheetId]
    if (!sheet) continue
    sheet.cellData ||= {}
    sheet.cellData[position.row] ||= {}
    sheet.cellData[position.row][position.column] = { v: change.value ?? '' }
  }
  return rendered
}

export function templateVariables(snapshot: any): string[] {
  const variables = new Set<string>()
  const pattern = /\{\{?\s*([A-Za-z_][A-Za-z0-9_.-]*)\s*\}?\}/g
  for (const sheet of Object.values(snapshot?.sheets || {})) {
    for (const cells of Object.values((sheet as any)?.cellData || {})) {
      for (const cell of Object.values(cells as Record<string, any>)) {
        if (typeof (cell as any)?.v !== 'string') continue
        for (const match of (cell as any).v.matchAll(pattern)) variables.add(match[1])
      }
    }
  }
  return [...variables]
}

export function columnName(index: number) {
  let result = ''; let value = index + 1
  while (value) { const remainder = (value - 1) % 26; result = String.fromCharCode(65 + remainder) + result; value = Math.floor((value - 1) / 26) }
  return result
}
