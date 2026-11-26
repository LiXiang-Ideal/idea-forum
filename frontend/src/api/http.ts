import type { ApiErrorBody, CsrfResponse } from '../types'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS'])
let csrfResponse: CsrfResponse | null = null

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(message: string, status: number, code: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

async function getCsrfResponse(): Promise<CsrfResponse> {
  if (csrfResponse) return csrfResponse

  let response: Response
  try {
    response = await fetch(`${apiBase}/auth/csrf`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
  } catch {
    throw new ApiError('无法连接服务，请确认后端已启动。', 0, 'NETWORK_ERROR')
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null
    throw new ApiError(
      body?.error?.message ?? `请求失败（${response.status}）`,
      response.status,
      body?.error?.code ?? 'REQUEST_FAILED',
    )
  }

  csrfResponse = (await response.json()) as CsrfResponse
  if (!csrfResponse.headerName || !csrfResponse.token) {
    csrfResponse = null
    throw new ApiError('无法获取安全令牌，请刷新页面后重试。', 500, 'INVALID_CSRF_RESPONSE')
  }
  return csrfResponse
}

export function clearCsrfResponse() {
  csrfResponse = null
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const method = (init.method ?? 'GET').toUpperCase()
  if (!safeMethods.has(method)) {
    const csrf = await getCsrfResponse()
    headers.set(csrf.headerName, csrf.token)
  }

  let response: Response
  try {
    response = await fetch(`${apiBase}${path}`, { ...init, headers, credentials: 'include' })
  } catch {
    throw new ApiError('无法连接服务，请确认后端已启动。', 0, 'NETWORK_ERROR')
  }

  const isCredentialSubmission = path === '/auth/login' || path === '/auth/register'
  if (response.status === 401 && !isCredentialSubmission) {
    clearCsrfResponse()
    localStorage.removeItem('community.profile')
    window.dispatchEvent(new Event('community:unauthorized'))
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null
    throw new ApiError(
      body?.error?.message ?? `请求失败（${response.status}）`,
      response.status,
      body?.error?.code ?? 'REQUEST_FAILED',
    )
  }

  if (path === '/auth/logout') clearCsrfResponse()
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
