const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || ''
).replace(/\/+$/, '')

// Callback for handling expired or invalid authentication.
let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export async function apiRequest(
  path,
  {
    method = 'GET',
    token,
    body,
    headers = {},
    requiresAuth = true,
  } = {},
) {
  const requestHeaders = {
    Accept: 'application/json',
    ...headers,
  }

  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json'
  }

  // Automatically retrieve JWT for authenticated requests.
  const authToken = token || (
    requiresAuth
      ? sessionStorage.getItem('accessToken')
      : null
  )

  if (authToken) {
    requestHeaders.Authorization = `Bearer ${authToken}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    // Automatically handle expired or invalid JWT.
    if (requiresAuth && response.status === 401) {
      unauthorizedHandler?.()
    }

    const message = await response.text()
    const error = new Error(
      message || `API request failed with status ${response.status}`,
    )
    error.status = response.status
    throw error
  }

  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    return response.json()
  }

  return response.text()
}

// Authentication APIs
export const authApi = {
  login: (credentials) =>
    apiRequest('/auth/login', {
      method: 'POST',
      body: credentials,
      requiresAuth: false,
    }),

  register: (registration) =>
    apiRequest('/auth/register', {
      method: 'POST',
      body: registration,
      requiresAuth: false,
    }),
}

// Transaction APIs
export const transactionApi = {
  // Create a transaction record.
  create: (transaction, idempotencyKey, token) =>
    apiRequest('/api/transactions', {
      method: 'POST',
      token,
      body: transaction,
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
    }),

  // Fetch all transactions.
  getAll: (token) =>
    apiRequest('/api/transactions', { token }),

  // Fetch paginated transactions with server-side filters and sorting.
  getPage: (
    {
      page = 0,
      size = 10,
      search = '',
      status = '',
      paymentMethod = '',
      sortBy = 'createdAt',
      direction = 'desc',
    } = {},
    token,
  ) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      sortBy,
      direction,
    })

    if (search.trim()) {
      params.set('search', search.trim())
    }

    if (status) {
      params.set('status', status)
    }

    if (paymentMethod) {
      params.set('paymentMethod', paymentMethod)
    }

    return apiRequest(
      `/api/transactions/page?${params.toString()}`,
      { token },
    )
  },

  // Fetch individual transaction details.
  getById: (id, token) =>
    apiRequest(`/api/transactions/${id}`, { token }),
}

// Reconciliation APIs
export const reconciliationApi = {
  getLatestByTransactionId: (transactionId, token) =>
    apiRequest(`/api/reconciliation/transactions/${transactionId}`, {
      token,
    }),

  reconcileTransaction: (transactionId, token) =>
    apiRequest(`/api/reconciliation/transactions/${transactionId}`, {
      method: 'POST',
      token,
    }),

  reconcileExternalRecord: (externalRecordId, token) =>
    apiRequest(`/api/reconciliation/external-records/${externalRecordId}`, {
      method: 'POST',
      token,
    }),

  getByStatus: (status, token) =>
    apiRequest(`/api/reconciliation/status/${encodeURIComponent(status)}`, {
      token,
    }),
}

// Analytics APIs
export const analyticsApi = {
  getOverview: () =>
    apiRequest('/api/analytics/overview'),

  getPaymentTrends: () =>
    apiRequest('/api/analytics/payment-trends'),

  getPaymentStatus: () =>
    apiRequest('/api/analytics/payment-status'),

  getGatewayPerformance: () =>
    apiRequest('/api/analytics/gateway-performance'),
}

// Settlement Jobs APIs
export const settlementJobApi = {
  // Create a settlement job.
  create: (transactionIds, token) =>
    apiRequest('/api/settlement-jobs', {
      method: 'POST',
      token,
      body: { transactionIds },
    }),

  // Fetch paginated settlement jobs.
  getPage: ({ page = 0, size = 20, status = '' } = {}, token) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    })

    if (status) {
      params.set('status', status)
    }

    return apiRequest(
      `/api/settlement-jobs?${params.toString()}`,
      { token },
    )
  },

  // Fetch individual settlement job.
  getById: (jobId, token) =>
    apiRequest(
      `/api/settlement-jobs/${encodeURIComponent(jobId)}`,
      { token },
    ),

  // Execute a settlement job.
  execute: (jobId, token) =>
    apiRequest(
      `/api/settlement-jobs/${encodeURIComponent(jobId)}/execute`,
      {
        method: 'POST',
        token,
      },
    ),

  // Fetch execution history.
  getHistory: (jobId, token) =>
    apiRequest(
      `/api/settlement-jobs/${encodeURIComponent(jobId)}/history`,
      { token },
    ),
}

// Event Monitoring APIs
export const eventMonitoringApi = {
  // Fetch event overview.
  getOverview: (token) =>
    apiRequest('/api/events/overview', { token }),

  // Fetch paginated processed events with filters.
  getProcessed: (
    {
      page = 0,
      size = 20,
      eventType = '',
      fromDate = '',
      toDate = '',
    } = {},
    token,
  ) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    })

   if (eventType.trim()) {
  params.set('eventType', eventType.trim().toUpperCase())
}

    if (fromDate) {
      params.set('fromDate', fromDate)
    }

    if (toDate) {
      params.set('toDate', toDate)
    }

    return apiRequest(
      `/api/events/processed?${params.toString()}`,
      { token },
    )
  },

  // Fetch paginated failed events with filters.
  getFailed: (
    {
      page = 0,
      size = 20,
      eventType = '',
      fromDate = '',
      toDate = '',
    } = {},
    token,
  ) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    })

    if (eventType.trim()) {
      params.set('eventType', eventType.trim().toUpperCase())
    }

    if (fromDate) {
      params.set('fromDate', fromDate)
    }

    if (toDate) {
      params.set('toDate', toDate)
    }

    return apiRequest(
      `/api/events/failed?${params.toString()}`,
      { token },
    )
  },

  // Fetch individual processed event details.
  getProcessedById: (eventId, token) =>
    apiRequest(
      `/api/events/processed/${encodeURIComponent(eventId)}`,
      { token },
    ),

  // Fetch individual failed event details.
  getFailedById: (eventId, token) =>
    apiRequest(
      `/api/events/failed/${encodeURIComponent(eventId)}`,
      { token },
    ),
}