import { createApp } from 'vue'
import { createPinia } from 'pinia'

// Pretendard 가변 폰트 (한글 UI 본문용)
import 'pretendard/dist/web/variable/pretendardvariable.css'
// 디자인 토큰 + 전역 스타일
import './style.css'

import App from './App.vue'
import router from './router'

/**
 * 앱 부트스트랩.
 * Pinia(상태관리) + Vue Router를 등록한 뒤 #app에 마운트한다.
 */
const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
