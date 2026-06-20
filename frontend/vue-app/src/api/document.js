import apiClient from './client'

/**
 * PDF 파일을 API Gateway를 통해 document-service로 업로드한다.
 *
 * IN  : file - 업로드할 PDF 파일 (브라우저 File 객체)
 * OUT : Promise<{documentId, fileName, pageCount, charCount, uploadedAt}>
 *       - 백엔드 ApiResponse 포맷에서 data 부분만 꺼내서 반환한다.
 */
export async function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/api/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return response.data.data
}
