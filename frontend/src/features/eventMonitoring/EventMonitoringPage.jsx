import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import {
  fetchEventOverview,
  fetchProcessedEvents,
  fetchFailedEvents,
  fetchProcessedEventById,
  fetchFailedEventById,
  setProcessedEventPage,
  setFailedEventPage,
  clearSelectedEvent,
} from './eventMonitoringSlice'

function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString()
}

function formatEventType(value) {
  return (value || 'UNKNOWN').replaceAll('_', ' ')
}

function EventMonitoringPage({ token }) {
  const dispatch = useDispatch()

  const {
    overview,
    processedEvents,
    failedEvents,

    processedPage,
    processedTotalPages,
    processedTotalElements,

    failedPage,
    failedTotalPages,
    failedTotalElements,

    selectedEvent,
    selectedEventCategory,

    overviewLoading,
    processedLoading,
    failedLoading,
    detailsLoading,

    overviewError,
    processedError,
    failedError,
    detailsError,
  } = useSelector((state) => state.eventMonitoring)

  const [eventType, setEventType] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  const [appliedFilters, setAppliedFilters] = useState({
    eventType: '',
    fromDate: '',
    toDate: '',
  })

  const [showDetails, setShowDetails] = useState(false)

  // Fetch event overview.
  useEffect(() => {
    dispatch(fetchEventOverview(token))
  }, [dispatch, token])

  // Fetch processed events.
  useEffect(() => {
    dispatch(
      fetchProcessedEvents({
        page: processedPage,
        size: 20,
        ...appliedFilters,
        token,
      }),
    )
  }, [dispatch, processedPage, appliedFilters, token])

  // Fetch failed events.
  useEffect(() => {
    dispatch(
      fetchFailedEvents({
        page: failedPage,
        size: 20,
        ...appliedFilters,
        token,
      }),
    )
  }, [dispatch, failedPage, appliedFilters, token])

  function handleApplyFilters(event) {
    event.preventDefault()

    if (
      fromDate &&
      toDate &&
      new Date(fromDate) > new Date(toDate)
    ) {
      return
    }

    setAppliedFilters({
      eventType: eventType.trim(),
      fromDate,
      toDate,
    })

    dispatch(setProcessedEventPage(0))
    dispatch(setFailedEventPage(0))
  }

  function handleClearFilters() {
    setEventType('')
    setFromDate('')
    setToDate('')

    setAppliedFilters({
      eventType: '',
      fromDate: '',
      toDate: '',
    })

    dispatch(setProcessedEventPage(0))
    dispatch(setFailedEventPage(0))
  }

  async function handleViewProcessedDetails(eventId) {
    setShowDetails(true)
    dispatch(clearSelectedEvent())

    await dispatch(
      fetchProcessedEventById({
        eventId,
        token,
      }),
    )
  }

  async function handleViewFailedDetails(eventId) {
    setShowDetails(true)
    dispatch(clearSelectedEvent())

    await dispatch(
      fetchFailedEventById({
        eventId,
        token,
      }),
    )
  }

  function closeDetails() {
    setShowDetails(false)
    dispatch(clearSelectedEvent())
  }

  function renderPagination({
    page,
    totalPages,
    loading,
    onPrevious,
    onNext,
    label,
  }) {
    return (
      <div className="event-pagination">
        <button
          type="button"
          className="secondary-button"
          disabled={page <= 0 || loading}
          onClick={onPrevious}
        >
          Previous
        </button>

        <span>
          Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
        </span>

        <button
          type="button"
          className="secondary-button"
          disabled={page + 1 >= totalPages || loading}
          onClick={onNext}
        >
          Next
        </button>

        <span className="event-pagination-label">{label}</span>
      </div>
    )
  }

  return (
    <div className="event-monitoring-page">

      {/* Page heading */}
      <section className="welcome-section">
        <div>
          <p className="eyebrow">PAYMENT OPERATIONS</p>

          <h2>Event Monitoring</h2>

          <p className="welcome-description">
            Monitor processed Kafka events and failed event records
            across your organization.
          </p>
        </div>
      </section>

      {/* Overview cards */}
      <section className="event-overview-grid">

        <div className="panel event-overview-card">
          <p>Total Events</p>

          <h3>
            {overviewLoading ? '...' : overview.totalEvents}
          </h3>

          <span>All recorded events</span>
        </div>

        <div className="panel event-overview-card">
          <p>Processed Events</p>

          <h3 className="event-processed-count">
            {overviewLoading
              ? '...'
              : overview.totalProcessedEvents}
          </h3>

          <span>Successfully processed events</span>
        </div>

        <div className="panel event-overview-card">
          <p>Failed Events</p>

          <h3 className="event-failed-count">
            {overviewLoading
              ? '...'
              : overview.totalFailedEvents}
          </h3>

          <span>Recorded failed events</span>
        </div>

      </section>

      {overviewError && (
        <div className="auth-alert auth-alert-error" role="alert">
          {overviewError}
        </div>
      )}

      {/* Filters */}
      <section className="panel event-filter-panel">

        <div className="panel-header">
          <div>
            <h3>Event Filters</h3>

            <p>Filter events by type and date range.</p>
          </div>
        </div>

        <form onSubmit={handleApplyFilters}>

          <div className="event-filter-grid">

            <div className="event-filter-field">
              <label htmlFor="event-type-filter">
                Event Type
              </label>

              <input
                id="event-type-filter"
                type="text"
                placeholder="Enter event type"
                value={eventType}
                onChange={(event) =>
                  setEventType(event.target.value)
                }
              />
            </div>

            <div className="event-filter-field">
              <label htmlFor="event-from-date">
                From Date
              </label>

              <input
                id="event-from-date"
                type="datetime-local"
                value={fromDate}
                onChange={(event) =>
                  setFromDate(event.target.value)
                }
              />
            </div>

            <div className="event-filter-field">
              <label htmlFor="event-to-date">
                To Date
              </label>

              <input
                id="event-to-date"
                type="datetime-local"
                value={toDate}
                onChange={(event) =>
                  setToDate(event.target.value)
                }
              />
            </div>

          </div>

          {fromDate &&
            toDate &&
            new Date(fromDate) > new Date(toDate) && (
              <p className="modal-error" role="alert">
                From date cannot be after the to date.
              </p>
            )}

          <div className="event-filter-actions">

            <button
              type="submit"
              className="primary-button"
              disabled={
                Boolean(
                  fromDate &&
                  toDate &&
                  new Date(fromDate) > new Date(toDate),
                )
              }
            >
              Apply Filters
            </button>

            <button
              type="button"
              className="secondary-button"
              onClick={handleClearFilters}
            >
              Clear Filters
            </button>

          </div>

        </form>
      </section>

      {/* Processed events */}
      <section className="panel event-table-panel">

        <div className="panel-header">
          <div>
            <h3>Processed Events</h3>

            <p>
              {processedTotalElements} processed events recorded.
            </p>
          </div>

          <span className="event-status-label processed-label">
            PROCESSED
          </span>
        </div>

        {processedLoading ? (
          <p className="modal-message">
            Loading processed events...
          </p>
        ) : processedError ? (
          <p className="modal-message modal-error" role="alert">
            {processedError}
          </p>
        ) : processedEvents.length === 0 ? (
          <p className="modal-message">
            No processed events found.
          </p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Event Type</th>
                  <th>Organization</th>
                  <th>Processed At</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {processedEvents.map((event) => (
                  <tr key={event.eventId}>
                    <td className="event-id-cell">
                      {event.eventId}
                    </td>

                    <td>
                      {formatEventType(event.eventType)}
                    </td>

                    <td>{event.orgId}</td>

                    <td>{formatDate(event.processedAt)}</td>

                    <td>
                      <button
                        type="button"
                        className="details-button"
                        onClick={() =>
                          handleViewProcessedDetails(event.eventId)
                        }
                      >
                        Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {renderPagination({
          page: processedPage,
          totalPages: processedTotalPages,
          loading: processedLoading,
          onPrevious: () =>
            dispatch(setProcessedEventPage(processedPage - 1)),
          onNext: () =>
            dispatch(setProcessedEventPage(processedPage + 1)),
          label: 'Processed events',
        })}

      </section>

      {/* Failed events */}
      <section className="panel event-table-panel">

        <div className="panel-header">
          <div>
            <h3>Failed Events</h3>

            <p>
              {failedTotalElements} failed events recorded.
            </p>
          </div>

          <span className="event-status-label failed-label">
            FAILED
          </span>
        </div>

        {failedLoading ? (
          <p className="modal-message">
            Loading failed events...
          </p>
        ) : failedError ? (
          <p className="modal-message modal-error" role="alert">
            {failedError}
          </p>
        ) : failedEvents.length === 0 ? (
          <p className="modal-message">
            No failed events found.
          </p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Event Type</th>
                  <th>Transaction ID</th>
                  <th>Failure Reason</th>
                  <th>Recorded At</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {failedEvents.map((event) => (
                  <tr key={event.eventId}>
                    <td className="event-id-cell">
                      {event.eventId}
                    </td>

                    <td>
                      {formatEventType(event.eventType)}
                    </td>

                    <td>{event.transactionId ?? '—'}</td>

                    <td className="event-failure-reason">
                      {event.failureReason || '—'}
                    </td>

                    <td>{formatDate(event.recordedAt)}</td>

                    <td>
                      <button
                        type="button"
                        className="details-button"
                        onClick={() =>
                          handleViewFailedDetails(event.eventId)
                        }
                      >
                        Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {renderPagination({
          page: failedPage,
          totalPages: failedTotalPages,
          loading: failedLoading,
          onPrevious: () =>
            dispatch(setFailedEventPage(failedPage - 1)),
          onNext: () =>
            dispatch(setFailedEventPage(failedPage + 1)),
          label: 'Failed events',
        })}

      </section>

      {/* Event details modal */}
      {showDetails && (
        <div
          className="modal-backdrop"
          onClick={closeDetails}
        >
          <section
            className="transaction-modal panel event-details-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="event-details-title"
            onClick={(event) => event.stopPropagation()}
          >

            <div className="modal-header">
              <div>
                <p className="eyebrow">EVENT MONITORING</p>

                <h3 id="event-details-title">
                  Event Details
                </h3>
              </div>

              <button
                type="button"
                className="modal-close"
                onClick={closeDetails}
                aria-label="Close event details"
              >
                ×
              </button>
            </div>

            {detailsLoading ? (
              <p className="modal-message">
                Loading event details...
              </p>
            ) : detailsError ? (
              <p className="modal-message modal-error" role="alert">
                {detailsError}
              </p>
            ) : selectedEvent ? (
              <div className="event-detail-grid">

                <div>
                  <span>Event ID</span>
                  <strong>{selectedEvent.eventId}</strong>
                </div>

                <div>
                  <span>Event Type</span>
                  <strong>
                    {formatEventType(selectedEvent.eventType)}
                  </strong>
                </div>

                <div>
                  <span>Category</span>
                  <strong>{selectedEventCategory}</strong>
                </div>

                <div>
                  <span>Organization ID</span>
                  <strong>{selectedEvent.orgId ?? '—'}</strong>
                </div>

                {selectedEventCategory === 'FAILED' && (
                  <>
                    <div>
                      <span>Transaction ID</span>
                      <strong>
                        {selectedEvent.transactionId ?? '—'}
                      </strong>
                    </div>

                    <div className="event-detail-full">
                      <span>Failure Reason</span>
                      <strong>
                        {selectedEvent.failureReason || '—'}
                      </strong>
                    </div>

                    <div>
                      <span>Recorded At</span>
                      <strong>
                        {formatDate(selectedEvent.recordedAt)}
                      </strong>
                    </div>
                  </>
                )}

                {selectedEventCategory === 'PROCESSED' && (
                  <div>
                    <span>Processed At</span>
                    <strong>
                      {formatDate(selectedEvent.processedAt)}
                    </strong>
                  </div>
                )}

              </div>
            ) : null}

          </section>
        </div>
      )}

    </div>
  )
}

export default EventMonitoringPage