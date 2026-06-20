import { defineStore } from 'pinia'

/**
 * 현재 화면에서 다루고 있는 문서(업로드된 PDF)의 정보를 보관하는 스토어.
 * 업로드 화면에서 채팅 화면으로 이동할 때, 화면 새로고침 없이 바로 참조할 수 있게 해준다.
 */
export const useDocumentStore = defineStore('document', {
  state: () => ({
    documentId: null,
    fileName: null,
    pageCount: 0,
    charCount: 0,
  }),

  actions: {
    /**
     * 업로드 성공 응답으로 스토어 상태를 채운다.
     * IN  : uploadResult - api/document.js의 uploadDocument() 반환값
     * OUT : 없음
     */
    setDocument(uploadResult) {
      this.documentId = uploadResult.documentId
      this.fileName = uploadResult.fileName
      this.pageCount = uploadResult.pageCount
      this.charCount = uploadResult.charCount
    },
  },
})
