import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configureStore } from '@reduxjs/toolkit'

const { mockGetAll, mockGetPage, mockGetById } = vi.hoisted(() => ({
  mockGetAll: vi.fn(),
  mockGetPage: vi.fn(),
  mockGetById: vi.fn(),
}))

vi.mock('../../services/api', () => ({
  transactionApi: {
    getAll: mockGetAll,
    getPage: mockGetPage,
    getById: mockGetById,
  },
}))

import transactionReducer, {
  clearSelectedTransaction,
  setPage,
  setPageSize,
  resetPagination,
  fetchTransactions,
  fetchTransactionsPage,
  fetchTransactionById,
} from './transactionSlice'

const createTestStore = () =>
  configureStore({
    reducer: {
      transactions: transactionReducer,
    },
  })

describe('transactionSlice', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should initialize with the correct state', () => {
    const store = createTestStore()
    const state = store.getState().transactions

    expect(state.items).toEqual([])
    expect(state.pageItems).toEqual([])
    expect(state.page).toBe(0)
    expect(state.size).toBe(10)
    expect(state.loading).toBe(false)
    expect(state.paginationLoading).toBe(false)
    expect(state.error).toBeNull()
    expect(state.selectedTransaction).toBeNull()
  })

  it('should update the current page', () => {
    const store = createTestStore()

    store.dispatch(setPage(3))

    expect(store.getState().transactions.page).toBe(3)
  })

  it('should update page size and reset the page', () => {
    const store = createTestStore()

    store.dispatch(setPage(4))
    store.dispatch(setPageSize(25))

    const state = store.getState().transactions

    expect(state.size).toBe(25)
    expect(state.page).toBe(0)
  })

  it('should reset pagination', () => {
    const store = createTestStore()

    store.dispatch(setPage(3))
    store.dispatch(setPageSize(25))

    store.dispatch(resetPagination())

    const state = store.getState().transactions

    expect(state.page).toBe(0)
    expect(state.size).toBe(10)
    expect(state.totalElements).toBe(0)
    expect(state.totalPages).toBe(0)
    expect(state.first).toBe(true)
    expect(state.last).toBe(true)
    expect(state.pageItems).toEqual([])
  })

  it('should clear the selected transaction', () => {
    const store = createTestStore()

    store.dispatch({
      type: fetchTransactionById.fulfilled.type,
      payload: { id: 101 },
    })

    store.dispatch(clearSelectedTransaction())

    expect(
      store.getState().transactions.selectedTransaction,
    ).toBeNull()
  })

  it('should fetch all transactions successfully', async () => {
    const transactions = [
      { id: 1, amount: 100 },
      { id: 2, amount: 200 },
    ]

    mockGetAll.mockResolvedValue(transactions)

    const store = createTestStore()

    await store.dispatch(fetchTransactions('test-token'))

    const state = store.getState().transactions

    expect(mockGetAll).toHaveBeenCalledWith('test-token')
    expect(state.items).toEqual(transactions)
    expect(state.loading).toBe(false)
    expect(state.error).toBeNull()
  })

  it('should support transaction responses wrapped in value', async () => {
    const transactions = [{ id: 1, amount: 100 }]

    mockGetAll.mockResolvedValue({ value: transactions })

    const store = createTestStore()

    await store.dispatch(fetchTransactions('test-token'))

    expect(store.getState().transactions.items).toEqual(transactions)
  })

  it('should handle transaction fetching failure', async () => {
    mockGetAll.mockRejectedValue(new Error('Server unavailable'))

    const store = createTestStore()

    await store.dispatch(fetchTransactions('test-token'))

    const state = store.getState().transactions

    expect(state.items).toEqual([])
    expect(state.loading).toBe(false)
    expect(state.error).toBe('Server unavailable')
  })

  it('should fetch a transaction page successfully', async () => {
    const response = {
      content: [{ id: 10, amount: 500 }],
      number: 1,
      size: 10,
      totalElements: 21,
      totalPages: 3,
      first: false,
      last: false,
    }

    mockGetPage.mockResolvedValue(response)

    const store = createTestStore()

    await store.dispatch(
      fetchTransactionsPage({
        page: 1,
        size: 10,
        search: 'payment',
        status: 'SUCCESS',
        paymentMethod: 'UPI',
        sortBy: 'createdAt',
        direction: 'desc',
        token: 'test-token',
      }),
    )

    const state = store.getState().transactions

    expect(mockGetPage).toHaveBeenCalledWith(
      {
        page: 1,
        size: 10,
        search: 'payment',
        status: 'SUCCESS',
        paymentMethod: 'UPI',
        sortBy: 'createdAt',
        direction: 'desc',
      },
      'test-token',
    )

    expect(state.pageItems).toEqual(response.content)
    expect(state.page).toBe(1)
    expect(state.totalElements).toBe(21)
    expect(state.totalPages).toBe(3)
    expect(state.first).toBe(false)
    expect(state.last).toBe(false)
    expect(state.paginationLoading).toBe(false)
  })

  it('should handle paginated transaction fetching failure', async () => {
    mockGetPage.mockRejectedValue(new Error('Page loading failed'))

    const store = createTestStore()

    await store.dispatch(fetchTransactionsPage({ token: 'test-token' }))

    const state = store.getState().transactions

    expect(state.paginationLoading).toBe(false)
    expect(state.error).toBe('Page loading failed')
  })

  it('should fetch a transaction by ID successfully', async () => {
    const transaction = {
      id: 101,
      amount: 1500,
      status: 'SUCCESS',
    }

    mockGetById.mockResolvedValue(transaction)

    const store = createTestStore()

    await store.dispatch(
      fetchTransactionById({
        id: 101,
        token: 'test-token',
      }),
    )

    expect(mockGetById).toHaveBeenCalledWith(101, 'test-token')

    expect(
      store.getState().transactions.selectedTransaction,
    ).toEqual(transaction)
  })
})
