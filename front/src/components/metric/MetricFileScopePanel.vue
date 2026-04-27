<script setup>
defineProps({
  scopeMode: {
    type: String,
    default: 'project'
  },
  availableFiles: {
    type: Array,
    default: () => []
  },
  selectedFileIds: {
    type: Array,
    default: () => []
  },
  supportedLabel: {
    type: String,
    default: ''
  }
})

defineEmits([
  'update:scopeMode',
  'toggle-file',
  'select-all',
  'clear-selection'
])

function fileTypeLabel(fileType) {
  return (fileType || 'file').toUpperCase()
}

function fileSizeLabel(size) {
  if (!size) {
    return '0 B'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <section class="scope-panel">
    <div class="scope-panel-header">
      <div>
        <h3>分析范围</h3>
        <p>可对整个项目统一分析，也可只分析部分文件。</p>
      </div>
      <div class="scope-toggle">
        <button
          type="button"
          class="scope-button"
          :class="{ active: scopeMode === 'project' }"
          @click="$emit('update:scopeMode', 'project')"
        >
          整个项目
        </button>
        <button
          type="button"
          class="scope-button"
          :class="{ active: scopeMode === 'selected' }"
          @click="$emit('update:scopeMode', 'selected')"
        >
          指定文件
        </button>
      </div>
    </div>

    <div class="scope-panel-body">
      <p class="scope-helper">
        支持文件类型：{{ supportedLabel || '当前模块支持的文件' }}
      </p>

      <template v-if="scopeMode === 'selected'">
        <div class="scope-actions">
          <span>已选 {{ selectedFileIds.length }} / {{ availableFiles.length }}</span>
          <div>
            <button type="button" class="scope-action-link" @click="$emit('select-all')">全选</button>
            <button type="button" class="scope-action-link" @click="$emit('clear-selection')">清空</button>
          </div>
        </div>

        <div v-if="availableFiles.length === 0" class="scope-empty">
          当前项目没有可用于该模块分析的文件。
        </div>

        <div v-else class="scope-file-list">
          <label
            v-for="file in availableFiles"
            :key="file.id"
            class="scope-file-item"
          >
            <input
              type="checkbox"
              :checked="selectedFileIds.includes(file.id)"
              @change="$emit('toggle-file', file.id)"
            />
            <span class="scope-file-main">
              <span class="scope-file-name">{{ file.originalName }}</span>
              <span class="scope-file-meta">
                <i>{{ fileTypeLabel(file.fileType) }}</i>
                <span>{{ fileSizeLabel(file.size) }}</span>
              </span>
            </span>
          </label>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.scope-panel {
  margin-bottom: 18px;
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #fff;
}

.scope-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.scope-panel-header h3 {
  margin: 0;
  font-size: 16px;
  color: #111827;
}

.scope-panel-header p,
.scope-helper {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.scope-toggle {
  display: inline-flex;
  padding: 4px;
  border-radius: 8px;
  background: #f1f5f9;
}

.scope-button {
  border: 0;
  background: transparent;
  color: #475569;
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.scope-button.active {
  background: #2563eb;
  color: #fff;
}

.scope-panel-body {
  margin-top: 14px;
}

.scope-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #475569;
}

.scope-action-link {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  padding: 0 0 0 12px;
}

.scope-file-list {
  display: grid;
  gap: 10px;
  max-height: 260px;
  overflow: auto;
}

.scope-file-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #f8fafc;
}

.scope-file-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.scope-file-name {
  color: #0f172a;
  font-size: 14px;
  word-break: break-all;
}

.scope-file-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #64748b;
  font-size: 12px;
}

.scope-file-meta i {
  font-style: normal;
  padding: 2px 6px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
}

.scope-empty {
  margin-top: 10px;
  padding: 18px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 900px) {
  .scope-panel-header,
  .scope-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .scope-toggle {
    width: 100%;
  }

  .scope-button {
    flex: 1;
  }
}
</style>
