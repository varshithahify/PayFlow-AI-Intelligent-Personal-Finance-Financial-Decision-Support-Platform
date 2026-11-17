import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configureStore } from '@reduxjs/toolkit'

const {
  mockGetOverview,
  mockGetProcessed,
  mockGetFailed,
  mockGetProcessedById,
  mockGetFailedById,
} = vi.hoisted(() => ({
  mockGetOverview: vi.fn(),
  mockGetProcessed: vi.fn(),
  mockGetFailed: vi.fn(),
  mockGetProcessedById: vi.fn(),
  mockGetFailedById: vi.fn(),
}))

vi.mock('../../services/api', () => ({
  eventMonitoringApi: {
    getOverview: mockGetOverview,
    getProcessed: mockGetProcessed,
    getFailed: mockGetFailed,
    getProcessedById: mockGetProcessedById,
    getFailedById: mockGetFailedById,
  },
}))

import eventMonitoringReducer, {
  fetchEventOverview,
  fetchProcessedEvents,
  fetchFailedEvents,
  fetchProcessedEventById,
  fetchFailedEventById,
  setProcessedEventPage,
  setFailedEventPage,
  clearSelectedEvent,
  resetEventMonitoringState,
} from './eventMonitoringSlice'

const createTestStore = () =>
  configureStore({
    reducer: {
      eventMonitoring: eventMonitoringReducer,
    },
  })

describe('eventMonitoringSlice', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // Initial state

  it('should initialize with the correct state', () => {
    const store = createTestStore()
    const state = store.getState().eventMonitoring

    expect(state.overview).toEqual({
      totalEvents: 0,
      totalProcessedEvents: 0,
      totalFailedEvents: 0,
    })

    expect(state.processedEvents).toEqual([])
    expect(state.failedEvents).toEqual([])
    expect(state.selectedEvent).toBeNull()
    expect(state.selectedEventCategory).toBeNull()

    expect(state.processedPage).toBe(0)
    expect(state.failedPage).toBe(0)

    expect(state.overviewLoading).toBe(false)
    expect(state.processedLoading).toBe(false)
    expect(state.failedLoading).toBe(false)
    expect(state.detailsLoading).toBe(false)
  })

  // Pagination reducers

  it('should update the processed events page', () => {
    const store = createTestStore()

    store.dispatch(setProcessedEventPage(3))

    expect(
      store.getState().eventMonitoring.processedPage,
    ).toBe(3)
  })

  it('should update the failed events page', () => {
    const store = createTestStore()

    store.dispatch(setFailedEventPage(4))

    expect(
      store.getState().eventMonitoring.failedPage,
    ).toBe(4)
  })

  // Event overview

  it('should fetch event overview successfully', async () => {
    const overview = {
      totalEvents: 150,
      totalProcessedEvents: 120,
      totalFailedEvents: 30,
    }

    mockGetOverview.mockResolvedValue(overview)

    const store = createTestStore()

    await store.dispatch(fetchEventOverview('test-token'))

    expect(mockGetOverview).toHaveBeenCalledWith('test-token')

    const state = store.getState().eventMonitoring

    expect(state.overview).toEqual(overview)
    expect(state.overviewLoading).toBe(false)
    expect(state.overviewError).toBeNull()
  })

  it('should handle event overview fetching failure', async () => {
    mockGetOverview.mockRejectedValue(
      new Error('Overview unavailable'),
    )

    const store = createTestStore()

    await store.dispatch(fetchEventOverview('test-token'))

    const state = store.getState().eventMonitoring

    expect(state.overviewLoading).toBe(false)
    expect(state.overviewError).toBe('Overview unavailable')
  })

  // Processed events

  it('should fetch processed events successfully', async () => {
    const response = {
      content: [
        {
          eventId: 'event-001',
          eventType: 'TRANSACTION_CREATED',
          orgId: 1,
          processedAt: '2026-11-17T10:30:00',
        },
      ],
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }

    mockGetProcessed.mockResolvedValue(response)

    const store = createTestStore()

    const filters = {
      page: 0,
      size: 20,
      eventType: 'TRANSACTION_CREATED',
      fromDate: '2026-11-01T00:00:00',
      toDate: '2026-11-17T23:59:59',
      token: 'test-token',
    }

    await store.dispatch(fetchProcessedEvents(filters))

    expect(mockGetProcessed).toHaveBeenCalledWith(
      {
        page: 0,
        size: 20,
        eventType: 'TRANSACTION_CREATED',
        fromDate: '2026-11-01T00:00:00',
        toDate: '2026-11-17T23:59:59',
      },
      'test-token',
    )

    const state = store.getState().eventMonitoring

    expect(state.processedEvents).toEqual(response.content)
    expect(state.processedPage).toBe(0)
    expect(state.processedSize).toBe(20)
    expect(state.processedTotalElements).toBe(1)
    expect(state.processedTotalPages).toBe(1)
    expect(state.processedLoading).toBe(false)
    expect(state.processedError).toBeNull()
  })

  it('should handle processed events fetching failure', async () => {
    mockGetProcessed.mockRejectedValue(
      new Error('Unable to load processed events'),
    )

    const store = createTestStore()

    await store.dispatch(
      fetchProcessedEvents({ token: 'test-token' }),
    )

    const state = store.getState().eventMonitoring

    expect(state.processedLoading).toBe(false)
    expect(state.processedError).toBe(
      'Unable to load processed events',
    )
  })

  // Failed events

  it('should fetch failed events successfully', async () => {
    const response = {
      content: [
        {
          eventId: 'event-002',
          transactionId: 101,
          eventType: 'TRANSACTION_FAILED',
          failureReason: 'Payment gateway timeout',
          recordedAt: '2026-11-17T11:00:00',
        },
      ],
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }

    mockGetFailed.mockResolvedValue(response)

    const store = createTestStore()

    const filters = {
      page: 0,
      size: 20,
      eventType: 'TRANSACTION_FAILED',
      fromDate: '2026-11-01T00:00:00',
      toDate: '2026-11-17T23:59:59',
      token: 'test-token',
    }

    await store.dispatch(fetchFailedEvents(filters))

    expect(mockGetFailed).toHaveBeenCalledWith(
      {
        page: 0,
        size: 20,
        eventType: 'TRANSACTION_FAILED',
        fromDate: '2026-11-01T00:00:00',
        toDate: '2026-11-17T23:59:59',
      },
      'test-token',
    )

    const state = store.getState().eventMonitoring

    expect(state.failedEvents).toEqual(response.content)
    expect(state.failedPage).toBe(0)
    expect(state.failedSize).toBe(20)
    expect(state.failedTotalElements).toBe(1)
    expect(state.failedTotalPages).toBe(1)
    expect(state.failedLoading).toBe(false)
    expect(state.failedError).toBeNull()
  })

  it('should handle failed events fetching failure', async () => {
    mockGetFailed.mockRejectedValue(
      new Error('Unable to load failed events'),
    )

    const store = createTestStore()

    await store.dispatch(
      fetchFailedEvents({ token: 'test-token' }),
    )

    const state = store.getState().eventMonitoring

    expect(state.failedLoading).toBe(false)
    expect(state.failedError).toBe(
      'Unable to load failed events',
    )
  })

  // Processed event details

  it('should fetch processed event details successfully', async () => {
    const event = {
      eventId: 'event-001',
      eventType: 'TRANSACTION_CREATED',
      orgId: 1,
      processedAt: '2026-11-17T10:30:00',
    }

    mockGetProcessedById.mockResolvedValue(event)

    const store = createTestStore()

    await store.dispatch(
      fetchProcessedEventById({
        eventId: 'event-001',
        token: 'test-token',
      }),
    )

    expect(mockGetProcessedById).toHaveBeenCalledWith(
      'event-001',
      'test-token',
    )

    const state = store.getState().eventMonitoring

    expect(state.selectedEvent).toEqual(event)
    expect(state.selectedEventCategory).toBe('PROCESSED')
    expect(state.detailsLoading).toBe(false)
    expect(state.detailsError).toBeNull()
  })

  it('should handle processed event details failure', async () => {
    mockGetProcessedById.mockRejectedValue(
      new Error('Processed event not found'),
    )

    const store = createTestStore()

    await store.dispatch(
      fetchProcessedEventById({
        eventId: 'event-999',
        token: 'test-token',
      }),
    )

    const state = store.getState().eventMonitoring

    expect(state.detailsLoading).toBe(false)
    expect(state.detailsError).toBe(
      'Processed event not found',
    )
  })

  // Failed event details

  it('should fetch failed event details successfully', async () => {
    const event = {
      eventId: 'event-002',
      transactionId: 101,
      eventType: 'TRANSACTION_FAILED',
      failureReason: 'Payment gateway timeout',
      recordedAt: '2026-11-17T11:00:00',
    }

    mockGetFailedById.mockResolvedValue(event)

    const store = createTestStore()

    await store.dispatch(
      fetchFailedEventById({
        eventId: 'event-002',
        token: 'test-token',
      }),
    )

    expect(mockGetFailedById).toHaveBeenCalledWith(
      'event-002',
      'test-token',
    )

    const state = store.getState().eventMonitoring

    expect(state.selectedEvent).toEqual(event)
    expect(state.selectedEventCategory).toBe('FAILED')
    expect(state.detailsLoading).toBe(false)
    expect(state.detailsError).toBeNull()
  })

  it('should handle failed event details failure', async () => {
    mockGetFailedById.mockRejectedValue(
      new Error('Failed event not found'),
    )

    const store = createTestStore()

    await store.dispatch(
      fetchFailedEventById({
        eventId: 'event-999',
        token: 'test-token',
      }),
    )

    const state = store.getState().eventMonitoring

    expect(state.detailsLoading).toBe(false)
    expect(state.detailsError).toBe(
      'Failed event not found',
    )
  })

  // Clear selected event

  it('should clear selected event details', () => {
    const store = createTestStore()

    store.dispatch({
      type: fetchProcessedEventById.fulfilled.type,
      payload: {
        event: {
          eventId: 'event-001',
          eventType: 'TRANSACTION_CREATED',
        },
        eventCategory: 'PROCESSED',
      },
    })

    store.dispatch(clearSelectedEvent())

    const state = store.getState().eventMonitoring

    expect(state.selectedEvent).toBeNull()
    expect(state.selectedEventCategory).toBeNull()
    expect(state.detailsError).toBeNull()
  })

  // Reset state

  it('should reset event monitoring state', () => {
    const store = createTestStore()

    store.dispatch(setProcessedEventPage(5))
    store.dispatch(setFailedEventPage(3))

    store.dispatch(resetEventMonitoringState())

    const state = store.getState().eventMonitoring

    expect(state.processedPage).toBe(0)
    expect(state.failedPage).toBe(0)
    expect(state.processedEvents).toEqual([])
    expect(state.failedEvents).toEqual([])
    expect(state.selectedEvent).toBeNull()
    expect(state.overviewLoading).toBe(false)
  })
})