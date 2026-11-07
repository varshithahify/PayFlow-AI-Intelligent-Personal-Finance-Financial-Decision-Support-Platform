import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configureStore } from '@reduxjs/toolkit'

const {
  mockGetOverview,
  mockGetPaymentTrends,
  mockGetPaymentStatus,
  mockGetGatewayPerformance,
} = vi.hoisted(() => ({
  mockGetOverview: vi.fn(),
  mockGetPaymentTrends: vi.fn(),
  mockGetPaymentStatus: vi.fn(),
  mockGetGatewayPerformance: vi.fn(),
}))

vi.mock('../../services/api', () => ({
  analyticsApi: {
    getOverview: mockGetOverview,
    getPaymentTrends: mockGetPaymentTrends,
    getPaymentStatus: mockGetPaymentStatus,
    getGatewayPerformance: mockGetGatewayPerformance,
  },
}))

import analyticsReducer, {
  clearAnalytics,
  fetchAnalyticsOverview,
  fetchPaymentTrends,
  fetchPaymentStatus,
  fetchGatewayPerformance,
} from './analyticsSlice'

const createTestStore = () =>
  configureStore({
    reducer: {
      analytics: analyticsReducer,
    },
  })

describe('analyticsSlice', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should initialize with the correct state', () => {
    const store = createTestStore()

    expect(store.getState().analytics).toEqual({
      overview: null,
      paymentTrends: [],
      paymentStatus: [],
      gatewayPerformance: [],
      loading: false,
      error: null,
    })
  })

  it('should fetch analytics overview successfully', async () => {
    const overview = {
      totalTransactions: 100,
      totalAmount: 50000,
    }

    mockGetOverview.mockResolvedValue(overview)

    const store = createTestStore()

    await store.dispatch(fetchAnalyticsOverview())

    expect(mockGetOverview).toHaveBeenCalledOnce()
    expect(store.getState().analytics.overview).toEqual(overview)
    expect(store.getState().analytics.loading).toBe(false)
    expect(store.getState().analytics.error).toBeNull()
  })

  it('should handle analytics overview failure', async () => {
    mockGetOverview.mockRejectedValue(
      new Error('Analytics unavailable'),
    )

    const store = createTestStore()

    await store.dispatch(fetchAnalyticsOverview())

    const state = store.getState().analytics

    expect(state.loading).toBe(false)
    expect(state.error).toBe('Analytics unavailable')
  })

  it('should fetch payment trends successfully', async () => {
    const trends = [
      { date: '2026-11-01', amount: 1000 },
      { date: '2026-11-02', amount: 2000 },
    ]

    mockGetPaymentTrends.mockResolvedValue(trends)

    const store = createTestStore()

    await store.dispatch(fetchPaymentTrends())

    expect(mockGetPaymentTrends).toHaveBeenCalledOnce()
    expect(store.getState().analytics.paymentTrends).toEqual(trends)
  })

  it('should handle payment trends failure', async () => {
    mockGetPaymentTrends.mockRejectedValue(
      new Error('Trends unavailable'),
    )

    const store = createTestStore()

    await store.dispatch(fetchPaymentTrends())

    expect(store.getState().analytics.error)
      .toBe('Trends unavailable')
  })

  it('should fetch payment status successfully', async () => {
    const statuses = [
      { status: 'SUCCESS', count: 80 },
      { status: 'FAILED', count: 20 },
    ]

    mockGetPaymentStatus.mockResolvedValue(statuses)

    const store = createTestStore()

    await store.dispatch(fetchPaymentStatus())

    expect(mockGetPaymentStatus).toHaveBeenCalledOnce()
    expect(store.getState().analytics.paymentStatus)
      .toEqual(statuses)
  })

  it('should handle payment status failure', async () => {
    mockGetPaymentStatus.mockRejectedValue(
      new Error('Status unavailable'),
    )

    const store = createTestStore()

    await store.dispatch(fetchPaymentStatus())

    expect(store.getState().analytics.error)
      .toBe('Status unavailable')
  })

  it('should fetch gateway performance successfully', async () => {
    const performance = [
      { gateway: 'RAZORPAY', successRate: 98 },
      { gateway: 'PAYU', successRate: 95 },
    ]

    mockGetGatewayPerformance.mockResolvedValue(performance)

    const store = createTestStore()

    await store.dispatch(fetchGatewayPerformance())

    expect(mockGetGatewayPerformance).toHaveBeenCalledOnce()
    expect(store.getState().analytics.gatewayPerformance)
      .toEqual(performance)
  })

  it('should handle gateway performance failure', async () => {
    mockGetGatewayPerformance.mockRejectedValue(
      new Error('Gateway data unavailable'),
    )

    const store = createTestStore()

    await store.dispatch(fetchGatewayPerformance())

    expect(store.getState().analytics.error)
      .toBe('Gateway data unavailable')
  })

  it('should clear all analytics data', async () => {
    mockGetOverview.mockResolvedValue({ totalTransactions: 100 })
    mockGetPaymentTrends.mockResolvedValue([{ amount: 1000 }])
    mockGetPaymentStatus.mockResolvedValue([{ status: 'SUCCESS' }])
    mockGetGatewayPerformance.mockResolvedValue([{ gateway: 'RAZORPAY' }])

    const store = createTestStore()

    await Promise.all([
      store.dispatch(fetchAnalyticsOverview()),
      store.dispatch(fetchPaymentTrends()),
      store.dispatch(fetchPaymentStatus()),
      store.dispatch(fetchGatewayPerformance()),
    ])

    store.dispatch(clearAnalytics())

    expect(store.getState().analytics).toEqual({
      overview: null,
      paymentTrends: [],
      paymentStatus: [],
      gatewayPerformance: [],
      loading: false,
      error: null,
    })
  })
})
