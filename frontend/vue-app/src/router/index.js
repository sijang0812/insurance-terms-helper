import { createRouter, createWebHistory } from 'vue-router'
import UploadView from '../views/UploadView.vue'
import ChatView from '../views/ChatView.vue'

/**
 * 라우트 정의.
 * '/'               : 첫 화면 - PDF 업로드
 * '/chat/:documentId' : 업로드 완료 후 이동하는 채팅 화면. documentId로 어떤 문서에 대해
 *                       대화 중인지 식별한다.
 */
const routes = [
  {
    path: '/',
    name: 'upload',
    component: UploadView,
  },
  {
    path: '/chat/:documentId',
    name: 'chat',
    component: ChatView,
    props: true,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
