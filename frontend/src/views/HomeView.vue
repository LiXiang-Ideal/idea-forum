<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, type RouteLocationRaw } from 'vue-router'
import { getArticles, getCategories } from '../api/community'
import { ApiError } from '../api/http'
import type { Article, Category } from '../types'

const route = useRoute()
const heroSlides: {
  eyebrow: string
  titleFirst: string
  titleHighlight: string
  description: string
  action: string
  target: RouteLocationRaw
  art: 'note' | 'topics'
}[] = [
  {
    eyebrow: 'IDEAFORUM · 分享与记录',
    titleFirst: '把灵感写下来，',
    titleHighlight: '让经验有回响。',
    description: '分享你正在解决的问题，也发现别人走过的路。',
    action: '开始写作',
    target: { name: 'write' },
    art: 'note',
  },
  {
    eyebrow: 'IDEAFORUM · 发现与交流',
    titleFirst: '每个技术问题，',
    titleHighlight: '都能找到新思路。',
    description: '从真实实践出发，看看开发者正在讨论什么。',
    action: '探索技术话题',
    target: { name: 'home', hash: '#topics' },
    art: 'topics',
  },
]
const articles = ref<Article[]>([])
const categories = ref<Category[]>([])
const categoriesError = ref('')
const activeCategory = ref<number | null>(null)
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const error = ref('')
const activeSlide = ref(0)
const pointerInsideCarousel = ref(false)
const focusInsideCarousel = ref(false)
const prefersReducedMotion = ref(false)
const carouselPaused = computed(() => pointerInsideCarousel.value || focusInsideCarousel.value)
const carouselElement = ref<HTMLElement | null>(null)
let carouselTimer: number | undefined
let motionPreference: MediaQueryList | undefined
const keyword = computed(() => typeof route.query.keyword === 'string' ? route.query.keyword : '')
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / 12)))

function clearCarouselTimer() {
  if (carouselTimer !== undefined) {
    window.clearInterval(carouselTimer)
    carouselTimer = undefined
  }
}

function updateCarouselTimer() {
  clearCarouselTimer()
  if (carouselPaused.value || prefersReducedMotion.value) return
  carouselTimer = window.setInterval(() => {
    activeSlide.value = (activeSlide.value + 1) % heroSlides.length
  }, 2000)
}

function showSlide(index: number) {
  activeSlide.value = index
}

function handleCarouselFocusOut(event: FocusEvent) {
  const nextTarget = event.relatedTarget
  if (nextTarget instanceof Node && carouselElement.value?.contains(nextTarget)) return
  focusInsideCarousel.value = false
}

function updateMotionPreference() {
  prefersReducedMotion.value = motionPreference?.matches ?? false
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await getArticles({
      keyword: keyword.value,
      categoryId: activeCategory.value ?? undefined,
      page: page.value,
      size: 12,
    })
    articles.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '内容加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number | null) {
  activeCategory.value = id
  page.value = 1
  void load()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)')
  motionPreference.addEventListener('change', updateMotionPreference)
  updateMotionPreference()
  updateCarouselTimer()
  try {
    categories.value = await getCategories()
  } catch (cause) {
    categories.value = []
    categoriesError.value = cause instanceof ApiError ? cause.message : '分类暂时不可用。'
  }
  await load()
})

watch([carouselPaused, prefersReducedMotion, activeSlide], updateCarouselTimer)
onBeforeUnmount(() => {
  clearCarouselTimer()
  motionPreference?.removeEventListener('change', updateMotionPreference)
})

watch(() => route.query.keyword, () => {
  page.value = 1
  void load()
})
</script>

<template>
  <main class="page-main">
    <section
      ref="carouselElement"
      class="hero-panel hero-carousel"
      role="region"
      aria-roledescription="轮播"
      aria-label="IdeaForum 首页推荐"
      @pointerenter="pointerInsideCarousel = true"
      @pointerleave="pointerInsideCarousel = false"
      @focusin="focusInsideCarousel = true"
      @focusout="handleCarouselFocusOut"
    >
      <article
        v-for="(slide, index) in heroSlides"
        :key="slide.art"
        class="hero-slide"
        :class="{ active: activeSlide === index }"
        :aria-hidden="activeSlide !== index"
      >
        <div class="hero-copy">
          <p class="eyebrow"><span class="live-dot"></span>{{ slide.eyebrow }}</p>
          <h1>{{ slide.titleFirst }}<br /><span>{{ slide.titleHighlight }}</span></h1>
          <p class="hero-description">{{ slide.description }}</p>
          <RouterLink class="hero-cta" :to="slide.target">
            {{ slide.action }} <span aria-hidden="true">↗</span>
          </RouterLink>
        </div>
        <div class="hero-art" aria-hidden="true">
          <template v-if="slide.art === 'note'">
            <div class="orbit orbit-one"></div>
            <div class="orbit orbit-two"></div>
            <div class="art-card">
              <span class="art-label">TODAY'S NOTE</span>
              <span class="art-code"><i>const</i> idea = <b>'share'</b></span>
              <span class="art-code">idea.<em>publish</em>()</span>
              <span class="art-line"></span>
              <span class="art-caption">一段经验，也能点亮另一个人。</span>
            </div>
            <span class="art-star star-a">✳</span>
            <span class="art-star star-b">✳</span>
          </template>
          <template v-else>
            <div class="topics-orbit topics-orbit-one"></div>
            <div class="topics-orbit topics-orbit-two"></div>
            <div class="topics-art-card">
              <span class="art-label">TRENDING TOPICS</span>
              <span># 前端工程化</span>
              <span># AI 应用实践</span>
              <span># 数据库优化</span>
            </div>
            <span class="art-star star-a">✳</span>
            <span class="art-star star-b">✳</span>
          </template>
        </div>
      </article>
      <div class="hero-controls" aria-label="轮播控制">
        <span class="hero-index"><span>0{{ activeSlide + 1 }}</span> / 02</span>
        <div class="hero-indicators">
          <button
            v-for="(slide, index) in heroSlides"
            :key="slide.art"
            type="button"
            :aria-label="`显示第 ${index + 1} 张：${slide.action}`"
            :aria-pressed="activeSlide === index"
            :class="{ selected: activeSlide === index }"
            @click="showSlide(index)"
          ></button>
        </div>
      </div>
    </section>

    <section id="topics" class="discovery-layout">
      <div class="feed-column">
        <div class="section-heading">
          <div>
            <p class="eyebrow">THE COMMUNITY</p>
            <h2>{{ keyword ? `搜索「${keyword}」` : '今天读点什么' }}</h2>
          </div>
          <span class="result-count">{{ total }} 篇内容</span>
        </div>

        <div class="feed-tabs" role="tablist" aria-label="文章筛选">
          <button :class="{ selected: activeCategory === null }" @click="selectCategory(null)">最新</button>
          <button
            v-for="category in categories"
            :key="category.id"
            :class="{ selected: activeCategory === category.id }"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>

        <div v-if="loading" class="state-card">
          <span class="loading-mark"></span> 正在整理新鲜内容…
        </div>
        <div v-else-if="error" class="state-card error-state">
          <strong>暂时没能连接社区</strong>
          <span>{{ error }}</span>
          <button class="outline-button" @click="load">再试一次</button>
        </div>
        <div v-else-if="articles.length === 0" class="state-card empty-state">
          <span class="empty-icon">✳</span>
          <strong>{{ keyword ? '没有找到相关内容' : '这里还没有文章' }}</strong>
          <span>成为第一个分享经验的人吧。</span>
          <RouterLink class="outline-button" :to="{ name: 'write' }">写下第一篇</RouterLink>
        </div>

        <div v-else class="article-list">
          <RouterLink
            v-for="(article, index) in articles"
            :key="article.id"
            class="article-card"
            :to="{ name: 'article', params: { id: article.id } }"
          >
            <div class="article-card-top">
              <span class="article-number">{{ String((page - 1) * 12 + index + 1).padStart(2, '0') }}</span>
              <span v-if="article.categoryName" class="category-pill">{{ article.categoryName }}</span>
              <time v-if="article.publishedAt">{{ new Date(article.publishedAt).toLocaleDateString('zh-CN') }}</time>
            </div>
            <span class="article-title">
              {{ article.title }}
            </span>
            <p class="article-summary">{{ article.summary || article.body }}</p>
            <div class="article-meta">
              <span class="author-avatar">{{ (article.authorName || '开').slice(0, 1) }}</span>
              <span class="author-name">{{ article.authorName }}</span>
              <span class="meta-separator"></span>
              <span>{{ article.viewCount }} 阅读</span>
              <span>♡ {{ article.likeCount }}</span>
              <span>▤ {{ article.commentCount }}</span>
            </div>
            <span class="article-arrow" aria-hidden="true">↗</span>
          </RouterLink>
        </div>

        <div v-if="!loading && !error && pageCount > 1" class="pagination">
          <button :disabled="page <= 1" @click="changePage(page - 1)">← 上一页</button>
          <span>{{ page }} / {{ pageCount }}</span>
          <button :disabled="page >= pageCount" @click="changePage(page + 1)">下一页 →</button>
        </div>
      </div>

      <aside class="side-column">
        <section class="side-card note-card">
          <p class="eyebrow">A SMALL REMINDER</p>
          <h3>好内容，<br />从一个真问题开始。</h3>
          <p>不必写得完美。记录你如何思考、尝试和解决，就已经很有价值。</p>
          <RouterLink :to="{ name: 'write' }">写下你的经验 <span>↗</span></RouterLink>
          <span class="note-decoration" aria-hidden="true">✳</span>
        </section>

        <section class="side-card topics-card">
          <div class="side-heading">
            <div><p class="eyebrow">EXPLORE</p><h3>社区话题</h3></div>
            <span aria-hidden="true">↗</span>
          </div>
          <button v-for="category in categories.slice(0, 5)" :key="category.id"
            class="topic-row" @click="selectCategory(category.id)">
            <span class="topic-dot"></span>
            <span>{{ category.name }}</span>
            <span class="topic-arrow">↗</span>
          </button>
          <p v-if="categories.length === 0" class="topics-empty">话题正在准备中。</p>
          <p v-if="categoriesError" class="inline-error">{{ categoriesError }}</p>
        </section>

        <p class="side-footnote">做真实的记录，分享真诚的经验。<br />让开发这件事，彼此陪伴。</p>
      </aside>
    </section>
  </main>
</template>
