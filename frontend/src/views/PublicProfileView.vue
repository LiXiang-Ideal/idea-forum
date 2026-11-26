<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  getFollowers,
  getFollowing,
  getPublicArticles,
  getPublicProfile,
  setUserFollow,
} from '../api/community'
import { ApiError } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { Article, FollowUser, PublicProfile } from '../types'

type ProfileTab = 'articles' | 'followers' | 'following'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const username = computed(() => String(route.params.username))
const profile = ref<PublicProfile | null>(null)
const activeTab = ref<ProfileTab>('articles')
const profileLoading = ref(true)
const profileError = ref('')
const followLoading = ref(false)
const followError = ref('')
const listLoading = ref(false)
const listError = ref('')
const articles = ref<Article[]>([])
const users = ref<FollowUser[]>([])
const page = ref(1)
const total = ref(0)
const pageSize = 12
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const ownProfile = computed(() => auth.profile?.username === profile.value?.username)
let profileRequestId = 0
let listRequestId = 0

const tabs: { id: ProfileTab; label: string }[] = [
  { id: 'articles', label: '文章' },
  { id: 'followers', label: '粉丝' },
  { id: 'following', label: '关注' },
]

async function loadProfile() {
  const requestId = ++profileRequestId
  listRequestId += 1
  profileLoading.value = true
  profileError.value = ''
  listLoading.value = false
  listError.value = ''
  profile.value = null
  try {
    const result = await getPublicProfile(username.value)
    if (requestId !== profileRequestId) return
    profile.value = result
    void loadList()
  } catch (cause) {
    if (requestId === profileRequestId) {
      profileError.value = cause instanceof ApiError ? cause.message : '作者主页加载失败。'
    }
  } finally {
    if (requestId === profileRequestId) profileLoading.value = false
  }
}

async function loadList() {
  if (!profile.value) return
  const requestId = ++listRequestId
  listLoading.value = true
  listError.value = ''
  try {
    if (activeTab.value === 'articles') {
      const result = await getPublicArticles(username.value, page.value, pageSize)
      if (requestId !== listRequestId) return
      total.value = result.total
      articles.value = result.items
      users.value = []
    } else if (activeTab.value === 'followers') {
      const result = await getFollowers(username.value, page.value, pageSize)
      if (requestId !== listRequestId) return
      total.value = result.total
      users.value = result.items
      articles.value = []
    } else {
      const result = await getFollowing(username.value, page.value, pageSize)
      if (requestId !== listRequestId) return
      total.value = result.total
      users.value = result.items
      articles.value = []
    }
  } catch (cause) {
    if (requestId === listRequestId) {
      listError.value = cause instanceof ApiError ? cause.message : '作者动态加载失败。'
    }
  } finally {
    if (requestId === listRequestId) listLoading.value = false
  }
}

function selectTab(tab: ProfileTab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  page.value = 1
  articles.value = []
  users.value = []
  total.value = 0
}

function changePage(nextPage: number) {
  page.value = nextPage
}

async function toggleFollow() {
  if (!profile.value || followLoading.value) return
  if (!auth.isAuthenticated) {
    void router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const target = profile.value
  const requestId = profileRequestId
  followLoading.value = true
  followError.value = ''
  try {
    const result = await setUserFollow(target.username, !target.following)
    if (requestId === profileRequestId) profile.value = result
  } catch (cause) {
    followError.value = cause instanceof ApiError ? cause.message : '关注操作失败，请稍后重试。'
  } finally {
    followLoading.value = false
  }
}

onMounted(() => void loadProfile())
watch(username, () => {
  activeTab.value = 'articles'
  page.value = 1
  articles.value = []
  users.value = []
  void loadProfile()
})
watch([activeTab, page], () => void loadList())
</script>

<template>
  <main class="profile-page public-profile-page">
    <RouterLink class="back-link" :to="{ name: 'home' }">← 返回发现</RouterLink>

    <div v-if="profileLoading" class="state-card">
      <span class="loading-mark"></span> 正在打开作者主页…
    </div>
    <section v-else-if="profileError" class="state-card error-state">
      <strong>作者主页暂时无法打开</strong>
      <span>{{ profileError }}</span>
      <button class="outline-button" type="button" @click="loadProfile">再试一次</button>
    </section>
    <template v-else-if="profile">
      <section class="public-author-card">
        <div class="public-author-avatar">
          <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="" />
          <span v-else>{{ (profile.displayName || profile.username).slice(0, 1) }}</span>
        </div>
        <div class="public-author-copy">
          <p class="eyebrow">AUTHOR PROFILE</p>
          <h1>{{ profile.displayName }}</h1>
          <span class="public-author-username">@{{ profile.username }}</span>
          <p class="public-author-bio">{{ profile.bio || '这位作者还没有填写个人简介。' }}</p>
        </div>
        <div class="public-author-actions">
          <button
            v-if="!ownProfile"
            class="primary-button follow-button"
            :class="{ followed: profile.following }"
            type="button"
            :disabled="followLoading"
            :aria-label="profile.following ? '取消关注' : '关注作者'"
            @click="toggleFollow"
          >
            {{ followLoading ? '请稍候…' : profile.following ? '✓ 已关注' : '＋ 关注' }}
          </button>
          <p v-if="followError" class="form-error" role="alert">{{ followError }}</p>
          <div class="public-author-stats">
            <button type="button" @click="selectTab('followers')">
              <strong>{{ profile.followerCount }}</strong><span>粉丝</span>
            </button>
            <button type="button" @click="selectTab('following')">
              <strong>{{ profile.followingCount }}</strong><span>关注</span>
            </button>
          </div>
        </div>
      </section>

      <nav class="profile-tabs" role="tablist" aria-label="作者内容">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          role="tab"
          :aria-selected="activeTab === tab.id"
          :class="{ selected: activeTab === tab.id }"
          @click="selectTab(tab.id)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <section class="profile-activity">
        <div v-if="listLoading" class="state-card">
          <span class="loading-mark"></span> 正在整理作者内容…
        </div>
        <div v-else-if="listError" class="state-card error-state">
          <strong>内容暂时无法加载</strong>
          <span>{{ listError }}</span>
          <button class="outline-button" type="button" @click="loadList">再试一次</button>
        </div>
        <div v-else-if="activeTab === 'articles' && articles.length" class="public-article-list">
          <RouterLink
            v-for="article in articles"
            :key="article.id"
            class="public-article-card"
            :to="{ name: 'article', params: { id: article.id } }"
          >
            <div class="article-card-top">
              <span v-if="article.categoryName" class="category-pill">{{ article.categoryName }}</span>
              <time v-if="article.publishedAt">{{ new Date(article.publishedAt).toLocaleDateString('zh-CN') }}</time>
            </div>
            <strong>{{ article.title }}</strong>
            <p>{{ article.summary || article.body }}</p>
            <span class="public-article-stats">
              {{ article.viewCount }} 阅读 · {{ article.likeCount }} 赞 · {{ article.commentCount }} 评论
            </span>
          </RouterLink>
        </div>
        <div v-else-if="activeTab !== 'articles' && users.length" class="follow-user-list">
          <RouterLink
            v-for="user in users"
            :key="user.id"
            class="follow-user-card"
            :to="{ name: 'author', params: { username: user.username } }"
          >
            <span class="author-avatar large-avatar">
              <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
              <span v-else>{{ (user.displayName || user.username).slice(0, 1) }}</span>
            </span>
            <span class="follow-user-copy">
              <strong>{{ user.displayName }}</strong>
              <span>@{{ user.username }}</span>
              <span v-if="user.bio">{{ user.bio }}</span>
            </span>
            <span class="profile-activity-arrow" aria-hidden="true">↗</span>
          </RouterLink>
        </div>
        <div v-else class="state-card empty-state">
          <span class="empty-icon">✳</span>
          <strong>{{ activeTab === 'articles' ? '还没有公开文章' : activeTab === 'followers' ? '还没有粉丝' : '还没有关注其他作者' }}</strong>
          <span>作者的新内容和关注关系会显示在这里。</span>
        </div>

        <div v-if="!listLoading && !listError && pageCount > 1" class="pagination">
          <button :disabled="page <= 1" @click="changePage(page - 1)">← 上一页</button>
          <span>{{ page }} / {{ pageCount }}</span>
          <button :disabled="page >= pageCount" @click="changePage(page + 1)">下一页 →</button>
        </div>
      </section>
    </template>
  </main>
</template>
