import { beforeEach, describe, expect, it, vi } from 'vitest'

describe('authSlice', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetModules()
  })

  it('should initialize with an unauthenticated state', async () => {
    const { default: authReducer } = await import('./authSlice')

    const state = authReducer(undefined, { type: '@@INIT' })

    expect(state).toEqual({
      token: null,
      userEmail: '',
      isAuthenticated: false,
    })
  })

  it('should authenticate the user after successful login', async () => {
    const { default: authReducer, loginSuccess } =
      await import('./authSlice')

    const state = authReducer(
      undefined,
      loginSuccess({
        token: 'test-jwt-token',
        email: 'user@example.com',
      }),
    )

    expect(state.token).toBe('test-jwt-token')
    expect(state.userEmail).toBe('user@example.com')
    expect(state.isAuthenticated).toBe(true)

    expect(sessionStorage.getItem('accessToken'))
      .toBe('test-jwt-token')

    expect(sessionStorage.getItem('userEmail'))
      .toBe('user@example.com')
  })

  it('should clear authentication after logout', async () => {
    const { default: authReducer, loginSuccess, logout } =
      await import('./authSlice')

    const authenticatedState = authReducer(
      undefined,
      loginSuccess({
        token: 'test-jwt-token',
        email: 'user@example.com',
      }),
    )

    const state = authReducer(authenticatedState, logout())

    expect(state.token).toBeNull()
    expect(state.userEmail).toBe('')
    expect(state.isAuthenticated).toBe(false)

    expect(sessionStorage.getItem('accessToken')).toBeNull()
    expect(sessionStorage.getItem('userEmail')).toBeNull()
  })

  it('should restore authentication from session storage', async () => {
    sessionStorage.setItem('accessToken', 'existing-jwt-token')
    sessionStorage.setItem('userEmail', 'user@example.com')

    const { default: authReducer } = await import('./authSlice')

    const state = authReducer(undefined, { type: '@@INIT' })

    expect(state.token).toBe('existing-jwt-token')
    expect(state.userEmail).toBe('user@example.com')
    expect(state.isAuthenticated).toBe(true)
  })
})