export interface Category {
  id: number
  name: string
  slug: string
  description: string | null
}

export interface Article {
  id: number
  title: string
  summary: string
  body: string | null
  authorName: string
  authorUsername: string
  categoryName: string | null
  categoryId?: number | null
  status?: 'DRAFT' | 'PUBLISHED'
  viewCount: number
  likeCount: number
  commentCount: number
  publishedAt: string | null
}

export interface ProfileArticle extends Omit<Article, 'body'> {
  status: 'DRAFT' | 'PUBLISHED'
}

export interface ProfileComment {
  id: number
  articleId: number
  articleTitle: string
  body: string
  createdAt: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export interface UserProfile {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
}

export interface PublicProfile extends UserProfile {
  followerCount: number
  followingCount: number
  following: boolean
}

export interface FollowUser {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
}

export interface AuthResponse {
  user: UserProfile
}

export interface CsrfResponse {
  headerName: string
  token: string
}

export interface ArticleInteraction {
  liked: boolean
  bookmarked: boolean
  likeCount: number
  bookmarkCount: number
}

export interface Comment {
  id: number
  authorName: string
  body: string
  createdAt: string
}

export interface ApiErrorBody {
  error?: {
    code?: string
    message?: string
    details?: string[]
  }
}
