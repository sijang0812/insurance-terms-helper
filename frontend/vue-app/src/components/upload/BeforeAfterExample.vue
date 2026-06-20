<script setup>
/**
 * 약관 원문 한 줄을 어떻게 풀어주는지 보여주는 정적 예시 카드.
 * 사용자가 업로드 전에 "이 서비스가 뭘 해주는지"를 직관적으로 이해하도록 돕는다.
 * (props 없이 고정 예시 - 추후 실제 업로드 사례로 교체 가능)
 */
</script>

<template>
  <div class="example-card">
    <span class="example-label">이렇게 풀어드려요</span>

    <div class="example-row">
      <span class="tag tag-before">약관 원문</span>
      <p class="example-text original">
        "본 계약은 청약일로부터 90일의 면책기간을 두며, 면책기간 중 발생한 사고에 대하여는
        보험금을 지급하지 아니한다."
      </p>
    </div>

    <div class="arrow" aria-hidden="true">↓</div>

    <div class="example-row">
      <span class="tag tag-after">쉬운 설명</span>
      <p class="example-text simplified">
        가입하고 <mark>90일 안에</mark> 사고가 나면 <mark>보험금을 못 받아요.</mark> 90일이
        지난 뒤부터 보장이 시작돼요.
      </p>
    </div>
  </div>
</template>

<style scoped>
.example-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px 28px;
}

.example-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-faint);
  letter-spacing: 0.02em;
  margin-bottom: 16px;
}

.example-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.tag {
  flex: none;
  margin-top: 2px;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 9px;
  border-radius: 999px;
}

.tag-before {
  background: #f1eee8;
  color: var(--color-ink-soft);
}

.tag-after {
  background: var(--color-brand-soft);
  color: var(--color-brand);
}

.example-text {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.7;
}

.original {
  color: var(--color-ink-soft);
}

.simplified {
  color: var(--color-ink);
}

.simplified mark {
  background: rgba(242, 183, 5, 0.4);
  color: var(--color-accent-ink);
  padding: 0 2px;
  border-radius: 3px;
}

.arrow {
  text-align: center;
  color: var(--color-ink-faint);
  margin: 10px 0;
  font-size: 14px;
}
</style>
