import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import { useAuthStore } from './stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    {
      path: '/articles/:id',
      name: 'article',
      component: () => import('./views/ArticleView.vue'),
      props: true,
    },
    {
      path: '/write/:id?',
      name: 'write',
      component: () => import('./views/WriteView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('./views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('./views/AuthView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: (to) => to.hash
    ? { el: to.hash, behavior: 'smooth' }
    : { top: 0 },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.restoreSession()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})
