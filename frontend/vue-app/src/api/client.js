import axios from 'axios'

/**
 * API Gateway(8080 포트)를 baseURL로 하는 axios 인스턴스.
 * 프론트엔드는 이 인스턴스 하나만 쓰면 되고, 어떤 마이크로서비스가 실제로
 * 요청을 처리하는지는 신경 쓸 필요가 없다 - Gateway가 알아서 라우팅해준다.
 *
 * [배포 시 수정 필요]
 * 운영 도메인으로 배포할 때는 이 baseURL을 실제 API 도메인으로 바꿔야 한다.
 * 지금은 .env 파일 없이 로컬 개발 기준 값으로 하드코딩되어 있다.
 */
const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000, // LLM 응답은 시간이 걸릴 수 있어 넉넉하게 30초로 설정
})

export default apiClient
