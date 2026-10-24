
const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')

export async function apiRequest(
  path,
  { method = 'GET', token, body, headers = {} } = {},
) {
  const requestHeaders = {
    Accept: 'application/json',
    ...headers,
  }

  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json'
  }

  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    const message = await response.text()

    throw new Error(
      message || `API request failed with status ${response.status}`,
    )
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

export const authApi = {
  login: (credentials) =>
    apiRequest('/auth/login', {
      method: 'POST',
      body: credentials,
    }),

  register: (registration) =>
    apiRequest('/auth/register', {
      method: 'POST',
      body: registration,
    }),
}

export const transactionApi = {
  getAll: (token) =>
    apiRequest('/api/transactions', { token }),

  getById: (id, token) =>
    apiRequest(`/api/transactions/${id}`, { token }),
}

export const reconciliationApi = {
  // Retrieve the latest reconciliation record for a transaction.
  getLatestByTransactionId: (transactionId, token) =>
    apiRequest(`/api/reconciliation/transactions/${transactionId}`, {
      token,
    }),

  // Create a reconciliation record for an internal transaction.
  reconcileTransaction: (transactionId, token) =>
    apiRequest(`/api/reconciliation/transactions/${transactionId}`, {
      method: 'POST',
      token,
    }),

  // Create a reconciliation record for an external payment record.
  reconcileExternalRecord: (externalRecordId, token) =>
    apiRequest(`/api/reconciliation/external-records/${externalRecordId}`, {
      method: 'POST',
      token,
    }),

  // Retrieve reconciliation records by status.
  getByStatus: (status, token) =>
    apiRequest(`/api/reconciliation/status/${encodeURIComponent(status)}`, {
      token,
    }),
}

