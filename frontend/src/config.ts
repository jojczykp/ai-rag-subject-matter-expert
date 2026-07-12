const DEFAULT_BACKEND_API_BASE_URL = 'http://localhost:8080'

export const backendApiBaseUrl =
  import.meta.env.VITE_BACKEND_API_BASE_URL ?? DEFAULT_BACKEND_API_BASE_URL

export function apiUrl(path: string): string {
  return new URL(path, backendApiBaseUrl).toString()
}
