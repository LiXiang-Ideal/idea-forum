<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  changeCurrentPassword,
  getCurrentProfile,
  getMyArticles,
  getMyBookmarks,
  getMyComments,
  getMyLikes,
  uploadCurrentAvatar,
  updateCurrentProfile,
} from '../api/community'
import { ApiError } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { ProfileArticle, ProfileComment, UserProfile } from '../types'

type ProfileTab = 'profile' | 'articles' | 'likes' | 'bookmarks' | 'comments'

const auth = useAuthStore()
const tabs: { id: ProfileTab; label: string }[] = [
  { id: 'profile', label: '个人资料' },
  { id: 'articles', label: '我的内容' },
  { id: 'likes', label: '我的点赞' },
  { id: 'bookmarks', label: '我的收藏' },
  { id: 'comments', label: '我的评论' },
]
const activeTab = ref<ProfileTab>('profile')
const profileForm = reactive({ displayName: '', avatarUrl: '', bio: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmNewPassword: '' })
const passwordExpanded = ref(false)
const profileLoading = ref(true)
const profileLoaded = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)
const profileError = ref('')
const profileNotice = ref('')
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarPreview = ref('')
const avatarImageFailed = ref(false)
const avatarUploading = ref(false)
const avatarError = ref('')
const avatarNotice = ref('')
const passwordError = ref('')
const passwordNotice = ref('')
const activityLoading = ref(false)
const activityError = ref('')
const articleItems = ref<ProfileArticle[]>([])
const commentItems = ref<ProfileComment[]>([])
const page = ref(1)
const total = ref(0)
const pageSize = 12
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
let activityRequestId = 0
let avatarObjectUrl: string | null = null

const profileInitial = computed(() => (profileForm.displayName || auth.profile?.username || '我').slice(0, 1))

function applyProfile(profile: UserProfile) {
  profileForm.displayName = profile.displayName
  profileForm.avatarUrl = profile.avatarUrl ?? ''
  profileForm.bio = profile.bio ?? ''
}

async function loadProfile() {
  profileLoading.value = true
  profileError.value = ''
  try {
    const profile = await getCurrentProfile()
    auth.updateProfile(profile)
    applyProfile(profile)
    profileLoaded.value = true
  } catch (cause) {
    profileError.value = cause instanceof ApiError ? cause.message : '个人资料加载失败。'
  } finally {
    profileLoading.value = false
  }
}

async function saveProfile() {
  if (profileSaving.value) return
  profileSaving.value = true
  profileError.value = ''
  profileNotice.value = ''
  try {
    const profile = await updateCurrentProfile(profileForm)
    auth.updateProfile(profile)
    applyProfile(profile)
    profileNotice.value = '个人资料已保存。'
  } catch (cause) {
    profileError.value = cause instanceof ApiError ? cause.message : '个人资料保存失败。'
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  if (passwordSaving.value) return
  passwordSaving.value = true
  passwordError.value = ''
  passwordNotice.value = ''
  try {
    await changeCurrentPassword(passwordForm)
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmNewPassword = ''
    passwordNotice.value = '密码已更新。'
  } catch (cause) {
    passwordError.value = cause instanceof ApiError ? cause.message : '密码更新失败。'
  } finally {
    passwordSaving.value = false
  }
}

function chooseAvatar() {
  avatarInput.value?.click()
}

function clearAvatarPreview() {
  if (avatarObjectUrl) {
    URL.revokeObjectURL(avatarObjectUrl)
    avatarObjectUrl = null
  }
  avatarPreview.value = ''
}

async function uploadAvatar(event: Event) {
  const input = event.currentTarget as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  avatarError.value = ''
  avatarNotice.value = ''
  if (file.size > 5 * 1024 * 1024) {
    avatarError.value = '头像文件不能超过 5 MB。'
    input.value = ''
    return
  }
  if (file.type && !['image/jpeg', 'image/png'].includes(file.type)) {
    avatarError.value = '请选择 JPEG 或 PNG 图片。'
    input.value = ''
    return
  }

  clearAvatarPreview()
  avatarObjectUrl = URL.createObjectURL(file)
  avatarPreview.value = avatarObjectUrl
  avatarImageFailed.value = false
  avatarUploading.value = true
  try {
    const profile = await uploadCurrentAvatar(file)
    auth.updateProfile(profile)
    profileForm.avatarUrl = profile.avatarUrl ?? ''
    clearAvatarPreview()
    avatarNotice.value = '头像已更新。'
  } catch (cause) {
    clearAvatarPreview()
    avatarError.value = cause instanceof ApiError ? cause.message : '头像上传失败，请稍后重试。'
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

function togglePasswordForm() {
  passwordExpanded.value = !passwordExpanded.value
  passwordError.value = ''
  passwordNotice.value = ''
  if (!passwordExpanded.value) {
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmNewPassword = ''
  }
}

function handleAvatarImageError() {
  avatarImageFailed.value = true
}

async function loadActivity() {
  if (activeTab.value === 'profile') return
  const requestId = ++activityRequestId
  activityLoading.value = true
  activityError.value = ''
  try {
    if (activeTab.value === 'comments') {
      const result = await getMyComments(page.value, pageSize)
      if (requestId !== activityRequestId) return
      commentItems.value = result.items
      total.value = result.total
    } else {
      const loadPage = activeTab.value === 'articles'
        ? getMyArticles
        : activeTab.value === 'likes'
          ? getMyLikes
          : getMyBookmarks
      const result = await loadPage(page.value, pageSize)
      if (requestId !== activityRequestId) return
      articleItems.value = result.items
      total.value = result.total
    }
  } catch (cause) {
    if (requestId === activityRequestId) {
      activityError.value = cause instanceof ApiError ? cause.message : '动态加载失败。'
    }
  } finally {
    if (requestId === activityRequestId) activityLoading.value = false
  }
}

function selectTab(tab: ProfileTab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  activityRequestId += 1
  page.value = 1
  articleItems.value = []
  commentItems.value = []
  total.value = 0
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadActivity()
}

onMounted(() => void loadProfile())
onBeforeUnmount(clearAvatarPreview)
watch(() => profileForm.avatarUrl, () => {
  avatarImageFailed.value = false
})
watch(activeTab, (tab) => {
  if (tab !== 'profile') void loadActivity()
})
</script>

<template>
  <main class="profile-page">
    <header class="profile-heading">
      <div>
        <p class="eyebrow">YOUR SPACE</p>
        <h1>个人中心</h1>
        <p>管理你的资料，回看一路写下和收藏的内容。</p>
      </div>
      <span class="write-mark" aria-hidden="true">✳</span>
    </header>

    <nav class="profile-tabs" role="tablist" aria-label="个人中心">
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

    <div v-if="activeTab === 'profile'" class="profile-panels">
      <section class="profile-panel editor-card">
        <p class="eyebrow">PROFILE</p>
        <h2>个人资料</h2>
        <p v-if="profileLoading" class="profile-hint">正在加载个人资料…</p>
        <div v-else-if="!profileLoaded" class="profile-load-error">
          <p class="form-error" role="alert">{{ profileError }}</p>
          <button class="outline-button" type="button" @click="loadProfile">再试一次</button>
        </div>
        <div v-else class="profile-edit">
          <div class="avatar-editor">
            <div class="profile-avatar-preview">
              <img
                v-if="!avatarImageFailed && (avatarPreview || profileForm.avatarUrl)"
                :src="avatarPreview || profileForm.avatarUrl"
                alt=""
                @error="handleAvatarImageError"
              />
              <span v-else>{{ profileInitial }}</span>
            </div>
            <div class="avatar-editor-actions">
              <input
                ref="avatarInput"
                class="visually-hidden"
                type="file"
                aria-label="选择头像图片"
                accept="image/png,image/jpeg"
                :disabled="avatarUploading"
                @change="uploadAvatar"
              />
              <button class="outline-button" type="button" :disabled="avatarUploading" @click="chooseAvatar">
                {{ avatarUploading ? '上传中…' : '选择本地头像' }}
              </button>
              <span>支持 JPEG / PNG，最大 5 MB</span>
              <p v-if="avatarError" class="form-error" role="alert">{{ avatarError }}</p>
              <p v-if="avatarNotice" class="form-success" role="status">{{ avatarNotice }}</p>
            </div>
          </div>
          <form id="profile-form" class="profile-form" @submit.prevent="saveProfile">
            <label class="form-field">
              <span>昵称</span>
              <input v-model="profileForm.displayName" maxlength="80" required />
            </label>
            <label class="form-field">
              <span>头像 URL</span>
              <input v-model="profileForm.avatarUrl" maxlength="500" type="text" placeholder="https://… 或上传本地图片" />
            </label>
            <label class="form-field">
              <span>个人简介</span>
              <textarea v-model="profileForm.bio" maxlength="300" rows="4" placeholder="介绍一下自己…" />
            </label>
          </form>
          <button class="password-toggle" type="button" @click="togglePasswordForm">
            {{ passwordExpanded ? '取消修改密码' : '修改密码' }}
          </button>
          <form v-if="passwordExpanded" class="profile-form password-form" @submit.prevent="savePassword">
            <label class="form-field">
              <span>当前密码</span>
              <input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" required />
            </label>
            <label class="form-field">
              <span>新密码</span>
              <input
                v-model="passwordForm.newPassword"
                type="password"
                autocomplete="new-password"
                minlength="8"
                maxlength="72"
                required
              />
            </label>
            <label class="form-field">
              <span>确认新密码</span>
              <input
                v-model="passwordForm.confirmNewPassword"
                type="password"
                autocomplete="new-password"
                maxlength="72"
                required
              />
            </label>
            <p v-if="passwordError" class="form-error" role="alert">{{ passwordError }}</p>
            <p v-if="passwordNotice" class="form-success" role="status">{{ passwordNotice }}</p>
            <button class="primary-button" type="submit" :disabled="passwordSaving">
              {{ passwordSaving ? '更新中…' : '更新密码' }}
            </button>
          </form>
          <p v-if="profileError" class="form-error" role="alert">{{ profileError }}</p>
          <p v-if="profileNotice" class="form-success" role="status">{{ profileNotice }}</p>
          <button class="primary-button" type="submit" form="profile-form" :disabled="profileSaving">
            {{ profileSaving ? '保存中…' : '保存资料' }}
          </button>
        </div>
      </section>
    </div>

    <section v-else class="profile-activity">
      <div v-if="activityLoading" class="state-card">
        <span class="loading-mark"></span> 正在整理你的内容…
      </div>
      <div v-else-if="activityError" class="state-card error-state">
        <strong>内容暂时无法加载</strong>
        <span>{{ activityError }}</span>
        <button class="outline-button" type="button" @click="loadActivity">再试一次</button>
      </div>
      <div v-else-if="activeTab === 'comments' && commentItems.length" class="profile-activity-list">
        <RouterLink
          v-for="comment in commentItems"
          :key="comment.id"
          class="profile-activity-card"
          :to="{ name: 'article', params: { id: comment.articleId } }"
        >
          <div class="profile-activity-top">
            <span>评论于</span>
            <time>{{ new Date(comment.createdAt).toLocaleString('zh-CN') }}</time>
          </div>
          <strong>{{ comment.articleTitle }}</strong>
          <p>{{ comment.body }}</p>
          <span class="profile-activity-arrow" aria-hidden="true">↗</span>
        </RouterLink>
      </div>
      <div v-else-if="activeTab !== 'comments' && articleItems.length" class="profile-activity-list">
        <RouterLink
          v-for="article in articleItems"
          :key="article.id"
          class="profile-activity-card"
          :to="activeTab === 'articles' && article.status === 'DRAFT'
            ? { name: 'write', params: { id: article.id } }
            : { name: 'article', params: { id: article.id } }"
        >
          <div class="profile-activity-top">
            <span>{{ article.categoryName || '社区文章' }}</span>
            <span v-if="activeTab === 'articles'" class="profile-status">{{ article.status === 'DRAFT' ? '草稿' : '已发布' }}</span>
            <time v-if="article.publishedAt">{{ new Date(article.publishedAt).toLocaleDateString('zh-CN') }}</time>
          </div>
          <strong>{{ article.title || '未命名草稿' }}</strong>
          <p>{{ article.summary || '暂无摘要' }}</p>
          <span class="profile-activity-arrow" aria-hidden="true">↗</span>
        </RouterLink>
      </div>
      <div v-else class="state-card empty-state">
        <span class="empty-icon">✳</span>
        <strong>这里还没有内容</strong>
        <span v-if="activeTab === 'articles'">写下你的第一篇文章吧。</span>
        <span v-else-if="activeTab === 'likes'">喜欢的文章会出现在这里。</span>
        <span v-else-if="activeTab === 'bookmarks'">收藏的文章会出现在这里。</span>
        <span v-else>你发布的评论会出现在这里。</span>
        <RouterLink v-if="activeTab === 'articles'" class="outline-button" :to="{ name: 'write' }">
          开始写作
        </RouterLink>
      </div>

      <div v-if="!activityLoading && !activityError && pageCount > 1" class="pagination">
        <button :disabled="page <= 1" @click="changePage(page - 1)">← 上一页</button>
        <span>{{ page }} / {{ pageCount }}</span>
        <button :disabled="page >= pageCount" @click="changePage(page + 1)">下一页 →</button>
      </div>
    </section>
  </main>
</template>
