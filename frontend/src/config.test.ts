import { afterEach, describe, expect, it, vi } from 'vitest'

describe('frontend API configuration', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
  })

  it('uses the environment-appropriate default backend URL', async () => {
    vi.stubEnv('VITE_BACKEND_API_BASE_URL', undefined)

    const { apiUrl, backendApiBaseUrl } = await import('./config')
    const expectedBaseUrl = import.meta.env.PROD
      ? window.location.origin
      : 'http://localhost:8080'

    expect(backendApiBaseUrl).toBe(expectedBaseUrl)
    expect(apiUrl('/subjects')).toBe(`${expectedBaseUrl}/subjects`)
  })

  it('honors an explicit backend URL', async () => {
    vi.stubEnv('VITE_BACKEND_API_BASE_URL', 'https://api.example.com/base/')

    const { apiUrl, backendApiBaseUrl } = await import('./config')

    expect(backendApiBaseUrl).toBe('https://api.example.com/base/')
    expect(apiUrl('/subjects')).toBe('https://api.example.com/subjects')
  })
})
