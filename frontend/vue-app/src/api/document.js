import apiClient from './client'

/**
 * PDF 파일을 API Gateway를 통해 document-service로 업로드한다.
 *
 * IN  : file     - 업로드할 PDF 파일 (브라우저 File 객체)
 *       uploadId - 진행률 폴링용 UUID (호출자가 생성해서 전달)
 * OUT : Promise<{documentId, fileName, pageCount, charCount, uploadedAt}>
 */
export async function uploadDocument(file, uploadId) {
  const formData = new FormData()
  formData.append('file', file)

  const url = uploadId ? `/api/documents?uploadId=${uploadId}` : '/api/documents'
  const response = await apiClient.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // 대용량 PDF는 청크+임베딩 저장에 1~2분 걸릴 수 있음
  })

  return response.data.data
}

/**
 * 업로드 진행 상황을 조회한다. UploadDropzone에서 폴링 용도로 사용.
 *
 * IN  : uploadId - 진행률 조회할 업로드 식별자
 * OUT : Promise<{phase, current, total}> - phase: "parsing"|"embedding"|"saving"|"done"
 */
export async function getUploadProgress(uploadId) {
  const response = await apiClient.post('/api/documents/upload-progress', { uploadId })
  return response.data.data
}
