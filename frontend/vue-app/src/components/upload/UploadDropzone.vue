<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { uploadDocument, getUploadProgress } from '../../api/document'

/**
 * PDF 드래그앤드롭 업로드 영역.
 *
 * emits:
 *  - 'uploaded' (uploadResult) : 업로드 성공 시 백엔드 응답 데이터를 그대로 전달
 *  - 'error' (message)         : 업로드 실패(형식 오류, 네트워크 오류 등) 시 에러 메시지 전달
 */
const emit = defineEmits(['uploaded', 'error'])

const isDragging = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(null) // { phase, current, total }
let pollInterval = null

function startPolling(uploadId) {
  pollInterval = setInterval(async () => {
    try {
      uploadProgress.value = await getUploadProgress(uploadId)
    } catch {
      // 폴 실패는 무시 - 메인 업로드 요청이 완료되면 자동으로 멈춤
    }
  }, 1500)
}

function stopPolling() {
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}

onUnmounted(stopPolling)

/**
 * 폴링 결과를 퍼센트(0~100)로 변환한다.
 * - parsing 단계: 10% (PDF 추출, 수 초 내로 끝남)
 * - embedding 단계: 10~90% (배치 진행에 비례)
 * - saving 단계: 92% (DB 저장, 빠름)
 * - 그 외(시작 전 / 알 수 없음): 5%
 */
const progressPercent = computed(() => {
  const p = uploadProgress.value
  if (!p || p.phase === 'done') return 5
  if (p.phase === 'parsing') return 10
  if (p.phase === 'embedding') {
    if (p.total === 0) return 10
    return 10 + Math.round((p.current / p.total) * 80)
  }
  if (p.phase === 'saving') return 92
  return 5
})

const progressLabel = computed(() => {
  const p = uploadProgress.value
  if (!p || p.phase === 'parsing') return 'PDF 텍스트 추출 중…'
  if (p.phase === 'embedding') {
    if (p.total > 0) return `임베딩 중 (${p.current} / ${p.total} 배치)`
    return '임베딩 준비 중…'
  }
  if (p.phase === 'saving') return 'DB에 저장 중…'
  return '처리 중…'
})

/**
 * 드래그한 파일 또는 파일 선택창으로 고른 파일을 처리한다.
 * IN  : file - 사용자가 선택/드롭한 파일 (브라우저 File 객체)
 * OUT : 없음 (성공/실패 결과를 emit으로 부모 컴포넌트에 알린다)
 */
async function handleFile(file) {
  if (!file) return

  if (!file.name.toLowerCase().endsWith('.pdf')) {
    emit('error', 'PDF 파일만 업로드할 수 있어요.')
    return
  }

  const uploadId = crypto.randomUUID()
  isUploading.value = true
  uploadProgress.value = null
  startPolling(uploadId)

  try {
    const result = await uploadDocument(file, uploadId)
    emit('uploaded', result)
  } catch (error) {
    const message =
      error.response?.data?.errorMessage ?? '업로드 중 문제가 생겼어요. 잠시 후 다시 시도해주세요.'
    emit('error', message)
  } finally {
    stopPolling()
    isUploading.value = false
    uploadProgress.value = null
  }
}

function onDrop(event) {
  isDragging.value = false
  const file = event.dataTransfer.files?.[0]
  handleFile(file)
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  handleFile(file)
  // 같은 파일을 다시 선택해도 change 이벤트가 발생하도록 값을 비워둔다
  event.target.value = ''
}

function openFileDialog() {
  fileInput.value?.click()
}

const fileInput = ref(null)
</script>

<template>
  <div
    class="dropzone"
    :class="{ dragging: isDragging, uploading: isUploading }"
    @dragover.prevent="isDragging = true"
    @dragleave.prevent="isDragging = false"
    @drop.prevent="onDrop"
    @click="openFileDialog"
  >
    <input
      ref="fileInput"
      type="file"
      accept="application/pdf"
      class="hidden-input"
      @change="onFileSelected"
    />

    <div class="corner-fold" aria-hidden="true"></div>

    <template v-if="isUploading">
      <p class="dropzone-title">약관을 분석하는 중이에요…</p>
      <div class="progress-bar-wrap">
        <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <p class="dropzone-sub">{{ progressLabel }}</p>
      <p class="dropzone-hint">창을 닫아도 괜찮아요 — 처리는 서버에서 계속돼요.<br>같은 파일을 다시 올리면 즉시 이어서 사용할 수 있어요.</p>
    </template>
    <template v-else>
      <p class="dropzone-title">PDF 약관을 여기에 끌어다 놓으세요</p>
      <p class="dropzone-sub">또는 클릭해서 파일을 선택하세요 · 최대 20MB</p>
    </template>
  </div>
</template>

<style scoped>
.dropzone {
  position: relative;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  padding: 56px 32px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.dropzone:hover,
.dropzone.dragging {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 4px rgba(242, 183, 5, 0.18);
}

.dropzone.uploading {
  cursor: progress;
  opacity: 0.85;
}

.hidden-input {
  display: none;
}

.corner-fold {
  position: absolute;
  top: 0;
  right: 0;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, transparent 50%, var(--color-brand-soft) 50%);
  border-top-right-radius: var(--radius-lg);
}

.dropzone-title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 600;
  color: var(--color-ink);
}

.dropzone-sub {
  margin: 0;
  font-size: 14px;
  color: var(--color-ink-soft);
}

.progress-bar-wrap {
  width: 100%;
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  margin: 16px 0 10px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: var(--color-accent);
  border-radius: 4px;
  transition: width 0.8s ease;
  min-width: 4px;
}

.dropzone-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--color-ink-soft);
  opacity: 0.7;
  line-height: 1.5;
}
</style>
