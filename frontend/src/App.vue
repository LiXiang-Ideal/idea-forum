<script setup lang="ts">
import { computed, onBeforeMount, onBeforeUnmount, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { ApiError } from './api/http'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const searchText = ref('')
const signOutError = ref('')
const activeNav = computed(() => {
  if (route.name !== 'home') return ''
  if (route.hash === '#topics') return 'topics'
  return 'discover'
})

function handleNavigationClick(event: MouseEvent, hash: string) {
  if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
  if (route.name !== 'home' || route.hash !== hash || Object.keys(route.query).length > 0) return

  event.preventDefault()
  const target = hash ? document.querySelector(hash) : null
  if (target) {
    target.scrollIntoView({ behavior: 'smooth' })
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function search() {
  const keyword = searchText.value.trim()
  router.push({ name: 'home', query: keyword ? { keyword } : {} })
}

async function signOut() {
  signOutError.value = ''
  try {
    await auth.signOut()
    await router.push({ name: 'home' })
  } catch (cause) {
    signOutError.value = cause instanceof ApiError ? cause.message : '退出失败，请稍后重试。'
  }
}

function handleUnauthorized() {
  auth.clearSession()
  const currentRoute = router.currentRoute.value
  if (currentRoute.meta.requiresAuth) {
    router.replace({ name: 'login', query: { redirect: currentRoute.fullPath } })
  }
}

onBeforeMount(() => window.addEventListener('community:unauthorized', handleUnauthorized))
onBeforeUnmount(() => window.removeEventListener('community:unauthorized', handleUnauthorized))
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="topbar-inner">
        <RouterLink class="brand" :to="{ name: 'home' }" aria-label="IdeaForum 首页">
          <span class="brand-mark">&lt;/&gt;</span>
          <span>IdeaForum</span>
        </RouterLink>

        <nav class="main-nav" aria-label="主导航">
          <RouterLink
            :to="{ name: 'home' }"
            :class="{ active: activeNav === 'discover' }"
            @click="handleNavigationClick($event, '')"
          >发现</RouterLink>
          <RouterLink
            :to="{ name: 'home', hash: '#topics' }"
            :class="{ active: activeNav === 'topics' }"
            @click="handleNavigationClick($event, '#topics')"
          >
            技术话题
          </RouterLink>
        </nav>

        <form class="top-search" role="search" @submit.prevent="search">
          <span aria-hidden="true">⌕</span>
          <input v-model="searchText" aria-label="搜索文章" placeholder="搜索文章、技术…" />
          <kbd>↵</kbd>
        </form>

        <div class="top-actions">
          <RouterLink class="write-button" :to="{ name: 'write' }">
            <span aria-hidden="true">＋</span> 写文章
          </RouterLink>
          <template v-if="auth.isAuthenticated">
            <RouterLink class="user-greeting" :to="{ name: 'profile' }">
              <img
                v-if="auth.profile?.avatarUrl"
                class="user-avatar-image"
                :src="auth.profile.avatarUrl"
                alt=""
              />
              <span v-else class="user-avatar-fallback" aria-hidden="true">
                {{ (auth.profile?.displayName || auth.profile?.username || '我').slice(0, 1) }}
              </span>
              {{ auth.profile?.displayName || auth.profile?.username || '个人中心' }}
            </RouterLink>
            <button class="text-button" type="button" @click="signOut">退出</button>
          </template>
          <RouterLink v-else class="sign-in-link" :to="{ name: 'login' }">登录</RouterLink>
          <span v-if="auth.sessionError || signOutError" class="session-error" role="alert">
            {{ auth.sessionError || signOutError }}
          </span>
        </div>
      </div>
    </header>

    <RouterView />

    <footer id="about" class="site-footer">
      <div class="footer-inner">
        <span class="footer-brand"><span class="brand-mark">&lt;/&gt;</span> IdeaForum</span>
        <span>为开发者记录灵感、分享实践。</span>
        <span>独立创作 · 友善交流</span>
      </div>
    </footer>
  </div>
</template>
