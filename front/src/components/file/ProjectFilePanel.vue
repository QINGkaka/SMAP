<script setup>
const props = defineProps({
  projectId: {
    type: String,
    default: ''
  },
  files: {
    type: Array,
    default: () => []
  },
  state: {
    type: Object,
    default: () => ({
      keyword: '',
      type: 'all',
      sortBy: 'uploadedAt',
      sortOrder: 'desc',
      page: 1,
      pageSize: 5
    })
  },
  categories: {
    type: Array,
    default: () => []
  },
  pagedFiles: {
    type: Array,
    default: () => []
  },
  filteredCount: {
    type: Number,
    default: 0
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageCount: {
    type: Number,
    default: 1
  },
  panelTitle: {
    type: String,
    default: ''
  },
  refreshLabel: {
    type: String,
    default: '刷新'
  },
  showRefresh: {
    type: Boolean,
    default: false
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  showCategoryTabs: {
    type: Boolean,
    default: true
  },
  showPageInfo: {
    type: Boolean,
    default: true
  },
  showPagination: {
    type: Boolean,
    default: true
  },
  showDate: {
    type: Boolean,
    default: false
  },
  showUploadArea: {
    type: Boolean,
    default: false
  },
  uploadLabel: {
    type: String,
    default: '点击上传'
  },
  uploadHint: {
    type: String,
    default: '支持 zip、java、oom、xml、xmi 文件'
  },
  selectedFileName: {
    type: String,
    default: ''
  },
  uploadLoading: {
    type: Boolean,
    default: false
  },
  uploadButtonText: {
    type: String,
    default: '保存到项目'
  },
  itemClass: {
    type: String,
    default: 'upload-item'
  },
  emptyMessage: {
    type: String,
    default: '暂无上传文件'
  },
  requireProjectSelection: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'refresh',
  'file-change',
  'upload',
  'update-state',
  'change-page',
  'remove-file'
])

function emitState(patch) {
  emit('update-state', patch)
}

function handleFileChange(event) {
  emit('file-change', event)
}

function handleRemove(file) {
  emit('remove-file', file)
}

function handlePageChange(page) {
  emit('change-page', page)
}
</script>

<template>
  <div class="project-file-panel">
    <div v-if="showUploadArea" class="project-file-panel-upload">
      <div class="upload-action-row">
        <label class="upload-button upload-picker-button">
          {{ uploadLabel }}
          <input
            type="file"
            accept=".zip,.java,.oom,.xml,.xmi"
            multiple
            hidden
            @change="handleFileChange"
          />
        </label>
        <button type="button" class="primary-button upload-submit-button" :disabled="uploadLoading" @click="emit('upload')">
          {{ uploadLoading ? '上传中...' : uploadButtonText }}
        </button>
      </div>
      <div class="file-status">
        <div class="file-status-copy">
          <span class="file-status-label">支持格式</span>
          <strong>{{ selectedFileName || uploadHint }}</strong>
        </div>
        <span class="success-dot"></span>
      </div>
    </div>

    <div class="upload-list">
      <div v-if="panelTitle || showRefresh" class="project-list-header">
        <strong>{{ panelTitle }}</strong>
        <button v-if="showRefresh" type="button" class="text-button" @click="emit('refresh')">{{ refreshLabel }}</button>
      </div>

      <div v-if="requireProjectSelection && !projectId" class="empty-state compact">请先选择项目</div>
      <template v-else>
        <div v-if="showToolbar" class="file-toolbar">
          <input
            :value="state.keyword"
            type="search"
            placeholder="搜索文件名、类型、ID"
            @input="emitState({ keyword: $event.target.value, page: 1 })"
          />
          <select
            :value="state.sortBy"
            @change="emitState({ sortBy: $event.target.value, page: 1 })"
          >
            <option value="uploadedAt">按上传时间</option>
            <option value="name">按文件名</option>
            <option value="type">按类型</option>
            <option value="size">按大小</option>
          </select>
          <select
            :value="state.sortOrder"
            @change="emitState({ sortOrder: $event.target.value, page: 1 })"
          >
            <option value="desc">降序</option>
            <option value="asc">升序</option>
          </select>
          <select
            :value="state.pageSize"
            @change="emitState({ pageSize: Number($event.target.value), page: 1 })"
          >
            <option :value="5">每页 5 条</option>
            <option :value="10">每页 10 条</option>
            <option :value="20">每页 20 条</option>
          </select>
        </div>

        <div v-if="showCategoryTabs" class="file-category-tabs">
          <button
            v-for="category in categories"
            :key="category.key"
            type="button"
            class="file-category-button"
            :class="{ active: state.type === category.key }"
            @click="emitState({ type: category.key, page: 1 })"
          >
            {{ category.label }}
          </button>
        </div>

        <div v-if="showPageInfo" class="file-page-info">
          共 {{ filteredCount }} 条，当前第 {{ currentPage }} / {{ pageCount }} 页
        </div>

        <div v-if="files.length === 0" class="empty-state compact">{{ emptyMessage }}</div>
        <div v-else-if="filteredCount === 0" class="empty-state compact">没有匹配当前筛选条件的文件</div>

        <article v-for="file in pagedFiles" :key="file.id" :class="itemClass">
          <div>
            <strong>{{ file.originalName }}</strong>
            <small v-if="showDate">{{ file.fileType }} · {{ file.sizeLabel }} · {{ file.uploadedAtLabel }}</small>
            <small v-else>{{ file.fileType }} · {{ file.sizeLabel }}</small>
          </div>
          <div v-if="showDate" class="upload-meta">
            <button type="button" class="mini-button danger" @click="handleRemove(file)">删除</button>
          </div>
          <button v-else type="button" class="mini-button danger" @click="handleRemove(file)">删除</button>
        </article>

        <div v-if="showPagination && filteredCount > 0" class="file-pagination">
          <button type="button" class="mini-button" :disabled="currentPage <= 1" @click="handlePageChange(currentPage - 1)">
            上一页
          </button>
          <span>第 {{ currentPage }} / {{ pageCount }} 页</span>
          <button type="button" class="mini-button" :disabled="currentPage >= pageCount" @click="handlePageChange(currentPage + 1)">
            下一页
          </button>
        </div>
      </template>
    </div>
  </div>
</template>
