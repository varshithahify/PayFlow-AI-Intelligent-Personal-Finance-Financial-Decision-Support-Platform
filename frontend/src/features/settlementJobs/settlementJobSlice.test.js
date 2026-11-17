import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configureStore } from '@reduxjs/toolkit'

const {
  mockGetPage,
  mockCreate,
  mockGetById,
  mockExecute,
  mockGetHistory,
} = vi.hoisted(() => ({
  mockGetPage: vi.fn(),
  mockCreate: vi.fn(),
  mockGetById: vi.fn(),
  mockExecute: vi.fn(),
  mockGetHistory: vi.fn(),
}))

vi.mock('../../services/api', () => ({
  settlementJobApi: {
    getPage: mockGetPage,
    create: mockCreate,
    getById: mockGetById,
    execute: mockExecute,
    getHistory: mockGetHistory,
  },
}))

import settlementJobReducer, {
  fetchSettlementJobs,
  createSettlementJob,
  fetchSettlementJobById,
  executeSettlementJob,
  fetchSettlementJobHistory,
  setSettlementJobPage,
  setSettlementJobPageSize,
  clearSelectedSettlementJob,
  resetSettlementJobState,
} from './settlementJobSlice'

const createTestStore = () =>
  configureStore({
    reducer: {
      settlementJobs: settlementJobReducer,
    },
  })

describe('settlementJobSlice', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should initialize with the correct state', () => {
    const store = createTestStore()
    const state = store.getState().settlementJobs

    expect(state.items).toEqual([])
    expect(state.selectedJob).toBeNull()
    expect(state.executionHistory).toEqual([])
    expect(state.page).toBe(0)
    expect(state.size).toBe(20)
    expect(state.totalElements).toBe(0)
    expect(state.totalPages).toBe(0)
    expect(state.loading).toBe(false)
    expect(state.creating).toBe(false)
    expect(state.executing).toBe(false)
    expect(state.error).toBeNull()
  })

  it('should update the current page', () => {
    const store = createTestStore()

    store.dispatch(setSettlementJobPage(3))

    expect(store.getState().settlementJobs.page).toBe(3)
  })

  it('should update page size and reset page', () => {
    const store = createTestStore()

    store.dispatch(setSettlementJobPage(4))
    store.dispatch(setSettlementJobPageSize(50))

    const state = store.getState().settlementJobs

    expect(state.size).toBe(50)
    expect(state.page).toBe(0)
  })

  it('should fetch settlement jobs successfully', async () => {
    const response = {
      content: [
        {
          id: 1,
          jobId: 'JOB-001',
          status: 'PENDING',
          totalTransactions: 5,
        },
      ],
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    }

    mockGetPage.mockResolvedValue(response)

    const store = createTestStore()

    await store.dispatch(
      fetchSettlementJobs({
        page: 0,
        size: 20,
        status: '',
        token: 'test-token',
      }),
    )

    expect(mockGetPage).toHaveBeenCalledWith(
      { page: 0, size: 20, status: '' },
      'test-token',
    )

    const state = store.getState().settlementJobs

    expect(state.items).toEqual(response.content)
    expect(state.totalElements).toBe(1)
    expect(state.totalPages).toBe(1)
    expect(state.loading).toBe(false)
    expect(state.error).toBeNull()
  })

  it('should handle settlement job fetching failure', async () => {
    mockGetPage.mockRejectedValue(
      new Error('Unable to load jobs'),
    )

    const store = createTestStore()

    await store.dispatch(fetchSettlementJobs({ token: 'test-token' }))

    const state = store.getState().settlementJobs

    expect(state.loading).toBe(false)
    expect(state.error).toBe('Unable to load jobs')
  })

  it('should create a settlement job successfully', async () => {
    const job = {
      id: 1,
      jobId: 'JOB-001',
      status: 'PENDING',
    }

    mockCreate.mockResolvedValue(job)

    const store = createTestStore()

    await store.dispatch(
      createSettlementJob({
        transactionIds: [101, 102],
        token: 'test-token',
      }),
    )

    expect(mockCreate).toHaveBeenCalledWith(
      [101, 102],
      'test-token',
    )

    const state = store.getState().settlementJobs

    expect(state.creating).toBe(false)
    expect(state.creationError).toBeNull()
  })

  it('should handle settlement job creation failure', async () => {
    mockCreate.mockRejectedValue(
      new Error('Job creation failed'),
    )

    const store = createTestStore()

    await store.dispatch(
      createSettlementJob({
        transactionIds: [101],
        token: 'test-token',
      }),
    )

    const state = store.getState().settlementJobs

    expect(state.creating).toBe(false)
    expect(state.creationError).toBe('Job creation failed')
  })

  it('should fetch settlement job details successfully', async () => {
    const job = {
      id: 1,
      jobId: 'JOB-001',
      status: 'COMPLETED',
    }

    mockGetById.mockResolvedValue(job)

    const store = createTestStore()

    await store.dispatch(
      fetchSettlementJobById({
        jobId: 'JOB-001',
        token: 'test-token',
      }),
    )

    expect(mockGetById).toHaveBeenCalledWith(
      'JOB-001',
      'test-token',
    )

    expect(store.getState().settlementJobs.selectedJob).toEqual(job)
    expect(store.getState().settlementJobs.detailsLoading).toBe(false)
  })

  it('should handle settlement job details failure', async () => {
    mockGetById.mockRejectedValue(
      new Error('Job not found'),
    )

    const store = createTestStore()

    await store.dispatch(
      fetchSettlementJobById({
        jobId: 'JOB-999',
        token: 'test-token',
      }),
    )

    const state = store.getState().settlementJobs

    expect(state.detailsLoading).toBe(false)
    expect(state.detailsError).toBe('Job not found')
  })

  it('should execute a settlement job successfully', async () => {
    const job = {
      id: 1,
      jobId: 'JOB-001',
      status: 'COMPLETED',
      processedCount: 5,
    }

    mockExecute.mockResolvedValue(job)

    const store = createTestStore()

    await store.dispatch(
      executeSettlementJob({
        jobId: 'JOB-001',
        token: 'test-token',
      }),
    )

    expect(mockExecute).toHaveBeenCalledWith(
      'JOB-001',
      'test-token',
    )

    const state = store.getState().settlementJobs

    expect(state.executing).toBe(false)
    expect(state.selectedJob).toEqual(job)
    expect(state.executionError).toBeNull()
  })

  it('should handle settlement job execution failure', async () => {
    mockExecute.mockRejectedValue(
      new Error('Execution failed'),
    )

    const store = createTestStore()

    await store.dispatch(
      executeSettlementJob({
        jobId: 'JOB-001',
        token: 'test-token',
      }),
    )

    const state = store.getState().settlementJobs

    expect(state.executing).toBe(false)
    expect(state.executionError).toBe('Execution failed')
  })

  it('should fetch execution history successfully', async () => {
    const history = [
      {
        id: 1,
        attemptNumber: 1,
        status: 'COMPLETED',
        processedCount: 5,
      },
    ]

    mockGetHistory.mockResolvedValue(history)

    const store = createTestStore()

    await store.dispatch(
      fetchSettlementJobHistory({
        jobId: 'JOB-001',
        token: 'test-token',
      }),
    )

    expect(mockGetHistory).toHaveBeenCalledWith(
      'JOB-001',
      'test-token',
    )

    const state = store.getState().settlementJobs

    expect(state.executionHistory).toEqual(history)
    expect(state.historyLoading).toBe(false)
    expect(state.historyError).toBeNull()
  })

  it('should handle execution history failure', async () => {
    mockGetHistory.mockRejectedValue(
      new Error('History unavailable'),
    )

    const store = createTestStore()

    await store.dispatch(
      fetchSettlementJobHistory({
        jobId: 'JOB-001',
        token: 'test-token',
      }),
    )

    const state = store.getState().settlementJobs

    expect(state.historyLoading).toBe(false)
    expect(state.historyError).toBe('History unavailable')
  })

  it('should clear selected job and execution history', () => {
    const store = createTestStore()

    store.dispatch({
      type: fetchSettlementJobById.fulfilled.type,
      payload: { id: 1, jobId: 'JOB-001' },
    })

    store.dispatch({
      type: fetchSettlementJobHistory.fulfilled.type,
      payload: [{ id: 1, attemptNumber: 1 }],
    })

    store.dispatch(clearSelectedSettlementJob())

    const state = store.getState().settlementJobs

    expect(state.selectedJob).toBeNull()
    expect(state.executionHistory).toEqual([])
    expect(state.detailsError).toBeNull()
    expect(state.historyError).toBeNull()
  })

  it('should reset settlement job state', () => {
    const store = createTestStore()

    store.dispatch(setSettlementJobPage(5))
    store.dispatch(resetSettlementJobState())

    const state = store.getState().settlementJobs

    expect(state.page).toBe(0)
    expect(state.items).toEqual([])
    expect(state.selectedJob).toBeNull()
    expect(state.executionHistory).toEqual([])
    expect(state.loading).toBe(false)
  })
})