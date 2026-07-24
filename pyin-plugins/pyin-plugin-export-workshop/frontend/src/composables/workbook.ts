export function cloneWorkbook<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T }

export function cellValue(snapshot: any, row: number, column: number): string {
  const cell = snapshot?.sheets?.['sheet-1']?.cellData?.[row]?.[column]
  return cell?.f || cell?.v || ''
}

export function setCellValue(snapshot: any, row: number, column: number, value: string) {
  const sheet = snapshot.sheets['sheet-1']
  sheet.cellData ||= {}; sheet.cellData[row] ||= {}
  sheet.cellData[row][column] = value.startsWith('=') ? { f: value } : { v: value }
}

export function columnName(index: number) {
  let result = ''; let value = index + 1
  while (value) { const remainder = (value - 1) % 26; result = String.fromCharCode(65 + remainder) + result; value = Math.floor((value - 1) / 26) }
  return result
}
