import { request } from './http'
import type {
  Article,
  ArticleInteraction,
  AuthResponse,
  Category,
  Comment,
  FollowUser,
  PageResponse,
  ProfileArticle,
  ProfileComment,
  PublicProfile,
  UserProfile,
} from '../types'

export function getCategories() {
  return request<Category[]>('/categories')
}

export function getArticles(params: {
  keyword?: string
  categoryId?: number
  page?: number
  size?: number
}) {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.categoryId) query.set('categoryId', String(params.categoryId))
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 12))
  return request<PageResponse<Article>>(`/articles?${query.toString()}`)
}

export function getArticle(id: number) {
  return request<Article>(`/articles/${id}`)
}

function profilePath(username: string) {
  return `/profiles/${encodeURIComponent(username)}`
}

export function getPublicProfile(username: string) {
  return request<PublicProfile>(profilePath(username))
}

export function getPublicArticles(username: string, page: number, size: number) {
  return request<PageResponse<Article>>(`${profilePath(username)}/articles${activityQuery(page, size)}`)
}

export function getFollowers(username: string, page: number, size: number) {
  return request<PageResponse<FollowUser>>(`${profilePath(username)}/followers${activityQuery(page, size)}`)
}

export function getFollowing(username: string, page: number, size: number) {
  return request<PageResponse<FollowUser>>(`${profilePath(username)}/following${activityQuery(page, size)}`)
}

export function setUserFollow(username: string, following: boolean) {
  return request<PublicProfile>(`${profilePath(username)}/follow`, {
    method: following ? 'PUT' : 'DELETE',
  })
}

export function getFollowFeed(page: number, size: number) {
  return request<PageResponse<Article>>(`/users/me/feed${activityQuery(page, size)}`)
}

export function getArticleInteraction(id: number) {
  return request<ArticleInteraction>(`/articles/${id}/interactions`)
}

export function setArticleLike(id: number, liked: boolean) {
  return request<ArticleInteraction>(`/articles/${id}/like`, { method: liked ? 'PUT' : 'DELETE' })
}

export function setArticleBookmark(id: number, bookmarked: boolean) {
  return request<ArticleInteraction>(`/articles/${id}/bookmark`, {
    method: bookmarked ? 'PUT' : 'DELETE',
  })
}

export function getComments(id: number) {
  return request<Comment[]>(`/articles/${id}/comments`)
}

export function createComment(id: number, body: string) {
  return request<Comment>(`/articles/${id}/comments`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  })
}

export interface ArticleWriteInput {
  title: string
  summary: string
  body: string
  categoryId: number | null
  status: 'DRAFT' | 'PUBLISHED'
}

export function saveArticle(article: ArticleWriteInput) {
  return request<Article>('/articles', {
    method: 'POST',
    body: JSON.stringify(article),
  })
}

export function updateArticle(id: number, article: ArticleWriteInput) {
  return request<Article>(`/articles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(article),
  })
}

export function register(input: {
  username: string
  email: string
  password: string
  displayName: string
}) {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function login(input: { username: string; password: string }) {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function logout() {
  return request<void>('/auth/logout', { method: 'POST' })
}

export function getCurrentProfile() {
  return request<UserProfile>('/users/me')
}

export function updateCurrentProfile(input: {
  displayName: string
  avatarUrl: string
  bio: string
}) {
  return request<UserProfile>('/users/me', {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function uploadCurrentAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<UserProfile>('/users/me/avatar', {
    method: 'POST',
    body: formData,
  })
}

export function changeCurrentPassword(input: {
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}) {
  return request<void>('/users/me/password', {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

function activityQuery(page: number, size: number) {
  return `?page=${page}&size=${size}`
}

export function getMyArticles(page: number, size: number) {
  return request<PageResponse<ProfileArticle>>(`/users/me/articles${activityQuery(page, size)}`)
}

export function getMyLikes(page: number, size: number) {
  return request<PageResponse<ProfileArticle>>(`/users/me/likes${activityQuery(page, size)}`)
}

export function getMyBookmarks(page: number, size: number) {
  return request<PageResponse<ProfileArticle>>(`/users/me/bookmarks${activityQuery(page, size)}`)
}

export function getMyComments(page: number, size: number) {
  return request<PageResponse<ProfileComment>>(`/users/me/comments${activityQuery(page, size)}`)
}
