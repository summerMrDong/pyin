<template>
  <section class="univer-wrapper">
    <div v-if="!snapshot" class="welcome">
      <h2>导出工坊</h2>
      <p>创建模板或导入已有 XLSX 文件，随后在单元格中写入 <code v-pre>{{customer.name}}</code>。</p>
      <div><button @click="$emit('create')">新建模板</button><button @click="$emit('import')">导入模板</button></div>
    </div>
    <div v-else ref="container" class="univer-container" :class="{ 'has-changes': changedCells.length }"></div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, toRaw, watch } from 'vue'
import { ICommandService, LocaleType, mergeLocales, Univer, UniverInstanceType } from '@univerjs/core'
import DesignZhCN from '@univerjs/design/locale/zh-CN'
import { UniverDocsPlugin } from '@univerjs/docs'
import { UniverDocsUIPlugin } from '@univerjs/docs-ui'
import DocsUIZhCN from '@univerjs/docs-ui/locale/zh-CN'
import { UniverFormulaEnginePlugin } from '@univerjs/engine-formula'
import { UniverRenderEnginePlugin } from '@univerjs/engine-render'
import { UniverSheetsPlugin } from '@univerjs/sheets'
import { UniverSheetsFormulaUIPlugin } from '@univerjs/sheets-formula-ui'
import SheetsFormulaUIZhCN from '@univerjs/sheets-formula-ui/locale/zh-CN'
import { UniverSheetsNumfmtUIPlugin } from '@univerjs/sheets-numfmt-ui'
import SheetsNumfmtUIZhCN from '@univerjs/sheets-numfmt-ui/locale/zh-CN'
import SheetsZhCN from '@univerjs/sheets/locale/zh-CN'
import { UniverSheetsUIPlugin } from '@univerjs/sheets-ui'
import SheetsUIZhCN from '@univerjs/sheets-ui/locale/zh-CN'
import { UniverUIPlugin } from '@univerjs/ui'
import UIZhCN from '@univerjs/ui/locale/zh-CN'
import '@univerjs/design/lib/index.css'
import '@univerjs/ui/lib/index.css'
import '@univerjs/docs-ui/lib/index.css'
import '@univerjs/sheets-ui/lib/index.css'
import '@univerjs/sheets-formula-ui/lib/index.css'
import '@univerjs/sheets-numfmt-ui/lib/index.css'
import { cloneWorkbook, normalizeWorkbookForUniver } from '../composables/workbook'

const props = defineProps<{ snapshot?: any; changedCells: any[] }>()
const emit = defineEmits(['update:snapshot', 'create', 'import'])
const container = ref<HTMLElement>()
let univer: Univer | undefined
let commandSubscription: { dispose: () => void } | undefined
let themeObserver: MutationObserver | undefined
let loadVersion = 0
let internalSnapshot: any
let renderedDarkMode = false

/** 主壳以 data-theme="dark" 为主题事实来源；兼容独立挂载时的 dark class。 */
function systemDarkMode() {
  const root = document.documentElement
  return root.dataset.theme === 'dark' || root.classList.contains('dark')
}

function dispose() {
  commandSubscription?.dispose()
  commandSubscription = undefined
  themeObserver?.disconnect()
  themeObserver = undefined
  univer?.dispose()
  univer = undefined
  if (container.value) container.value.replaceChildren()
}

async function create(snapshot: any) {
  const requestVersion = ++loadVersion
  dispose()
  await nextTick()
  if (!container.value || requestVersion !== loadVersion) return

  const hostId = `univer-sheet-${requestVersion}`
  const darkMode = systemDarkMode()
  renderedDarkMode = darkMode
  container.value.id = hostId
  univer = new Univer({
    locale: LocaleType.ZH_CN,
    // 只使用 Univer 官方深色模式，不叠加自定义色板。
    darkMode,
    locales: {
      [LocaleType.ZH_CN]: mergeLocales(DesignZhCN, UIZhCN, DocsUIZhCN, SheetsZhCN, SheetsUIZhCN, SheetsFormulaUIZhCN, SheetsNumfmtUIZhCN),
    },
  })
  univer.registerPlugin(UniverRenderEnginePlugin)
  univer.registerPlugin(UniverFormulaEnginePlugin)
  univer.registerPlugin(UniverUIPlugin, { container: hostId, disableAutoFocus: true })
  univer.registerPlugin(UniverDocsPlugin)
  univer.registerPlugin(UniverDocsUIPlugin)
  univer.registerPlugin(UniverSheetsPlugin)
  univer.registerPlugin(UniverSheetsUIPlugin)
  univer.registerPlugin(UniverSheetsFormulaUIPlugin)
  univer.registerPlugin(UniverSheetsNumfmtUIPlugin)

  const workbook = univer.createUnit(UniverInstanceType.UNIVER_SHEET, normalizeWorkbookForUniver(snapshot)) as any
  commandSubscription = univer.__getInjector().get(ICommandService).onCommandExecuted(() => {
    internalSnapshot = cloneWorkbook(workbook.save())
    emit('update:snapshot', internalSnapshot)
  })
  themeObserver = new MutationObserver(() => {
    const nextDarkMode = systemDarkMode()
    if (nextDarkMode !== renderedDarkMode && props.snapshot) void create(props.snapshot)
  })
  // 主壳主题的唯一事实来源是 data-theme。不要监听 class，Univer 自身也可能维护 class。
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
}

watch(() => props.snapshot, snapshot => {
  // 父组件的 ref 会把事件参数转换为响应式代理；必须取回原对象比较，
  // 否则一次 Univer 命令会触发无限的“快照变更 -> 销毁 -> 重建”循环。
  if (snapshot && toRaw(snapshot) === internalSnapshot) {
    internalSnapshot = undefined
    return
  }
  if (snapshot) void create(snapshot)
  else { loadVersion += 1; dispose() }
}, { immediate: true, deep: false })

onBeforeUnmount(() => { loadVersion += 1; dispose() })
</script>

<style>
.univer-wrapper{min-width:0;min-height:0;position:relative;background:var(--surface);color:var(--text)}
.univer-container{width:100%;height:100%;min-height:0;overflow:hidden;background:var(--surface)}
.univer-container.has-changes::after{position:absolute;right:14px;bottom:12px;z-index:5;padding:4px 7px;border:1px solid var(--border);border-radius:4px;background:var(--surface);color:var(--muted);content:'预览结果已生成；高亮详情见打印预览';font:11px var(--font);pointer-events:none}
.univer-wrapper .univer-workbench-container{height:100%;font-family:var(--font)}
.welcome{display:grid;height:100%;place-content:center;gap:10px;text-align:center;color:var(--muted)}
.welcome h2{margin:0;color:var(--text);font:600 22px var(--font)}
.welcome p{margin:0;max-width:380px}.welcome code{font-family:var(--mono);color:var(--text)}
.welcome div{display:flex;justify-content:center;gap:8px}.welcome button{padding:7px 11px;border:1px solid var(--border);border-radius:5px;background:var(--button);color:var(--text);cursor:pointer;font:12px var(--font)}.welcome button:first-child{border-color:var(--accent);background:var(--accent);color:#fff}
.univer-container .univer-scrollbar-thumb{background:var(--border)}
</style>
