import { defineStore } from 'pinia'
import { workshopApi } from '../api/workshop'

export interface Mapping { sheetId: string; cellAddress: string; jsonPath: string; format?: string; emptyValue?: string }

export const useWorkshopStore = defineStore('export-workshop', {
  state: () => ({ nodes: [] as any[], active: null as any, tabs: [] as any[], mappings: [] as Mapping[], loading: false, changedCells: [] as any[] }),
  actions: {
    async refreshTree() { this.nodes = await workshopApi.tree() },
    async openTemplate(id: number) {
      this.loading = true
      try {
        const template = await workshopApi.template(id)
        const existing = this.tabs.find(tab => tab.id === id)
        if (!existing) this.tabs.push(template)
        else Object.assign(existing, template)
        this.active = template; this.mappings = template.mappings || []
      } finally { this.loading = false }
    },
    closeTab(id: number) { this.tabs = this.tabs.filter(tab => tab.id !== id); if (this.active?.id === id) this.active = this.tabs.length ? this.tabs[this.tabs.length - 1] : null },
    async save(snapshot: any) {
      if (!this.active) return
      const saved = await workshopApi.save(this.active.id, { name: this.active.name, workbookSnapshot: snapshot, mappings: this.mappings })
      this.active = saved; const tab = this.tabs.find(item => item.id === saved.id); if (tab) Object.assign(tab, saved)
    }
  }
})
