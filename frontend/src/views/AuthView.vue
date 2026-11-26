<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isRegister = ref(false)
const username = ref('')
const email = ref('')
const displayName = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')
const title = computed(() => isRegister.value ? '加入这场分享。' : '欢迎回来。')

async function submit() {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    if (isRegister.value) {
      await auth.signUp({
        username: username.value,
        email: email.value,
        password: password.value,
        displayName: displayName.value,
      })
    } else {
      await auth.signIn(username.value, password.value)
    }
    const requestedRedirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    const redirect = requestedRedirect.startsWith('/') && !requestedRedirect.startsWith('//')
      ? requestedRedirect
      : '/'
    await router.replace(redirect)
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '暂时无法完成请求，请稍后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <div class="auth-aside">
      <span class="auth-asterisk" aria-hidden="true">✳</span>
      <p class="eyebrow">A COMMUNITY FOR MAKERS</p>
      <h1>把路上的<br /><span>微光分享出来。</span></h1>
      <p>每一次记录，都会成为另一个人的起点。</p>
      <span class="auth-aside-note">IdeaForum · 写给认真做事的人</span>
    </div>
    <section class="auth-card">
      <p class="eyebrow">{{ isRegister ? 'CREATE YOUR SPACE' : 'YOUR SPACE IS HERE' }}</p>
      <h2>{{ title }}</h2>
      <p class="auth-intro">{{ isRegister ? '创建账号，开始记录你的开发旅程。' : '登录后继续阅读、收藏与分享。' }}</p>
      <form @submit.prevent="submit">
        <label class="form-field">
          <span>用户名</span>
          <input v-model="username" autocomplete="username" minlength="3" maxlength="32"
            placeholder="字母、数字或下划线" required />
        </label>
        <label v-if="isRegister" class="form-field">
          <span>显示名称</span>
          <input v-model="displayName" autocomplete="nickname" maxlength="80" placeholder="社区里如何称呼你" required />
        </label>
        <label v-if="isRegister" class="form-field">
          <span>邮箱</span>
          <input v-model="email" type="email" autocomplete="email" maxlength="254" placeholder="you@example.com" required />
        </label>
        <label class="form-field">
          <span>密码</span>
          <input v-model="password" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'"
            :minlength="isRegister ? 8 : undefined" maxlength="72" placeholder="至少 8 位" required />
        </label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="primary-button auth-submit" type="submit" :disabled="submitting">
          {{ submitting ? '请稍候…' : isRegister ? '创建账号 ↗' : '登录 ↗' }}
        </button>
      </form>
      <p class="auth-toggle">
        {{ isRegister ? '已经有账号？' : '第一次来？' }}
        <button type="button" @click="isRegister = !isRegister; error = ''">
          {{ isRegister ? '去登录' : '创建账号' }}
        </button>
      </p>
    </section>
  </main>
</template>
