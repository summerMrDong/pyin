<template>
  <div class="json-editor">
    <div class="json-editor-toolbar">
      <span>JSON 编辑器</span>
      <span class="json-editor-actions">
        <span v-if="validationMessage" class="json-editor-validation" role="alert">{{ validationMessage }}</span>
        <el-button link type="primary" title="格式化 JSON" aria-label="格式化 JSON" @click="formatJson"><el-icon><MagicStick /></el-icon></el-button>
      </span>
    </div>
    <div ref="containerRef" class="json-editor-container" />
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api'
import 'monaco-editor/esm/vs/language/json/monaco.contribution'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'

const props = defineProps({
  modelValue: { type: String, default: '' },
  validationMessage: { type: String, default: '' }
})
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
  applyValidationMarker()
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

watch(() => props.validationMessage, applyValidationMarker)

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

function applyValidationMarker() {
  if (!model) return
  monaco.editor.setModelMarkers(model, 'pyin-json-validation', props.validationMessage ? [{
    startLineNumber: 1,
    startColumn: 1,
    endLineNumber: 1,
    endColumn: 1,
    message: props.validationMessage,
    severity: monaco.MarkerSeverity.Error
  }] : [])
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
.json-editor { width: 100%; min-width: 0; border: 1px solid var(--shell-tool-border-strong); border-radius: 6px; overflow: hidden; }
.json-editor-toolbar, .json-editor-actions { display: flex; align-items: center; }
.json-editor-toolbar { min-height: 34px; justify-content: space-between; gap: 8px; padding: 0 10px; border-bottom: 1px solid var(--shell-tool-divider); background: var(--shell-tool-toolbar-bg); color: var(--shell-tool-subtle-text); font-size: 12px; font-weight: 600; }
.json-editor-actions { min-width: 0; gap: 4px; }
.json-editor-actions :deep(.el-button) { width: 24px; height: 24px; margin: 0; padding: 0; font-size: 15px; }
.json-editor-validation { overflow: hidden; color: var(--el-color-danger, #d14d4d); font-size: 10px; font-weight: 400; text-overflow: ellipsis; white-space: nowrap; }
.json-editor-container { width: 100%; height: 320px; background: var(--shell-tool-code-bg); }
</style>
