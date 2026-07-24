<template>
  <div class="json-editor">
    <div class="json-editor-toolbar">
      <span>JSON 编辑器</span>
      <el-button link type="primary" @click="formatJson">格式化 JSON</el-button>
    </div>
    <div ref="containerRef" class="json-editor-container" />
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api'
import 'monaco-editor/esm/vs/language/json/monaco.contribution'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'validation-change'])
const containerRef = ref()
let editor
let model
let observer
let resizeObserver
let applyingValue = false

self.MonacoEnvironment = {
  getWorker(_moduleId, label) {
    return label === 'json' ? new jsonWorker() : new editorWorker()
  }
}

onMounted(async () => {
  await nextTick()
  defineThemes()
  model = monaco.editor.createModel(props.modelValue, 'json')
  editor = monaco.editor.create(containerRef.value, {
    model,
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 13,
    lineNumbersMinChars: 3,
    scrollBeyondLastLine: false,
    tabSize: 2,
    insertSpaces: true
  })
  applyTheme()
  model.onDidChangeContent(() => {
    if (!applyingValue) emit('update:modelValue', model.getValue())
    validate()
  })
  observer = new MutationObserver(applyTheme)
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  resizeObserver = new ResizeObserver(() => editor?.layout())
  resizeObserver.observe(containerRef.value)
  validate()
})

watch(() => props.modelValue, (value) => {
  if (!model || value === model.getValue()) return
  applyingValue = true
  model.setValue(value)
  applyingValue = false
  validate()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  resizeObserver?.disconnect()
  editor?.dispose()
  model?.dispose()
})

function defineThemes() {
  const styles = getComputedStyle(document.documentElement)
  const background = styles.getPropertyValue('--shell-tool-code-bg').trim() || '#f3f5f8'
  const foreground = styles.getPropertyValue('--shell-tool-code-text').trim() || '#1e293b'
  const border = styles.getPropertyValue('--shell-tool-divider').trim() || '#cbd5e1'
  const accent = styles.getPropertyValue('--shell-accent').trim() || '#0f766e'
  monaco.editor.defineTheme('pyin-config-light', {
    base: 'vs',
    inherit: true,
    colors: { 'editor.background': background, 'editor.foreground': foreground, 'editorLineNumber.foreground': '#94a3b8', 'editorCursor.foreground': accent, 'editorIndentGuide.background1': border },
    rules: []
  })
  monaco.editor.defineTheme('pyin-config-dark', {
    base: 'vs-dark',
    inherit: true,
    colors: { 'editor.background': background, 'editor.foreground': foreground, 'editorCursor.foreground': accent, 'editorIndentGuide.background1': border },
    rules: []
  })
}

function applyTheme() {
  defineThemes()
  monaco.editor.setTheme(document.documentElement.dataset.theme === 'dark' ? 'pyin-config-dark' : 'pyin-config-light')
}

function validate() {
  try {
    const value = JSON.parse(model?.getValue() || '')
    emit('validation-change', value && typeof value === 'object' && !Array.isArray(value) || Array.isArray(value))
  } catch {
    emit('validation-change', false)
  }
}

function formatJson() {
  try {
    model.setValue(JSON.stringify(JSON.parse(model.getValue()), null, 2))
  } catch {
    emit('validation-change', false)
  }
}
</script>

<style scoped>
.json-editor { border: 1px solid var(--shell-tool-border-strong); border-radius: 6px; overflow: hidden; }
.json-editor-toolbar { display: flex; min-height: 34px; align-items: center; justify-content: space-between; padding: 0 10px; border-bottom: 1px solid var(--shell-tool-divider); background: var(--shell-tool-toolbar-bg); color: var(--shell-tool-subtle-text); font-size: 12px; font-weight: 600; }
.json-editor-container { height: 320px; background: var(--shell-tool-code-bg); }
</style>
