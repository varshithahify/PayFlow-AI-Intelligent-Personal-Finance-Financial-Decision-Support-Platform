import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import {
  fetchSettlementJobs,
  createSettlementJob,
  fetchSettlementJobById,
  fetchSettlementJobHistory,
  executeSettlementJob,
  clearSelectedSettlementJob,
  setSettlementJobPage,
} from './settlementJobSlice'

import { fetchTransactions } from '../transactions/transactionSlice'

const STATUS_OPTIONS = [
  'PENDING',
  'RUNNING',
  'COMPLETED',
  'FAILED',
]

function formatDate(value) {
  if (!value) return '—'

  return new Date(value).toLocaleString()
}

function formatStatus(status) {
  return (status || 'UNKNOWN').replaceAll('_', ' ')
}

function SettlementJobsPage({ token }) {
  const dispatch = useDispatch()

  const transactions = useSelector(
    (state) => state.transactions.items,
  )

  const {
    items,
    selectedJob,
    executionHistory,
    page,
    totalPages,
    totalElements,
    loading,
    creating,
    executing,
    detailsLoading,
    historyLoading,
    error,
    creationError,
    executionError,
    detailsError,
    historyError,
  } = useSelector((state) => state.settlementJobs)

  const [selectedTransactions, setSelectedTransactions] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [selectedJobId, setSelectedJobId] = useState(null)
  const [successMessage, setSuccessMessage] = useState('')

  useEffect(() => {
    dispatch(fetchTransactions(token))
  }, [dispatch, token])

  useEffect(() => {
    dispatch(
      fetchSettlementJobs({
        page,
        size: 20,
        status: statusFilter,
        token,
      }),
    )
  }, [dispatch, page, statusFilter, token])

  function toggleTransaction(id) {
    setSelectedTransactions((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id],
    )
  }

  function toggleAllTransactions() {
    const ids = transactions.map((transaction) => transaction.id)

    const allSelected =
      ids.length > 0 &&
      ids.every((id) => selectedTransactions.includes(id))

    setSelectedTransactions((current) =>
      allSelected
        ? current.filter((id) => !ids.includes(id))
        : [...new Set([...current, ...ids])],
    )
  }

  async function handleCreateJob(event) {
    event.preventDefault()
    setSuccessMessage('')

    if (selectedTransactions.length === 0) {
      return
    }

    try {
      await dispatch(
        createSettlementJob({
          transactionIds: selectedTransactions,
          token,
        }),
      ).unwrap()

      setSelectedTransactions([])
      setShowCreateForm(false)
      setSuccessMessage('Settlement job created successfully.')

      dispatch(
        fetchSettlementJobs({
          page: 0,
          size: 20,
          status: statusFilter,
          token,
        }),
      )
    } catch {
      // Error is stored in Redux.
    }
  }

  async function handleViewDetails(jobId) {
    setSelectedJobId(jobId)

    dispatch(clearSelectedSettlementJob())

    try {
      await Promise.all([
        dispatch(fetchSettlementJobById({ jobId, token })).unwrap(),
        dispatch(fetchSettlementJobHistory({ jobId, token })).unwrap(),
      ])
    } catch {
      // Errors are displayed from Redux.
    }
  }

  async function handleExecute(jobId) {
    setSuccessMessage('')

    try {
      await dispatch(
        executeSettlementJob({ jobId, token }),
      ).unwrap()

      setSuccessMessage('Settlement job execution completed.')

      dispatch(
        fetchSettlementJobs({
          page,
          size: 20,
          status: statusFilter,
          token,
        }),
      )

      dispatch(
        fetchSettlementJobHistory({ jobId, token }),
      )
    } catch {
      // Execution errors are displayed from Redux.
    }
  }

  function closeDetails() {
    setSelectedJobId(null)
    dispatch(clearSelectedSettlementJob())
  }

  return (
    <div className="settlement-jobs-page">
      <section className="welcome-section">
        <div>
          <p className="eyebrow">PAYMENT OPERATIONS</p>
          <h2>Settlement job management</h2>
          <p className="welcome-description">
            Create, execute, and track settlement reconciliation jobs.
          </p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={() => setShowCreateForm((value) => !value)}
        >
          <span>＋</span>
          Create settlement job
        </button>
      </section>

      {successMessage && (
        <div className="auth-alert auth-alert-success" role="status">
          {successMessage}
        </div>
      )}

      {creationError && (
        <div className="auth-alert auth-alert-error" role="alert">
          {creationError}
        </div>
      )}

      {executionError && (
        <div className="auth-alert auth-alert-error" role="alert">
          {executionError}
        </div>
      )}

      {showCreateForm && (
        <section className="panel settlement-create-panel">
          <div className="panel-header">
            <div>
              <h3>Create settlement job</h3>
              <p>Select transactions to include in this job.</p>
            </div>

            <button
              type="button"
              className="secondary-button"
              onClick={() => setShowCreateForm(false)}
            >
              Close
            </button>
          </div>

          {transactions.length === 0 ? (
            <p className="modal-message">
              No transactions are currently available.
            </p>
          ) : (
            <form onSubmit={handleCreateJob}>
              <div className="settlement-selection-toolbar">
                <label>
                  <input
                    type="checkbox"
                    checked={
                      transactions.length > 0 &&
                      transactions.every((transaction) =>
                        selectedTransactions.includes(transaction.id),
                      )
                    }
                    onChange={toggleAllTransactions}
                  />
                  Select all transactions
                </label>

                <span>
                  {selectedTransactions.length} selected
                </span>
              </div>

              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Select</th>
                      <th>Transaction ID</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Created</th>
                    </tr>
                  </thead>

                  <tbody>
                    {transactions.map((transaction) => (
                      <tr key={transaction.id}>
                        <td>
                          <input
                            type="checkbox"
                            checked={selectedTransactions.includes(
                              transaction.id,
                            )}
                            onChange={() =>
                              toggleTransaction(transaction.id)
                            }
                          />
                        </td>
                        <td>{transaction.id}</td>
                        <td>
                          {transaction.currency || 'INR'}{' '}
                          {transaction.amount ?? '—'}
                        </td>
                        <td>{formatStatus(transaction.status)}</td>
                        <td>{formatDate(transaction.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="settlement-form-footer">
                <button
                  type="submit"
                  className="primary-button"
                  disabled={
                    creating || selectedTransactions.length === 0
                  }
                >
                  {creating ? 'Creating...' : 'Create job'}
                </button>
              </div>
            </form>
          )}
        </section>
      )}

      <section className="panel settlement-list-panel">
        <div className="panel-header">
          <div>
            <h3>Settlement jobs</h3>
            <p>{totalElements} total jobs</p>
          </div>

          <select
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value)
              dispatch(setSettlementJobPage(0))
            }}
            aria-label="Filter settlement jobs by status"
          >
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>
                {formatStatus(status)}
              </option>
            ))}
          </select>
        </div>

        {loading ? (
          <p className="modal-message">Loading settlement jobs...</p>
        ) : error ? (
          <p className="modal-message modal-error">{error}</p>
        ) : items.length === 0 ? (
          <p className="modal-message">
            No settlement jobs found.
          </p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Job ID</th>
                  <th>Status</th>
                  <th>Transactions</th>
                  <th>Processed</th>
                  <th>Discrepancies</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {items.map((job) => (
                  <tr key={job.id}>
                    <td>{job.jobId}</td>
                    <td>
                      <span
                        className={`status-badge ${
                          job.status === 'COMPLETED'
                            ? 'success'
                            : job.status === 'FAILED'
                              ? 'failed'
                              : 'pending'
                        }`}
                      >
                        {formatStatus(job.status)}
                      </span>
                    </td>
                    <td>{job.totalTransactions}</td>
                    <td>{job.processedCount}</td>
                    <td>{job.discrepancyCount}</td>
                    <td>{formatDate(job.createdAt)}</td>
                    <td>
                      <div className="settlement-actions">
                        <button
                          type="button"
                          className="details-button"
                          onClick={() => handleViewDetails(job.jobId)}
                        >
                          Details
                        </button>

                        {job.status === 'PENDING' && (
                          <button
                            type="button"
                            className="primary-button"
                            disabled={executing}
                            onClick={() => handleExecute(job.jobId)}
                          >
                            {executing ? 'Executing...' : 'Execute'}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="settlement-pagination">
          <button
            type="button"
            className="secondary-button"
            disabled={page <= 0 || loading}
            onClick={() => dispatch(setSettlementJobPage(page - 1))}
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
            onClick={() => dispatch(setSettlementJobPage(page + 1))}
          >
            Next
          </button>
        </div>
      </section>

      {selectedJobId && (
        <div className="modal-backdrop">
          <section
            className="transaction-modal panel settlement-details-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="settlement-details-title"
          >
            <div className="modal-header">
              <div>
                <p className="eyebrow">SETTLEMENT JOB</p>
                <h3 id="settlement-details-title">Job details</h3>
              </div>

              <button
                type="button"
                className="modal-close"
                onClick={closeDetails}
                aria-label="Close details"
              >
                ×
              </button>
            </div>

            {detailsLoading ? (
              <p className="modal-message">Loading job details...</p>
            ) : detailsError ? (
              <p className="modal-message modal-error">
                {detailsError}
              </p>
            ) : selectedJob ? (
              <>
                <div className="detail-grid">
                  <div>
                    <span>Job ID</span>
                    <strong>{selectedJob.jobId}</strong>
                  </div>
                  <div>
                    <span>Status</span>
                    <strong>{formatStatus(selectedJob.status)}</strong>
                  </div>
                  <div>
                    <span>Total transactions</span>
                    <strong>{selectedJob.totalTransactions}</strong>
                  </div>
                  <div>
                    <span>Processed</span>
                    <strong>{selectedJob.processedCount}</strong>
                  </div>
                  <div>
                    <span>Discrepancies</span>
                    <strong>{selectedJob.discrepancyCount}</strong>
                  </div>
                  <div>
                    <span>Created</span>
                    <strong>{formatDate(selectedJob.createdAt)}</strong>
                  </div>
                  <div>
                    <span>Started</span>
                    <strong>{formatDate(selectedJob.startedAt)}</strong>
                  </div>
                  <div>
                    <span>Completed</span>
                    <strong>{formatDate(selectedJob.completedAt)}</strong>
                  </div>
                </div>

                {selectedJob.errorMessage && (
                  <p className="modal-error">
                    {selectedJob.errorMessage}
                  </p>
                )}
              </>
            ) : null}

            <div className="settlement-history">
              <h4>Execution history</h4>

              {historyLoading ? (
                <p>Loading execution history...</p>
              ) : historyError ? (
                <p className="modal-error">{historyError}</p>
              ) : executionHistory.length === 0 ? (
                <p>No execution history available.</p>
              ) : (
                <div className="table-wrapper">
                  <table>
                    <thead>
                      <tr>
                        <th>Attempt</th>
                        <th>Status</th>
                        <th>Processed</th>
                        <th>Discrepancies</th>
                        <th>Started</th>
                        <th>Completed</th>
                      </tr>
                    </thead>
                    <tbody>
                      {executionHistory.map((execution) => (
                        <tr key={execution.id}>
                          <td>{execution.attemptNumber}</td>
                          <td>{formatStatus(execution.status)}</td>
                          <td>{execution.processedCount}</td>
                          <td>{execution.discrepancyCount}</td>
                          <td>{formatDate(execution.startedAt)}</td>
                          <td>{formatDate(execution.completedAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

export default SettlementJobsPage