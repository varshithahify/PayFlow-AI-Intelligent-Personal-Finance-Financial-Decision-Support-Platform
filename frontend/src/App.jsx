import { useEffect, useState } from 'react'
import {
  authApi,
  transactionApi,
  reconciliationApi,
} from './services/api'
import './App.css'

const navigation = [
  { label: 'Overview', icon: '◫' },
  { label: 'Transactions', icon: '⇄' },
  { label: 'Reconciliation', icon: '◴' },
  { label: 'Settlement Jobs', icon: '▤' },
  { label: 'Event Monitoring', icon: '⌁' },
]

const RECONCILIATION_STATUSES = [
  'MATCHED',
  'MISMATCH',
  'MISSING_EXTERNAL',
  'MISSING_INTERNAL',
]

function App() {
  const [token, setToken] = useState(null)
  const [userEmail, setUserEmail] = useState('')
  const [authMode, setAuthMode] = useState('login')

  function handleAuthenticated(authToken, email) {
    setToken(authToken)
    setUserEmail(email)
  }

  function handleLogout() {
    setToken(null)
    setUserEmail('')
    setAuthMode('login')
  }

  if (!token) {
    return (
      <AuthScreen
        mode={authMode}
        onModeChange={setAuthMode}
        onAuthenticated={handleAuthenticated}
      />
    )
  }

  return (
    <Dashboard
      email={userEmail}
      token={token}
      onLogout={handleLogout}
    />
  )
}

function AuthScreen({ mode, onModeChange, onAuthenticated }) {
  const isRegister = mode === 'register'

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')

    const normalizedEmail = email.trim()

    if (!normalizedEmail || !password.trim()) {
      setError('Please enter your email and password.')
      return
    }

    if (isRegister && !name.trim()) {
      setError('Please enter your name.')
      return
    }

    if (isRegister && password.length < 6) {
      setError('Password must contain at least 6 characters.')
      return
    }

    setLoading(true)

    try {
      if (isRegister) {
        await authApi.register({
          name: name.trim(),
          email: normalizedEmail,
          password,
        })

        setMessage(
          'Account created successfully. You can now sign in.',
        )
        setPassword('')
        onModeChange('login')
      } else {
        const response = await authApi.login({
          email: normalizedEmail,
          password,
        })

        if (!response?.token) {
          throw new Error(
            'The server did not return an authentication token.',
          )
        }

        onAuthenticated(response.token, normalizedEmail)
      }
    } catch (requestError) {
      setError(
        requestError.message ||
          'Something went wrong. Please try again.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-brand-panel">
        <div className="auth-brand">
          <div className="brand-icon">P</div>
          <div>
            <h1>PayFlow AI</h1>
            <p>Payment Intelligence</p>
          </div>
        </div>

        <div className="auth-hero">
          <span className="auth-eyebrow">
            PAYMENT OPERATIONS PLATFORM
          </span>

          <h2>
            Every payment.
            <br />
            One clear picture.
          </h2>

          <p>
            Monitor transactions, investigate discrepancies, and bring
            payment operations together in one workspace.
          </p>

          <div className="auth-feature-list">
            <div>
              <span>✓</span>
              Transaction visibility
            </div>
            <div>
              <span>✓</span>
              Reconciliation workflows
            </div>
            <div>
              <span>✓</span>
              Event-driven processing
            </div>
          </div>
        </div>

        <p className="auth-footer">
          PayFlow AI · Development environment
        </p>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-mobile-brand">
            <div className="brand-icon">P</div>
            <strong>PayFlow AI</strong>
          </div>

          <span className="auth-form-eyebrow">
            {isRegister ? 'GET STARTED' : 'WELCOME BACK'}
          </span>

          <h2>
            {isRegister
              ? 'Create your account'
              : 'Sign in to PayFlow AI'}
          </h2>

          <p className="auth-form-description">
            {isRegister
              ? 'Create an account to access your payment workspace.'
              : 'Enter your credentials to access your workspace.'}
          </p>

          {error && (
            <div className="auth-alert auth-alert-error" role="alert">
              {error}
            </div>
          )}

          {message && (
            <div
              className="auth-alert auth-alert-success"
              role="status"
            >
              {message}
            </div>
          )}

          <form className="auth-form" onSubmit={handleSubmit}>
            {isRegister && (
              <label className="auth-field">
                <span>Full name</span>
                <input
                  type="text"
                  name="name"
                  autoComplete="name"
                  placeholder="Enter your full name"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  required
                />
              </label>
            )}

            <label className="auth-field">
              <span>Email address</span>
              <input
                type="email"
                name="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </label>

            <label className="auth-field">
              <span>Password</span>
              <input
                type="password"
                name="password"
                autoComplete={
                  isRegister ? 'new-password' : 'current-password'
                }
                placeholder={
                  isRegister
                    ? 'At least 6 characters'
                    : 'Enter your password'
                }
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                minLength={isRegister ? 6 : undefined}
                required
              />
            </label>

            <button
              className="auth-submit-button"
              type="submit"
              disabled={loading}
            >
              {loading
                ? 'Please wait...'
                : isRegister
                  ? 'Create account'
                  : 'Sign in'}

              {!loading && <span aria-hidden="true">→</span>}
            </button>
          </form>

          <div className="auth-switch">
            {isRegister
              ? 'Already have an account?'
              : "Don't have an account?"}{' '}

            <button
              type="button"
              onClick={() => {
                setError('')
                setMessage('')
                setPassword('')
                onModeChange(isRegister ? 'login' : 'register')
              }}
            >
              {isRegister ? 'Sign in' : 'Create account'}
            </button>
          </div>

          <p className="auth-security-note">
            Your connection uses the PayFlow AI backend.
          </p>
        </div>
      </section>
    </main>
  )
}

function Dashboard({ email, token, onLogout }) {
  const [activePage, setActivePage] = useState('Overview')

  const [transactions, setTransactions] = useState([])
  const [loadingTransactions, setLoadingTransactions] = useState(true)
  const [transactionError, setTransactionError] = useState('')

  const [refreshVersion, setRefreshVersion] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const [selectedTransaction, setSelectedTransaction] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')

  const [reconciliations, setReconciliations] = useState([])
  const [loadingReconciliations, setLoadingReconciliations] =
    useState(true)
  const [reconciliationError, setReconciliationError] = useState('')
  const [reconciliationFilter, setReconciliationFilter] =
    useState('ALL')
  const [reconciliationRefreshVersion, setReconciliationRefreshVersion] =
    useState(0)
  const [reconcilingTransactionId, setReconcilingTransactionId] =
    useState(null)
  const [reconciliationActionError, setReconciliationActionError] =
    useState('')
  const [reconciliationActionMessage, setReconciliationActionMessage] =
    useState('')

  // Load internal transactions from the backend.
  useEffect(() => {
    let cancelled = false

    async function loadTransactions() {
      setLoadingTransactions(true)
      setTransactionError('')

      try {
        const data = await transactionApi.getAll(token)

        const records = Array.isArray(data)
          ? data
          : Array.isArray(data?.value)
            ? data.value
            : []

        if (!cancelled) {
          setTransactions(records)
        }
      } catch (error) {
        if (!cancelled) {
          setTransactionError(
            error.message || 'Unable to load transactions.',
          )
        }
      } finally {
        if (!cancelled) {
          setLoadingTransactions(false)
        }
      }
    }

    if (token) {
      loadTransactions()
    } else {
      setLoadingTransactions(false)
    }

    return () => {
      cancelled = true
    }
  }, [token, refreshVersion])

  // Load reconciliation records using the four supported backend statuses.
  useEffect(() => {
    let cancelled = false

    async function loadReconciliations() {
      setLoadingReconciliations(true)
      setReconciliationError('')

      try {
        const responses = await Promise.all(
          RECONCILIATION_STATUSES.map((status) =>
            reconciliationApi.getByStatus(status, token),
          ),
        )

        const recordsById = new Map()

        responses.forEach((response) => {
          const records = Array.isArray(response)
            ? response
            : Array.isArray(response?.value)
              ? response.value
              : []

          records.forEach((record) => {
            if (record?.id != null) {
              recordsById.set(String(record.id), record)
            }
          })
        })

        const records = Array.from(recordsById.values()).sort(
          (a, b) =>
            new Date(b.reconciledAt || 0).getTime() -
            new Date(a.reconciledAt || 0).getTime(),
        )

        if (!cancelled) {
          setReconciliations(records)
        }
      } catch (error) {
        if (!cancelled) {
          setReconciliationError(
            error.message ||
              'Unable to load reconciliation records.',
          )
        }
      } finally {
        if (!cancelled) {
          setLoadingReconciliations(false)
        }
      }
    }

    if (token) {
      loadReconciliations()
    } else {
      setLoadingReconciliations(false)
    }

    return () => {
      cancelled = true
    }
  }, [token, reconciliationRefreshVersion])

  const getStatus = (transaction) =>
    String(transaction?.status || 'UNKNOWN').toUpperCase()

  function formatAmount(amount, currency = 'INR') {
    try {
      return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: currency || 'INR',
      }).format(Number(amount ?? 0))
    } catch {
      return `${currency || 'INR'} ${Number(amount ?? 0).toFixed(2)}`
    }
  }

  function formatDate(date) {
    if (!date) return '—'

    const parsedDate = new Date(date)

    if (Number.isNaN(parsedDate.getTime())) {
      return '—'
    }

    return parsedDate.toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const successfulTransactions = transactions.filter(
    (transaction) => getStatus(transaction) === 'SUCCESS',
  ).length

  const pendingTransactions = transactions.filter((transaction) =>
    ['CREATED', 'PROCESSING', 'PENDING'].includes(
      getStatus(transaction),
    ),
  ).length

  const failedTransactions = transactions.filter((transaction) =>
    ['FAILED', 'REFUNDED'].includes(getStatus(transaction)),
  ).length

  const matchedReconciliations = reconciliations.filter(
    (record) => record.status === 'MATCHED',
  ).length

  const mismatchedReconciliations = reconciliations.filter(
    (record) => record.status === 'MISMATCH',
  ).length

  const missingExternalReconciliations = reconciliations.filter(
    (record) => record.status === 'MISSING_EXTERNAL',
  ).length

  const missingInternalReconciliations = reconciliations.filter(
    (record) => record.status === 'MISSING_INTERNAL',
  ).length

  const normalizedSearch = search.trim().toLowerCase()

  const filteredTransactions = transactions.filter((transaction) => {
    const transactionId = String(transaction.id ?? '')
    const searchableId = `txn-${transactionId}`.toLowerCase()

    const matchesSearch =
      !normalizedSearch ||
      transactionId.includes(normalizedSearch.replace(/^txn-/, '')) ||
      searchableId.includes(normalizedSearch) ||
      String(transaction.paymentMethod || '')
        .toLowerCase()
        .includes(normalizedSearch) ||
      String(transaction.currency || '')
        .toLowerCase()
        .includes(normalizedSearch)

    const matchesStatus =
      statusFilter === 'ALL' ||
      getStatus(transaction) === statusFilter

    return matchesSearch && matchesStatus
  })

  const filteredReconciliations = reconciliations.filter((record) => {
    if (reconciliationFilter === 'ALL') {
      return true
    }

    return record.status === reconciliationFilter
  })

  async function openTransaction(transaction) {
    setSelectedTransaction(transaction)
    setDetailError('')
    setDetailLoading(true)

    try {
      const detail = await transactionApi.getById(
        transaction.id,
        token,
      )

      if (detail && typeof detail === 'object') {
        setSelectedTransaction(detail)
      }
    } catch (error) {
      setDetailError(
        error.message || 'Could not load the latest transaction details.',
      )
    } finally {
      setDetailLoading(false)
    }
  }

  async function reconcileTransaction(transactionId) {
    setReconcilingTransactionId(transactionId)
    setReconciliationActionError('')
    setReconciliationActionMessage('')

    try {
      const result = await reconciliationApi.reconcileTransaction(
        transactionId,
        token,
      )

      setReconciliationActionMessage(
        `Transaction ${transactionId} reconciled successfully. Status: ${
          result?.status || 'UNKNOWN'
        }.`,
      )

      setReconciliationRefreshVersion((version) => version + 1)
    } catch (error) {
      setReconciliationActionError(
        error.message || 'Unable to reconcile this transaction.',
      )
    } finally {
      setReconcilingTransactionId(null)
    }
  }

  const pageTitle =
    activePage === 'Overview' ? 'Payment Overview' : activePage

  const pageDescription = {
    Overview:
      'Monitor transaction activity and key payment metrics.',
    Transactions:
      'Search, filter, and inspect transactions returned by your backend.',
    Reconciliation:
      'Compare internal transactions with external payment records.',
    'Settlement Jobs':
      'Settlement job visibility will be connected in a later phase.',
    'Event Monitoring':
      'Kafka event and dead-letter queue monitoring will be connected in a later phase.',
  }[activePage]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">P</div>
          <div>
            <h1>PayFlow AI</h1>
            <p>Payment Intelligence</p>
          </div>
        </div>

        <div className="workspace-label">WORKSPACE</div>

        <nav className="navigation" aria-label="Main navigation">
          {navigation.map((item) => (
            <button
              key={item.label}
              type="button"
              className={`nav-item ${
                activePage === item.label ? 'active' : ''
              }`}
              onClick={() => setActivePage(item.label)}
              aria-current={
                activePage === item.label ? 'page' : undefined
              }
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="system-status">
            <span className="status-dot" />
            <div>
              <strong>System status</strong>
              <p>Development environment</p>
            </div>
          </div>

          <div className="profile">
            <div className="avatar">
              {email.charAt(0).toUpperCase()}
            </div>
            <div className="profile-info">
              <strong>{email}</strong>
              <p>Authenticated user</p>
            </div>
          </div>

          <button
            className="logout-button"
            type="button"
            onClick={onLogout}
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <p className="breadcrumb">
              Workspace / {activePage}
            </p>
            <h2>{pageTitle}</h2>
          </div>

          <div className="topbar-right">
            <span className="environment-badge">
              {transactionError
                ? 'API ERROR'
                : loadingTransactions
                  ? 'LOADING'
                  : 'LIVE TRANSACTIONS'}
            </span>

            <div className="avatar top-avatar">
              {email.charAt(0).toUpperCase()}
            </div>
          </div>
        </header>

        {activePage === 'Overview' ? (
          <>
            <section className="welcome-section">
              <div>
                <p className="eyebrow">PAYMENT OPERATIONS</p>
                <h2>Your payment command center</h2>
                <p className="welcome-description">
                  Monitor transactions, investigate discrepancies, and
                  track reconciliation activity from one place.
                </p>
              </div>

              <button
                className="primary-button"
                type="button"
                onClick={() => setActivePage('Transactions')}
              >
                <span>⇄</span> View transactions
              </button>
            </section>

            {transactionError && (
              <div
                className="auth-alert auth-alert-error"
                role="alert"
              >
                <strong>Could not load transactions.</strong>
                <p>{transactionError}</p>

                <button
                  className="secondary-button"
                  type="button"
                  onClick={() =>
                    setRefreshVersion((version) => version + 1)
                  }
                >
                  Retry
                </button>
              </div>
            )}

            <section className="metrics-grid">
              <MetricCard
                label="Total transactions"
                value={
                  loadingTransactions ? '—' : String(transactions.length)
                }
                note="Live backend data"
                icon="⇄"
                tone="purple"
              />

              <MetricCard
                label="Successful payments"
                value={
                  loadingTransactions
                    ? '—'
                    : String(successfulTransactions)
                }
                note="Status: SUCCESS"
                icon="✓"
                tone="green"
              />

              <MetricCard
                label="Pending payments"
                value={
                  loadingTransactions
                    ? '—'
                    : String(pendingTransactions)
                }
                note="Created or processing"
                icon="◷"
                tone="orange"
              />

              <MetricCard
                label="Failed / refunded"
                value={
                  loadingTransactions
                    ? '—'
                    : String(failedTransactions)
                }
                note="Failed or refunded status"
                icon="!"
                tone="red"
              />
            </section>

            <section className="metrics-grid">
              <MetricCard
                label="Matched reconciliations"
                value={
                  loadingReconciliations
                    ? '—'
                    : String(matchedReconciliations)
                }
                note="Internal and external records match"
                icon="✓"
                tone="green"
              />

              <MetricCard
                label="Mismatches"
                value={
                  loadingReconciliations
                    ? '—'
                    : String(mismatchedReconciliations)
                }
                note="Records requiring investigation"
                icon="!"
                tone="red"
              />
            </section>

            <section className="content-grid">
              <TransactionTable
                transactions={transactions.slice(0, 5)}
                loading={loadingTransactions}
                error={transactionError}
                formatAmount={formatAmount}
                formatDate={formatDate}
                onSelect={openTransaction}
                title="Recent transactions"
                subtitle="Latest records returned by the backend"
                onViewAll={() => setActivePage('Transactions')}
              />

              <div className="right-column">
                <section className="panel reconciliation-panel">
                  <div className="panel-header">
                    <div>
                      <h3>Reconciliation</h3>
                      <p>Live backend results</p>
                    </div>
                    <span className="reconciliation-icon">◴</span>
                  </div>

                  {loadingReconciliations ? (
                    <p className="modal-message">
                      Loading reconciliation records...
                    </p>
                  ) : reconciliationError ? (
                    <div
                      className="auth-alert auth-alert-error"
                      role="alert"
                    >
                      {reconciliationError}
                    </div>
                  ) : (
                    <>
                      <div className="reconciliation-total">
                        <span>Total reconciliation records</span>
                        <strong>{reconciliations.length}</strong>
                      </div>

                      <div className="reconciliation-total">
                        <span>Matched</span>
                        <strong>{matchedReconciliations}</strong>
                      </div>

                      <div className="reconciliation-total">
                        <span>Mismatched</span>
                        <strong>{mismatchedReconciliations}</strong>
                      </div>

                      <div className="reconciliation-total">
                        <span>Missing external</span>
                        <strong>{missingExternalReconciliations}</strong>
                      </div>

                      <div className="reconciliation-total">
                        <span>Missing internal</span>
                        <strong>{missingInternalReconciliations}</strong>
                      </div>
                    </>
                  )}

                  <div className="panel-action-row">
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => setActivePage('Reconciliation')}
                    >
                      Open reconciliation
                    </button>
                  </div>
                </section>

                <section className="panel system-panel">
                  <div className="panel-header">
                    <div>
                      <h3>Platform components</h3>
                      <p>
                        Architecture overview, not live health checks
                      </p>
                    </div>
                  </div>

                  <SystemRow
                    name="Spring Boot API"
                    description="Backend services"
                  />
                  <SystemRow
                    name="PostgreSQL"
                    description="Persistent storage"
                  />
                  <SystemRow
                    name="Apache Kafka"
                    description="Event processing"
                  />
                  <SystemRow
                    name="Redis"
                    description="Caching layer"
                  />

                  <p className="demo-note">
                    Component health checks will be added separately.
                  </p>
                </section>
              </div>
            </section>
          </>
        ) : activePage === 'Transactions' ? (
          <>
            <section className="welcome-section">
              <div>
                <p className="eyebrow">PAYMENT OPERATIONS</p>
                <h2>Transaction explorer</h2>
                <p className="welcome-description">
                  {pageDescription}
                </p>
              </div>

              <button
                className="primary-button"
                type="button"
                onClick={() =>
                  setRefreshVersion((version) => version + 1)
                }
                disabled={loadingTransactions}
              >
                <span>↻</span>
                {loadingTransactions ? 'Refreshing...' : 'Refresh'}
              </button>
            </section>

            {transactionError && (
              <div
                className="auth-alert auth-alert-error"
                role="alert"
              >
                <strong>Could not load transactions.</strong>
                <p>{transactionError}</p>

                <button
                  className="secondary-button"
                  type="button"
                  onClick={() =>
                    setRefreshVersion((version) => version + 1)
                  }
                >
                  Retry
                </button>
              </div>
            )}

            <section className="panel transaction-explorer">
              <div className="explorer-toolbar">
                <label className="search-field">
                  <span aria-hidden="true">⌕</span>
                  <input
                    type="search"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search transaction ID, payment method, currency..."
                    aria-label="Search transactions"
                  />
                </label>

                <label className="filter-field">
                  <span>Status</span>
                  <select
                    value={statusFilter}
                    onChange={(event) =>
                      setStatusFilter(event.target.value)
                    }
                    aria-label="Filter by transaction status"
                  >
                    <option value="ALL">All statuses</option>
                    <option value="CREATED">Created</option>
                    <option value="PROCESSING">Processing</option>
                    <option value="PENDING">Pending</option>
                    <option value="SUCCESS">Success</option>
                    <option value="FAILED">Failed</option>
                    <option value="REFUNDED">Refunded</option>
                  </select>
                </label>
              </div>

              <div className="panel-header explorer-heading">
                <div>
                  <h3>All transactions</h3>
                  <p>
                    {loadingTransactions
                      ? 'Loading records from the backend...'
                      : `${filteredTransactions.length} of ${transactions.length} transaction(s)`}
                  </p>
                </div>
              </div>

              <TransactionTable
                transactions={filteredTransactions}
                loading={loadingTransactions}
                error={transactionError}
                formatAmount={formatAmount}
                formatDate={formatDate}
                onSelect={openTransaction}
                title=""
                subtitle=""
                showHeader={false}
              />
            </section>
          </>
        ) : activePage === 'Reconciliation' ? (
          <>
            <section className="welcome-section">
              <div>
                <p className="eyebrow">PAYMENT OPERATIONS</p>
                <h2>Reconciliation center</h2>
                <p className="welcome-description">
                  {pageDescription}
                </p>
              </div>

              <button
                className="primary-button"
                type="button"
                disabled={loadingReconciliations}
                onClick={() =>
                  setReconciliationRefreshVersion(
                    (version) => version + 1,
                  )
                }
              >
                <span>↻</span>
                {loadingReconciliations ? 'Refreshing...' : 'Refresh'}
              </button>
            </section>

            {reconciliationActionMessage && (
              <div
                className="auth-alert auth-alert-success"
                role="status"
              >
                {reconciliationActionMessage}
              </div>
            )}

            {reconciliationActionError && (
              <div
                className="auth-alert auth-alert-error"
                role="alert"
              >
                {reconciliationActionError}
              </div>
            )}

            {reconciliationError && (
              <div
                className="auth-alert auth-alert-error"
                role="alert"
              >
                <strong>
                  Could not load reconciliation records.
                </strong>
                <p>{reconciliationError}</p>
                <p>
                  Check the backend logs and the reconciliation API
                  responses if this error persists.
                </p>
              </div>
            )}

            <section className="metrics-grid">
              <MetricCard
                label="Total reconciliations"
                value={
                  loadingReconciliations
                    ? '—'
                    : String(reconciliations.length)
                }
                note="Records returned by backend"
                icon="◴"
                tone="purple"
              />

              <MetricCard
                label="Matched"
                value={
                  loadingReconciliations
                    ? '—'
                    : String(matchedReconciliations)
                }
                note="Successful comparisons"
                icon="✓"
                tone="green"
              />

              <MetricCard
                label="Mismatches"
                value={
                  loadingReconciliations
                    ? '—'
                    : String(mismatchedReconciliations)
                }
                note="Amount, currency, or status differences"
                icon="!"
                tone="red"
              />

              <MetricCard
                label="Missing records"
                value={
                  loadingReconciliations
                    ? '—'
                    : String(
                        missingExternalReconciliations +
                          missingInternalReconciliations,
                      )
                }
                note="Missing external or internal payment"
                icon="◷"
                tone="orange"
              />
            </section>

            <section className="panel transaction-explorer">
              <div className="explorer-toolbar">
                <label className="filter-field">
                  <span>Reconciliation status</span>
                  <select
                    value={reconciliationFilter}
                    onChange={(event) =>
                      setReconciliationFilter(event.target.value)
                    }
                    aria-label="Filter reconciliation status"
                  >
                    <option value="ALL">All statuses</option>
                    <option value="MATCHED">Matched</option>
                    <option value="MISMATCH">Mismatch</option>
                    <option value="MISSING_EXTERNAL">
                      Missing external
                    </option>
                    <option value="MISSING_INTERNAL">
                      Missing internal
                    </option>
                  </select>
                </label>
              </div>

              <div className="panel-header explorer-heading">
                <div>
                  <h3>Reconciliation records</h3>
                  <p>
                    {loadingReconciliations
                      ? 'Loading records...'
                      : `${filteredReconciliations.length} record(s) displayed`}
                  </p>
                </div>
              </div>

              <ReconciliationTable
                records={filteredReconciliations}
                loading={loadingReconciliations}
                error={reconciliationError}
                formatAmount={formatAmount}
                formatDate={formatDate}
              />
            </section>

            <section className="panel transaction-explorer">
              <div className="panel-header explorer-heading">
                <div>
                  <h3>Run reconciliation</h3>
                  <p>
                    Select an internal transaction to compare it with
                    its external payment record.
                  </p>
                </div>
              </div>

              {loadingTransactions ? (
                <p className="modal-message">
                  Loading transactions...
                </p>
              ) : transactionError ? (
                <div
                  className="auth-alert auth-alert-error"
                  role="alert"
                >
                  {transactionError}
                </div>
              ) : transactions.length === 0 ? (
                <p className="modal-message">
                  No internal transactions are available.
                </p>
              ) : (
                <div className="table-wrapper">
                  <table>
                    <thead>
                      <tr>
                        <th>Transaction</th>
                        <th>Amount</th>
                        <th>Payment status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.map((transaction) => (
                        <tr key={transaction.id}>
                          <td className="transaction-id">
                            TXN-{transaction.id}
                          </td>
                          <td>
                            {formatAmount(
                              transaction.amount,
                              transaction.currency,
                            )}
                          </td>
                          <td>
                            <StatusBadge status={transaction.status} />
                          </td>
                          <td>
                            <button
                              className="details-button"
                              type="button"
                              disabled={
                                reconcilingTransactionId ===
                                transaction.id
                              }
                              onClick={() =>
                                reconcileTransaction(transaction.id)
                              }
                            >
                              {reconcilingTransactionId ===
                              transaction.id
                                ? 'Reconciling...'
                                : 'Reconcile'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </>
        ) : (
          <section className="page-placeholder panel">
            <div className="placeholder-icon">
              {navigation.find(
                (item) => item.label === activePage,
              )?.icon || '•'}
            </div>

            <p className="eyebrow">WORK IN PROGRESS</p>
            <h2>{activePage}</h2>
            <p>{pageDescription}</p>

            <p className="placeholder-note">
              This section is not connected to a live backend workflow
              yet. It will be implemented in a later PayFlow AI phase.
            </p>

            <button
              className="secondary-button"
              type="button"
              onClick={() => setActivePage('Overview')}
            >
              Back to overview
            </button>
          </section>
        )}

        <footer className="footer">
          <span>PayFlow AI</span>
          <span>
            Payment Intelligence Platform · Development UI
          </span>
        </footer>
      </main>

      {selectedTransaction && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setSelectedTransaction(null)
            }
          }}
        >
          <section
            className="transaction-modal panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="transaction-modal-title"
          >
            <div className="panel-header modal-header">
              <div>
                <p className="eyebrow">TRANSACTION DETAILS</p>
                <h3 id="transaction-modal-title">
                  TXN-{selectedTransaction.id}
                </h3>
              </div>

              <button
                type="button"
                className="modal-close"
                onClick={() => setSelectedTransaction(null)}
                aria-label="Close transaction details"
              >
                ×
              </button>
            </div>

            {detailLoading && (
              <p className="modal-message">
                Loading latest details...
              </p>
            )}

            {detailError && (
              <div
                className="auth-alert auth-alert-error modal-error"
                role="alert"
              >
                {detailError}
              </div>
            )}

            <div className="detail-grid">
              <DetailItem
                label="Amount"
                value={formatAmount(
                  selectedTransaction.amount,
                  selectedTransaction.currency,
                )}
              />

              <DetailItem
                label="Status"
                value={
                  <StatusBadge status={selectedTransaction.status} />
                }
              />

              <DetailItem
                label="Payment method"
                value={selectedTransaction.paymentMethod || '—'}
              />

              <DetailItem
                label="Currency"
                value={selectedTransaction.currency || '—'}
              />

              <DetailItem
                label="Transaction ID"
                value={String(selectedTransaction.id)}
              />

              <DetailItem
                label="User ID"
                value={selectedTransaction.userId ?? '—'}
              />

              <DetailItem
                label="Created at"
                value={formatDate(selectedTransaction.createdAt)}
              />

              <DetailItem
                label="Updated at"
                value={formatDate(selectedTransaction.updatedAt)}
              />
            </div>

            <p className="demo-note modal-note">
              This view displays transaction fields returned by the
              backend. No payment state is changed from this dialog.
            </p>
          </section>
        </div>
      )}
    </div>
  )
}

function TransactionTable({
  transactions,
  loading,
  error,
  formatAmount,
  formatDate,
  onSelect,
  title,
  subtitle,
  onViewAll,
  showHeader = true,
}) {
  return (
    <div className="panel transactions-panel">
      {showHeader && (
        <div className="panel-header">
          <div>
            <h3>{title}</h3>
            <p>{subtitle}</p>
          </div>

          {onViewAll && (
            <button
              className="text-button"
              type="button"
              onClick={onViewAll}
            >
              View all →
            </button>
          )}
        </div>
      )}

      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Transaction</th>
              <th>Payment method</th>
              <th>Date</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Details</th>
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6}>Loading transactions...</td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={6}>
                  Transaction data could not be loaded.
                </td>
              </tr>
            ) : transactions.length === 0 ? (
              <tr>
                <td colSpan={6}>
                  No transactions match your filters.
                </td>
              </tr>
            ) : (
              transactions.map((transaction) => (
                <tr key={transaction.id}>
                  <td className="transaction-id">
                    TXN-{transaction.id}
                  </td>

                  <td>{transaction.paymentMethod || '—'}</td>

                  <td className="date-cell">
                    {formatDate(transaction.createdAt)}
                  </td>

                  <td className="amount-cell">
                    {formatAmount(
                      transaction.amount,
                      transaction.currency,
                    )}
                  </td>

                  <td>
                    <StatusBadge status={transaction.status} />
                  </td>

                  <td>
                    <button
                      className="details-button"
                      type="button"
                      onClick={() => onSelect(transaction)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="table-footer">
        <span>
          {loading
            ? 'Loading records...'
            : `Showing ${transactions.length} record(s)`}
        </span>

        <span>
          {error
            ? 'Connection issue'
            : loading
              ? 'Please wait'
              : 'Backend response'}
        </span>
      </div>
    </div>
  )
}

function ReconciliationTable({
  records,
  loading,
  error,
  formatAmount,
  formatDate,
}) {
  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Record</th>
            <th>Transaction</th>
            <th>External record</th>
            <th>Internal amount</th>
            <th>External amount</th>
            <th>Internal status</th>
            <th>External status</th>
            <th>Result</th>
            <th>Reconciled at</th>
          </tr>
        </thead>

        <tbody>
          {loading ? (
            <tr>
              <td colSpan={9}>
                Loading reconciliation records...
              </td>
            </tr>
          ) : error ? (
            <tr>
              <td colSpan={9}>
                Reconciliation records could not be loaded.
              </td>
            </tr>
          ) : records.length === 0 ? (
            <tr>
              <td colSpan={9}>
                No reconciliation records match this filter.
              </td>
            </tr>
          ) : (
            records.map((record) => (
              <tr key={record.id}>
                <td>REC-{record.id}</td>

                <td>
                  {record.transactionId != null
                    ? `TXN-${record.transactionId}`
                    : '—'}
                </td>

                <td>{record.externalRecordId ?? '—'}</td>

                <td>
                  {record.internalAmount != null
                    ? formatAmount(record.internalAmount)
                    : '—'}
                </td>

                <td>
                  {record.externalAmount != null
                    ? formatAmount(record.externalAmount)
                    : '—'}
                </td>

                <td>{record.internalStatus || '—'}</td>

                <td>{record.externalStatus || '—'}</td>

                <td>
                  <ReconciliationBadge status={record.status} />
                </td>

                <td className="date-cell">
                  {formatDate(record.reconciledAt)}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

function ReconciliationBadge({ status }) {
  const normalized = String(status || 'UNKNOWN').toUpperCase()

  const styles = {
    MATCHED: {
      background: '#dcfce7',
      color: '#166534',
    },
    MISMATCH: {
      background: '#fee2e2',
      color: '#991b1b',
    },
    MISSING_EXTERNAL: {
      background: '#ffedd5',
      color: '#9a3412',
    },
    MISSING_INTERNAL: {
      background: '#fef3c7',
      color: '#92400e',
    },
  }

  const style = styles[normalized] || {
    background: '#e5e7eb',
    color: '#374151',
  }

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '4px 9px',
        borderRadius: '999px',
        fontSize: '12px',
        fontWeight: 600,
        whiteSpace: 'nowrap',
        background: style.background,
        color: style.color,
      }}
    >
      {normalized.replaceAll('_', ' ')}
    </span>
  )
}

function DetailItem({ label, value }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function MetricCard({ label, value, note, icon, tone }) {
  return (
    <article className="metric-card">
      <div className="metric-top">
        <span>{label}</span>
        <span className={`metric-icon ${tone}`}>{icon}</span>
      </div>

      <div className="metric-value">{value}</div>
      <p className="metric-note">{note}</p>
    </article>
  )
}

function StatusBadge({ status }) {
  const normalizedStatus = String(status || 'UNKNOWN').toUpperCase()

  const statusClasses = {
    SUCCESS: 'success',
    CREATED: 'pending',
    PROCESSING: 'pending',
    PENDING: 'pending',
    FAILED: 'failed',
    REFUNDED: 'failed',
  }

  const displayNames = {
    SUCCESS: 'Success',
    CREATED: 'Created',
    PROCESSING: 'Processing',
    PENDING: 'Pending',
    FAILED: 'Failed',
    REFUNDED: 'Refunded',
  }

  const className = statusClasses[normalizedStatus] || 'pending'
  const displayName = displayNames[normalizedStatus] || normalizedStatus

  return (
    <span className={`status-badge ${className}`}>
      <span className="badge-dot" />
      {displayName}
    </span>
  )
}

function SystemRow({ name, description }) {
  return (
    <div className="system-row">
      <div className="service-indicator" />

      <div className="service-info">
        <strong>{name}</strong>
        <p>{description}</p>
      </div>

      <span className="planned-label">Planned</span>
    </div>
  )
}

export default App