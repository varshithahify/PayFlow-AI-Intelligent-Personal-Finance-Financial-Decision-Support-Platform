import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  apiRequest,
  setUnauthorizedHandler,
  authApi,
  transactionApi,
  reconciliationApi,
  analyticsApi,
} from './api'

describe('api service', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    sessionStorage.clear()
    setUnauthorizedHandler(null)
    globalThis.fetch = vi.fn()
  })

  it('should make a GET request with default headers', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: {
        get: () => 'application/json',
      },
      json: async () => ({ message: 'Success' }),
    })

    const result = await apiRequest('/test')

    expect(fetch).toHaveBeenCalledWith(
      'http://127.0.0.1:8081/test',
      expect.objectContaining({
        method: 'GET',
        headers: {
          Accept: 'application/json',
        },
        body: undefined,
      }),
    )

    expect(result).toEqual({ message: 'Success' })
  })

  it('should attach the supplied JWT token', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({}),
    })

    await apiRequest('/protected', {
      token: 'test-token',
    })

    expect(fetch.mock.calls[0][1].headers.Authorization)
      .toBe('Bearer test-token')
  })

  it('should retrieve JWT automatically from session storage', async () => {
    sessionStorage.setItem('accessToken', 'stored-token')

    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({}),
    })

    await apiRequest('/protected')

    expect(fetch.mock.calls[0][1].headers.Authorization)
      .toBe('Bearer stored-token')
  })

  it('should send a JSON request body', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ success: true }),
    })

    const requestBody = {
      email: 'user@example.com',
      password: 'password123',
    }

    await apiRequest('/auth/login', {
      method: 'POST',
      body: requestBody,
      requiresAuth: false,
    })

    expect(fetch.mock.calls[0][1].headers['Content-Type'])
      .toBe('application/json')

    expect(fetch.mock.calls[0][1].body)
      .toBe(JSON.stringify(requestBody))
  })

  it('should return null for HTTP 204 responses', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 204,
      headers: { get: () => '' },
    })

    const result = await apiRequest('/test')

    expect(result).toBeNull()
  })

  it('should parse JSON responses', async () => {
    const responseData = { id: 1, status: 'SUCCESS' }

    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => responseData,
    })

    const result = await apiRequest('/transactions')

    expect(result).toEqual(responseData)
  })

  it('should return text responses when content type is not JSON', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'text/plain' },
      text: async () => 'Operation completed',
    })

    const result = await apiRequest('/test')

    expect(result).toBe('Operation completed')
  })

  it('should throw an error for unsuccessful HTTP responses', async () => {
    fetch.mockResolvedValue({
      ok: false,
      status: 500,
      text: async () => 'Internal server error',
    })

    await expect(apiRequest('/test')).rejects.toMatchObject({
      message: 'Internal server error',
      status: 500,
    })
  })

  it('should invoke unauthorized handler when an authenticated request returns 401', async () => {
    const unauthorizedHandler = vi.fn()

    setUnauthorizedHandler(unauthorizedHandler)

    fetch.mockResolvedValue({
      ok: false,
      status: 401,
      text: async () => 'Unauthorized',
    })

    await expect(apiRequest('/protected')).rejects.toThrow(
      'Unauthorized',
    )

    expect(unauthorizedHandler).toHaveBeenCalledOnce()
  })

  it('should not invoke unauthorized handler for public requests', async () => {
    const unauthorizedHandler = vi.fn()

    setUnauthorizedHandler(unauthorizedHandler)

    fetch.mockResolvedValue({
      ok: false,
      status: 401,
      text: async () => 'Unauthorized',
    })

    await expect(
      apiRequest('/auth/login', {
        requiresAuth: false,
      }),
    ).rejects.toThrow('Unauthorized')

    expect(unauthorizedHandler).not.toHaveBeenCalled()
  })

  it('should call the login endpoint without authentication', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ token: 'jwt-token' }),
    })

    await authApi.login({
      email: 'user@example.com',
      password: 'password123',
    })

    expect(fetch.mock.calls[0][0])
      .toBe('http://127.0.0.1:8081/auth/login')

    expect(fetch.mock.calls[0][1].headers.Authorization)
      .toBeUndefined()
  })

  it('should construct transaction pagination query parameters', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => ({ content: [] }),
    })

    await transactionApi.getPage(
      {
        page: 2,
        size: 20,
        search: '  payment test  ',
        status: 'SUCCESS',
        paymentMethod: 'UPI',
        sortBy: 'createdAt',
        direction: 'desc',
      },
      'test-token',
    )

    const requestedUrl = fetch.mock.calls[0][0]

    const url = new URL(requestedUrl)

    expect(url.pathname).toBe('/api/transactions/page')
    expect(url.searchParams.get('page')).toBe('2')
    expect(url.searchParams.get('size')).toBe('20')
    expect(url.searchParams.get('search')).toBe('payment test')
    expect(url.searchParams.get('status')).toBe('SUCCESS')
    expect(url.searchParams.get('paymentMethod')).toBe('UPI')
    expect(url.searchParams.get('sortBy')).toBe('createdAt')
    expect(url.searchParams.get('direction')).toBe('desc')
  })

  it('should call reconciliation and analytics endpoints', async () => {
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: async () => [],
    })

    await reconciliationApi.getByStatus('SUCCESS', 'test-token')
    await analyticsApi.getOverview()
    await analyticsApi.getPaymentTrends()
    await analyticsApi.getPaymentStatus()
    await analyticsApi.getGatewayPerformance()

    const requestedUrls = fetch.mock.calls.map((call) => call[0])

    expect(requestedUrls).toContain(
      'http://127.0.0.1:8081/api/reconciliation/status/SUCCESS',
    )

    expect(requestedUrls).toContain(
      'http://127.0.0.1:8081/api/analytics/overview',
    )

    expect(requestedUrls).toContain(
      'http://127.0.0.1:8081/api/analytics/payment-trends',
    )

    expect(requestedUrls).toContain(
      'http://127.0.0.1:8081/api/analytics/payment-status',
    )

    expect(requestedUrls).toContain(
      'http://127.0.0.1:8081/api/analytics/gateway-performance',
    )
  })
})