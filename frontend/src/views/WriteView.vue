<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, getCategories, saveArticle, updateArticle } from '../api/community'
import { ApiError } from '../api/http'
import type { Category } from '../types'

const route = useRoute()
const router = useRouter()
const categories = ref<Category[]>([])
const title = ref('')
const summary = ref('')
const body = ref('')
const categoryId = ref<number | null>(null)
const submitting = ref(false)
const loadingDraft = ref(false)
const loadedDraftId = ref<number | null>(null)
const draftLoadError = ref('')
const error = ref('')
const notice = ref('')
const hasDraftId = computed(() => route.params.id !== undefined)
const draftId = computed(() => {
  const value = route.params.id
  if (typeof value !== 'string') return null
  const parsed = Number(value)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null
})
let draftRequestId = 0

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '分类加载失败，请重试。'
  }
}

async function loadDraft(id: number) {
  const requestId = ++draftRequestId
  loadedDraftId.value = null
  draftLoadError.value = ''
  loadingDraft.value = true
  try {
    const article = await getArticle(id)
    if (requestId !== draftRequestId) return
    if (article.status !== 'DRAFT') {
      draftLoadError.value = '这篇文章已发布，不能从草稿编辑入口打开。'
      return
    }
    title.value = article.title
    summary.value = article.summary
    body.value = article.body ?? ''
    categoryId.value = article.categoryId ?? null
    loadedDraftId.value = article.id
  } catch (cause) {
    if (requestId === draftRequestId) {
      draftLoadError.value = cause instanceof ApiError ? cause.message : '草稿加载失败，请重试。'
    }
  } finally {
    if (requestId === draftRequestId) loadingDraft.value = false
  }
}

async function publish(status: 'DRAFT' | 'PUBLISHED') {
  if (submitting.value) return
  const existingDraftId = draftId.value
  if (hasDraftId.value && existingDraftId === null) {
    error.value = '草稿编号无效，未创建新文章。'
    return
  }
  submitting.value = true
  error.value = ''
  notice.value = ''
  try {
    const request = {
      title: title.value,
      summary: summary.value,
      body: body.value,
      categoryId: categoryId.value,
      status,
    }
    const article = existingDraftId !== null
      ? await updateArticle(existingDraftId, request)
      : await saveArticle(request)
    if (status === 'DRAFT' && existingDraftId !== null) {
      notice.value = '草稿已更新。'
    } else {
      await router.push({ name: 'article', params: { id: article.id } })
    }
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '保存文章失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

onMounted(() => void loadCategories())
watch(() => route.params.id, (value) => {
  error.value = ''
  notice.value = ''
  if (value === undefined) {
    draftRequestId += 1
    loadingDraft.value = false
    loadedDraftId.value = null
    draftLoadError.value = ''
    title.value = ''
    summary.value = ''
    body.value = ''
    categoryId.value = null
    return
  }
  if (draftId.value === null) {
    draftRequestId += 1
    loadedDraftId.value = null
    loadingDraft.value = false
    draftLoadError.value = '草稿编号无效。'
    loadingDraft.value = false
    return
  }
  void loadDraft(draftId.value)
}, { immediate: true })
</script>

<template>
  <main class="write-page">
    <div class="write-heading">
      <div>
        <p class="eyebrow">{{ draftId ? 'EDIT YOUR DRAFT' : 'YOUR NEXT NOTE' }}</p>
        <h1>{{ draftId ? '继续完善这篇草稿。' : '写下正在发生的事。' }}</h1>
        <p>{{ draftId ? '修改后可以继续保存，或直接发布。' : '分享解决问题的过程，让经验成为彼此的路标。' }}</p>
      </div>
      <span class="write-mark" aria-hidden="true">✳</span>
    </div>

    <div v-if="loadingDraft" class="state-card">
      <span class="loading-mark"></span> 正在载入草稿…
    </div>
    <div v-else-if="hasDraftId && (draftId === null || loadedDraftId !== draftId)" class="state-card error-state">
      <strong>草稿暂时无法打开</strong>
      <span>{{ draftLoadError || '请重新加载草稿。' }}</span>
      <button v-if="draftId !== null" class="outline-button" type="button" @click="loadDraft(draftId)">再试一次</button>
    </div>
    <form v-else class="editor-card" @submit.prevent="publish('PUBLISHED')">
      <label class="field-label" for="article-title">文章标题</label>
      <input id="article-title" v-model="title" class="title-input" maxlength="200"
        placeholder="给这次经验起个名字…" required />

      <div class="editor-options">
        <label class="form-field">
          <span>所属话题</span>
          <select v-model="categoryId">
            <option :value="null">选择一个分类（可选）</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>
        <label class="form-field summary-field">
          <span>一句话摘要</span>
          <input v-model="summary" maxlength="500" placeholder="用一句话介绍这篇文章" />
        </label>
      </div>

      <label class="field-label body-label" for="article-body">正文</label>
      <textarea id="article-body" v-model="body" class="body-input" maxlength="200000"
        placeholder="从遇到的问题开始，记录你的思路、尝试和结果…" required></textarea>
      <div class="editor-hint"><span>正文以原文保存，并保留换行。</span><span>{{ body.length }} / 200000</span></div>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <p v-if="notice" class="form-success" role="status">{{ notice }}</p>
      <div class="editor-actions">
        <span class="save-note">{{ draftId ? '修改会保存到这篇草稿，不会新建文章。' : '内容会保存到你的社区文章中。' }}</span>
        <div>
          <button class="outline-button" type="button" :disabled="submitting" @click="publish('DRAFT')">
            {{ submitting ? '保存中…' : '存为草稿' }}
          </button>
          <button class="primary-button" type="submit" :disabled="submitting">
            {{ submitting ? '正在处理…' : '发布文章 ↗' }}
          </button>
        </div>
      </div>
    </form>
  </main>
</template>
