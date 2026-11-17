import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
} from '@testing-library/react'
import { Provider } from 'react-redux'
import { configureStore } from '@reduxjs/toolkit'

import EventMonitoringPage from './EventMonitoringPage'
import eventMonitoringReducer from './eventMonitoringSlice'

const {
  eventMonitoringApi,
} = vi.hoisted(() => ({
  eventMonitoringApi: {
    getOverview: vi.fn(),
    getProcessed: vi.fn(),
    getFailed: vi.fn(),
    getProcessedById: vi.fn(),
    getFailedById: vi.fn(),
  },
}))

vi.mock('../../services/api', () => ({
  eventMonitoringApi,
}))

const mockOverview = {
  totalEvents: 15,
  totalProcessedEvents: 10,
  totalFailedEvents: 5,
}

const mockProcessedEvent = {
  id: 1,
  eventId: 'processed-event-001',
  eventType: 'TRANSACTION_PROCESSED',
  orgId: 'ORG-001',
  processedAt: '2026-11-16T10:00:00',
}

const mockFailedEvent = {
  id: 2,
  eventId: 'failed-event-001',
  transactionId: 101,
  orgId: 'ORG-001',
  eventType: 'TRANSACTION_FAILED',
  failureReason: 'Payment gateway timeout',
  recordedAt: '2026-11-16T11:00:00',
}

const mockProcessedPage = {
  content: [mockProcessedEvent],
  number: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}

const mockFailedPage = {
  content: [mockFailedEvent],
  number: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}

function createTestStore() {
  return configureStore({
    reducer: {
      eventMonitoring: eventMonitoringReducer,
    },
  })
}

function renderPage() {
  const store = createTestStore()

  render(
    <Provider store={store}>
      <EventMonitoringPage token="test-token" />
    </Provider>,
  )

  return store
}

describe('EventMonitoringPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    eventMonitoringApi.getOverview.mockResolvedValue(
      mockOverview,
    )

    eventMonitoringApi.getProcessed.mockResolvedValue(
      mockProcessedPage,
    )

    eventMonitoringApi.getFailed.mockResolvedValue(
      mockFailedPage,
    )

    eventMonitoringApi.getProcessedById.mockResolvedValue(
      mockProcessedEvent,
    )

    eventMonitoringApi.getFailedById.mockResolvedValue(
      mockFailedEvent,
    )
  })

  it('should render the Event Monitoring page', async () => {
    renderPage()

    expect(
      screen.getByRole('heading', {
        name: /event monitoring/i,
      }),
    ).toBeInTheDocument()

    expect(
      await screen.findByText('processed-event-001'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('failed-event-001'),
    ).toBeInTheDocument()

    expect(
      eventMonitoringApi.getOverview,
    ).toHaveBeenCalledWith('test-token')
  })

  it('should display event overview statistics', async () => {
    renderPage()

    expect(
      await screen.findByText('15'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('10'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('5'),
    ).toBeInTheDocument()
  })

  it('should display processed event information', async () => {
    renderPage()

    expect(
      await screen.findByText('processed-event-001'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('TRANSACTION PROCESSED'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('ORG-001'),
    ).toBeInTheDocument()

    expect(
      eventMonitoringApi.getProcessed,
    ).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        size: 20,
      }),
      'test-token',
    )
  })

  it('should display failed event information', async () => {
    renderPage()

    expect(
      await screen.findByText('failed-event-001'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('TRANSACTION FAILED'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('Payment gateway timeout'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('101'),
    ).toBeInTheDocument()

    expect(
      eventMonitoringApi.getFailed,
    ).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        size: 20,
      }),
      'test-token',
    )
  })

  it('should apply event type and date filters', async () => {
    renderPage()

    await screen.findByText('processed-event-001')

    fireEvent.change(
      screen.getByLabelText('Event Type'),
      {
        target: {
          value: 'TRANSACTION_CREATED',
        },
      },
    )

    fireEvent.change(
      screen.getByLabelText('From Date'),
      {
        target: {
          value: '2026-11-16T09:00',
        },
      },
    )

    fireEvent.change(
      screen.getByLabelText('To Date'),
      {
        target: {
          value: '2026-11-16T18:00',
        },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Apply Filters',
      }),
    )

    await waitFor(() => {
      expect(
        eventMonitoringApi.getProcessed,
      ).toHaveBeenLastCalledWith(
        expect.objectContaining({
          page: 0,
          size: 20,
          eventType: 'TRANSACTION_CREATED',
          fromDate: '2026-11-16T09:00',
          toDate: '2026-11-16T18:00',
        }),
        'test-token',
      )
    })

    expect(
      eventMonitoringApi.getFailed,
    ).toHaveBeenLastCalledWith(
      expect.objectContaining({
        eventType: 'TRANSACTION_CREATED',
      }),
      'test-token',
    )
  })

  it('should clear applied filters', async () => {
    renderPage()

    await screen.findByText('processed-event-001')

    fireEvent.change(
      screen.getByLabelText('Event Type'),
      {
        target: {
          value: 'TRANSACTION_FAILED',
        },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Apply Filters',
      }),
    )

    await waitFor(() => {
      expect(
        eventMonitoringApi.getProcessed,
      ).toHaveBeenLastCalledWith(
        expect.objectContaining({
          eventType: 'TRANSACTION_FAILED',
        }),
        'test-token',
      )
    })

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Clear Filters',
      }),
    )

    await waitFor(() => {
      expect(
        eventMonitoringApi.getProcessed,
      ).toHaveBeenLastCalledWith(
        expect.objectContaining({
          eventType: '',
          fromDate: '',
          toDate: '',
        }),
        'test-token',
      )
    })

    expect(
      screen.getByLabelText('Event Type'),
    ).toHaveValue('')
  })

  it('should display processed event details', async () => {
    renderPage()

    await screen.findByText('processed-event-001')

    const processedSection = screen
      .getByRole('heading', {
        name: 'Processed Events',
      })
      .closest('section')

    fireEvent.click(
      within(processedSection).getByRole('button', {
        name: 'Details',
      }),
    )

    const dialog = await screen.findByRole('dialog')

    expect(
      await within(dialog).findByText('Event Details'),
    ).toBeInTheDocument()

    expect(
      within(dialog).getByText('PROCESSED'),
    ).toBeInTheDocument()

    expect(
      within(dialog).getByText('processed-event-001'),
    ).toBeInTheDocument()

    expect(
      eventMonitoringApi.getProcessedById,
    ).toHaveBeenCalledWith(
      'processed-event-001',
      'test-token',
    )
  })

  it('should display failed event details and close the modal', async () => {
    renderPage()

    await screen.findByText('failed-event-001')

    const failedSection = screen
      .getByRole('heading', {
        name: 'Failed Events',
      })
      .closest('section')

    fireEvent.click(
      within(failedSection).getByRole('button', {
        name: 'Details',
      }),
    )

    const dialog = await screen.findByRole('dialog')

    expect(
      await within(dialog).findByText(
        'Payment gateway timeout',
      ),
    ).toBeInTheDocument()

    expect(
      within(dialog).getByText('FAILED'),
    ).toBeInTheDocument()

    expect(
      within(dialog).getByText('failed-event-001'),
    ).toBeInTheDocument()

    expect(
      eventMonitoringApi.getFailedById,
    ).toHaveBeenCalledWith(
      'failed-event-001',
      'test-token',
    )

    fireEvent.click(
      within(dialog).getByRole('button', {
        name: 'Close event details',
      }),
    )

    await waitFor(() => {
      expect(
        screen.queryByRole('dialog'),
      ).not.toBeInTheDocument()
    })
  })
})