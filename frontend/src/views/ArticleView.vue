<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  createComment,
  getArticle,
  getArticleInteraction,
  getComments,
  setArticleBookmark,
  setArticleLike,
} from '../api/community'
import { ApiError } from '../api/http'
import type { Article, ArticleInteraction, Comment } from '../types'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const article = ref<Article | null>(null)
const interaction = ref<ArticleInteraction>({
  liked: false,
  bookmarked: false,
  likeCount: 0,
  bookmarkCount: 0,
})
const comments = ref<Comment[]>([])
const commentBody = ref('')
const loading = ref(true)
const commenting = ref(false)
const error = ref('')
const commentError = ref('')

function openLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

onMounted(async () => {
  try {
    article.value = await getArticle(Number(route.params.id))
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '文章加载失败。'
  } finally {
    loading.value = false
  }
  if (!article.value) return
  try {
    const [state, articleComments] = await Promise.all([
      getArticleInteraction(Number(route.params.id)),
      getComments(Number(route.params.id)),
    ])
    interaction.value = state
    comments.value = articleComments
  } catch (cause) {
    commentError.value = cause instanceof ApiError ? cause.message : '互动内容加载失败。'
  }
})

async function toggleLike() {
  if (!auth.isAuthenticated) {
    openLogin()
    return
  }
  try {
    interaction.value = await setArticleLike(Number(route.params.id), !interaction.value.liked)
  } catch (cause) {
    commentError.value = cause instanceof ApiError ? cause.message : '点赞操作失败，请重试。'
  }
}

async function toggleBookmark() {
  if (!auth.isAuthenticated) {
    openLogin()
    return
  }
  try {
    interaction.value = await setArticleBookmark(Number(route.params.id), !interaction.value.bookmarked)
  } catch (cause) {
    commentError.value = cause instanceof ApiError ? cause.message : '收藏操作失败，请重试。'
  }
}

async function submitComment() {
  if (!auth.isAuthenticated) {
    openLogin()
    return
  }
  if (!commentBody.value.trim() || commenting.value) return
  commenting.value = true
  commentError.value = ''
  try {
    const comment = await createComment(Number(route.params.id), commentBody.value)
    comments.value.push(comment)
    commentBody.value = ''
    if (article.value) article.value.commentCount += 1
  } catch (cause) {
    commentError.value = cause instanceof ApiError ? cause.message : '评论发送失败，请重试。'
  } finally {
    commenting.value = false
  }
}
</script>

<template>
  <main class="article-page">
    <RouterLink class="back-link" :to="{ name: 'home' }">← 返回发现</RouterLink>
    <div v-if="loading" class="state-card"><span class="loading-mark"></span> 正在打开文章…</div>
    <section v-else-if="error" class="state-card error-state">
      <strong>文章暂时无法打开</strong>
      <span>{{ error }}</span>
    </section>
    <article v-else-if="article" class="article-detail">
      <div class="detail-kicker">
        <span>{{ article.categoryName || '社区文章' }}</span>
        <time v-if="article.publishedAt">{{ new Date(article.publishedAt).toLocaleDateString('zh-CN') }}</time>
        <span v-else>草稿</span>
      </div>
      <h1>{{ article.title }}</h1>
      <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>
      <div class="detail-author">
        <RouterLink class="detail-author-link" :to="{ name: 'author', params: { username: article.authorUsername } }">
          <span class="author-avatar large-avatar">{{ (article.authorName || '开').slice(0, 1) }}</span>
          <span class="detail-author-copy">
            <strong>{{ article.authorName }}</strong>
            <span>@{{ article.authorUsername }} · 分享了一段实践经验</span>
          </span>
        </RouterLink>
        <div class="detail-stats"><span>{{ article.viewCount }} 阅读</span><span>♡ {{ article.likeCount }}</span></div>
      </div>
      <div class="detail-rule"></div>
      <div class="article-body">{{ article.body }}</div>
      <div class="article-actions">
        <button :class="{ engaged: interaction.liked }" @click="toggleLike">
          {{ interaction.liked ? '♥ 已赞' : '♡ 点赞' }} <span>{{ interaction.likeCount }}</span>
        </button>
        <button :class="{ engaged: interaction.bookmarked }" @click="toggleBookmark">
          {{ interaction.bookmarked ? '★ 已收藏' : '☆ 收藏' }}
        </button>
        <span>{{ interaction.bookmarkCount }} 人收藏</span>
      </div>
      <div class="article-end"><span>✳</span><p>感谢阅读，欢迎把你的实践也分享出来。</p></div>
      <section class="comments-section">
        <div class="comments-heading">
          <div><p class="eyebrow">JOIN THE CONVERSATION</p><h2>评论 <span>{{ comments.length }}</span></h2></div>
        </div>
        <form class="comment-form" @submit.prevent="submitComment">
          <span class="author-avatar">{{ (auth.profile?.displayName || '我').slice(0, 1) }}</span>
          <textarea v-model="commentBody" maxlength="3000" rows="3"
            :placeholder="auth.isAuthenticated ? '分享你的想法…' : '登录后参与讨论'" />
          <button class="primary-button" type="submit" :disabled="commenting || !commentBody.trim()">
            {{ commenting ? '发送中…' : '发送' }}
          </button>
        </form>
        <p v-if="commentError" class="form-error" role="alert">{{ commentError }}</p>
        <div v-if="comments.length" class="comment-list">
          <article v-for="comment in comments" :key="comment.id" class="comment-item">
            <span class="author-avatar">{{ comment.authorName.slice(0, 1) }}</span>
            <div class="comment-content">
              <div><strong>{{ comment.authorName }}</strong><time>{{ new Date(comment.createdAt).toLocaleString('zh-CN') }}</time></div>
              <p>{{ comment.body }}</p>
            </div>
          </article>
        </div>
        <p v-else class="comments-empty">还没有评论，来开启一段友好的讨论吧。</p>
      </section>
    </article>
  </main>
</template>
