<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDocumentStore } from '../store/documentStore'
import UploadDropzone from '../components/upload/UploadDropzone.vue'
import BeforeAfterExample from '../components/upload/BeforeAfterExample.vue'

const router = useRouter()
const documentStore = useDocumentStore()
const errorMessage = ref('')

/**
 * 업로드 성공 시: 스토어에 문서 정보를 저장하고 채팅 화면으로 이동한다.
 * IN  : uploadResult - { documentId, fileName, pageCount, charCount, uploadedAt }
 * OUT : 없음 (라우터 이동)
 */
function handleUploaded(uploadResult) {
  errorMessage.value = ''
  documentStore.setDocument(uploadResult)
  router.push({ name: 'chat', params: { documentId: uploadResult.documentId } })
}

/**
 * 업로드 실패 시 에러 메시지를 화면에 표시한다.
 * IN  : message - 사용자에게 보여줄 에러 메시지
 */
function handleError(message) {
  errorMessage.value = message
}
</script>

<template>
  <main class="page">
    <div class="container">
      <span class="wordmark">약관 쉽게</span>

      <h1 class="headline">
        보험 약관, <br />
        이제 어렵지 않게 읽어보세요
      </h1>
      <p class="subhead">
        PDF 약관을 올리면 어려운 조항을 쉬운 말로 풀어 설명해드려요. 궁금한 부분은 채팅으로
        자유롭게 물어보세요.
      </p>

      <UploadDropzone @uploaded="handleUploaded" @error="handleError" />

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <BeforeAfterExample />

      <p class="privacy-note">
        업로드한 약관은 답변 생성에만 사용되며, 서버에는 임시로만 보관돼요.
      </p>
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  padding: 96px 24px 64px;
}

.container {
  width: 100%;
  max-width: 560px;
}

.wordmark {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-brand);
  letter-spacing: 0.04em;
  margin-bottom: 28px;
}

.headline {
  font-family: var(--font-display);
  font-size: 36px;
  line-height: 1.35;
  font-weight: 600;
  color: var(--color-ink);
  margin: 0 0 16px;
}

.subhead {
  font-size: 15.5px;
  line-height: 1.7;
  color: var(--color-ink-soft);
  margin: 0 0 40px;
}

.error-text {
  margin: 14px 2px 0;
  font-size: 13.5px;
  color: var(--color-danger);
}

.container :deep(.dropzone) {
  margin-bottom: 36px;
}

.privacy-note {
  margin: 24px 2px 0;
  font-size: 12.5px;
  color: var(--color-ink-faint);
  text-align: center;
}
</style>
