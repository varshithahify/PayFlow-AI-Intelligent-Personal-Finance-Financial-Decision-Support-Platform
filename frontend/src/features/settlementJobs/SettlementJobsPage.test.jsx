import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { Provider } from 'react-redux'
import { configureStore } from '@reduxjs/toolkit'

import SettlementJobsPage from './SettlementJobsPage'
import settlementJobsReducer from './settlementJobSlice'
import transactionsReducer from '../transactions/transactionSlice'

const { transactionApi, settlementJobApi } = vi.hoisted(() => ({
  transactionApi: {
    getAll: vi.fn(),
  },
  settlementJobApi: {
    getPage: vi.fn(),
    create: vi.fn(),
    getById: vi.fn(),
    execute: vi.fn(),
    getHistory: vi.fn(),
  },
}))

vi.mock('../../services/api', () => ({
      transactionApi,
  settlementJobApi,
}))

const mockTransactions = [
  {
    id: 101,
    amount: 1500,
    currency: 'INR',
    status: 'SUCCESS',
    createdAt: '2026-11-16T10:00:00',
  },
  {
    id: 102,
    amount: 2500,
    currency: 'INR',
    status: 'SUCCESS',
    createdAt: '2026-11-16T11:00:00',
  },
]

const mockJob = {
  id: 1,
  jobId: 'JOB-001',
  status: 'PENDING',
  totalTransactions: 2,
  processedCount: 0,
  discrepancyCount: 0,
  createdAt: '2026-11-16T10:00:00',
  startedAt: null,
  completedAt: null,
  errorMessage: null,
}

const mockPage = {
  content: [mockJob],
  number: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}

const mockHistory = [
  {
    id: 1,
    attemptNumber: 1,
    status: 'COMPLETED',
    processedCount: 2,
    discrepancyCount: 0,
    startedAt: '2026-11-16T10:00:00',
    completedAt: '2026-11-16T10:01:00',
  },
]

function createTestStore() {
  return configureStore({
    reducer: {
      settlementJobs: settlementJobsReducer,
      transactions: transactionsReducer,
    },
  })
}

function renderPage() {
  const store = createTestStore()

  render(
    <Provider store={store}>
      <SettlementJobsPage token="test-token" />
    </Provider>,
  )

  return store
}

describe('SettlementJobsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    transactionApi.getAll.mockResolvedValue(mockTransactions)
    settlementJobApi.getPage.mockResolvedValue(mockPage)
    settlementJobApi.create.mockResolvedValue(mockJob)
    settlementJobApi.getById.mockResolvedValue(mockJob)
    settlementJobApi.execute.mockResolvedValue({
      ...mockJob,
      status: 'COMPLETED',
      processedCount: 2,
    })
    settlementJobApi.getHistory.mockResolvedValue(mockHistory)
  })

  it('should render the settlement jobs page', async () => {
    renderPage()

    expect(
      screen.getByRole('heading', {
        name: /settlement job management/i,
      }),
    ).toBeInTheDocument()

    expect(
      await screen.findByText('JOB-001'),
    ).toBeInTheDocument()

    expect(transactionApi.getAll).toHaveBeenCalledWith('test-token')
  })

  it('should display available transactions in the create form', async () => {
    renderPage()

    await screen.findByText('JOB-001')

    fireEvent.click(
      screen.getByRole('button', {
        name: /create settlement job/i,
      }),
    )

    expect(
      await screen.findByText('Select all transactions'),
    ).toBeInTheDocument()

    expect(screen.getByText('101')).toBeInTheDocument()
    expect(screen.getByText('102')).toBeInTheDocument()
  })

  it('should create a settlement job using selected transactions', async () => {
    renderPage()

    await screen.findByText('JOB-001')

    fireEvent.click(
      screen.getByRole('button', {
        name: /create settlement job/i,
      }),
    )

    await screen.findByText('Select all transactions')

    const checkboxes = screen.getAllByRole('checkbox')

    fireEvent.click(checkboxes[1])

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Create job',
      }),
    )

    await waitFor(() => {
      expect(settlementJobApi.create).toHaveBeenCalledWith(
        [101],
        'test-token',
      )
    })

    expect(
      await screen.findByText(
        'Settlement job created successfully.',
      ),
    ).toBeInTheDocument()
  })

  it('should filter settlement jobs by status', async () => {
    renderPage()

    await screen.findByText('JOB-001')

    fireEvent.change(
      screen.getByRole('combobox', {
        name: /filter settlement jobs by status/i,
      }),
      {
        target: {
          value: 'COMPLETED',
        },
      },
    )

    await waitFor(() => {
      expect(settlementJobApi.getPage).toHaveBeenCalledWith(
        {
          page: 0,
          size: 20,
          status: 'COMPLETED',
        },
        'test-token',
      )
    })
  })

  it('should display settlement job details and execution history', async () => {
    renderPage()

    await screen.findByText('JOB-001')

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Details',
      }),
    )

    expect(
      await screen.findByRole('dialog'),
    ).toBeInTheDocument()

    expect(
      screen.queryByText('No execution history available.'),
      ).not.toBeInTheDocument()

    expect(
    await screen.findByRole('cell', { name: 'COMPLETED' }),
    ).toBeInTheDocument()

    expect(
    screen.getByText('1'),
    ).toBeInTheDocument()

    expect(settlementJobApi.getById).toHaveBeenCalledWith(
      'JOB-001',
      'test-token',
    )

    expect(settlementJobApi.getHistory).toHaveBeenCalledWith(
      'JOB-001',
      'test-token',
    )

    expect(
      await screen.findByRole('cell', { name: 'COMPLETED' }),
    ).toBeInTheDocument()
  })

  it('should execute a pending settlement job', async () => {
    renderPage()

    await screen.findByText('JOB-001')

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Execute',
      }),
    )

    await waitFor(() => {
      expect(settlementJobApi.execute).toHaveBeenCalledWith(
        'JOB-001',
        'test-token',
      )
    })

    expect(
      await screen.findByText(
        'Settlement job execution completed.',
      ),
    ).toBeInTheDocument()
  })
})