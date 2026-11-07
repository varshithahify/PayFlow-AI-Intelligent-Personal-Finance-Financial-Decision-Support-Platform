import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { configureStore } from '@reduxjs/toolkit'

const {
  mockLogin,
  mockRegister,
  mockGetAll,
  mockGetPage,
  mockGetById,
  mockGetOverview,
  mockGetPaymentTrends,
  mockGetPaymentStatus,
  mockGetGatewayPerformance,
  mockGetByStatus,
} = vi.hoisted(() => ({
  mockLogin: vi.fn(),
  mockRegister: vi.fn(),
  mockGetAll: vi.fn(),
  mockGetPage: vi.fn(),
  mockGetById: vi.fn(),
  mockGetOverview: vi.fn(),
  mockGetPaymentTrends: vi.fn(),
  mockGetPaymentStatus: vi.fn(),
  mockGetGatewayPerformance: vi.fn(),
  mockGetByStatus: vi.fn(),
}))

vi.mock('./services/api', () => ({
  authApi: {
    login: mockLogin,
    register: mockRegister,
  },
  transactionApi: {
    getAll: mockGetAll,
    getPage: mockGetPage,
    getById: mockGetById,
  },
  analyticsApi: {
    getOverview: mockGetOverview,
    getPaymentTrends: mockGetPaymentTrends,
    getPaymentStatus: mockGetPaymentStatus,
    getGatewayPerformance: mockGetGatewayPerformance,
  },
  reconciliationApi: {
    getByStatus: mockGetByStatus,
    getLatestByTransactionId: vi.fn(),
    reconcileTransaction: vi.fn(),
    reconcileExternalRecord: vi.fn(),
  },
}))

import App from './App'
import authReducer from './features/auth/authSlice'
import transactionReducer from './features/transactions/transactionSlice'
import analyticsReducer from './features/analytics/analyticsSlice'

function renderApp(preloadedState) {
  const store = configureStore({
    reducer: {
      auth: authReducer,
      transactions: transactionReducer,
      analytics: analyticsReducer,
    },
    preloadedState,
  })

  return {
    store,
    ...render(
      <Provider store={store}>
        <App />
      </Provider>,
    ),
  }
}

function renderAuthenticatedApp() {
  return renderApp({
    auth: {
      ...authReducer(undefined, { type: '@@INIT' }),
      token: 'test-jwt-token',
      userEmail: 'user@example.com',
      isAuthenticated: true,
    },
    transactions: transactionReducer(undefined, {
      type: '@@INIT',
    }),
    analytics: analyticsReducer(undefined, {
      type: '@@INIT',
    }),
  })
}

function getNavigationButton(label) {
  const navigation = screen.getByRole('navigation', {
    name: 'Main navigation',
  })

  return within(navigation)
    .getByText(label)
    .closest('button')
}

describe('App UI', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()

    mockGetAll.mockResolvedValue([])


mockGetPage.mockImplementation(async (params) => ({
  content: [],
  number: params?.page ?? 0,
  size: params?.size ?? 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}))


    mockGetById.mockResolvedValue(null)

    mockGetOverview.mockResolvedValue({})
    mockGetPaymentTrends.mockResolvedValue([])
    mockGetPaymentStatus.mockResolvedValue([])
    mockGetGatewayPerformance.mockResolvedValue([])

    mockGetByStatus.mockResolvedValue([])
  })

  it('should open Reconciliation from the Overview panel', async () => {
  const user = userEvent.setup()

  renderAuthenticatedApp()

  expect(
    await screen.findByText('Your payment command center'),
  ).toBeInTheDocument()

  await user.click(
    screen.getByRole('button', {
      name: /open reconciliation/i,
    }),
  )

  expect(
    await screen.findByRole('heading', {
      name: 'Reconciliation',
    }),
  ).toBeInTheDocument()
})

it('should open Transactions using the Overview panel button', async () => {
  const user = userEvent.setup()

  renderAuthenticatedApp()

  // Confirm Overview is displayed.
  expect(
    await screen.findByText('Your payment command center'),
  ).toBeInTheDocument()

  // Click the Overview panel button.
  await user.click(
    screen.getByRole('button', {
      name: /view transactions/i,
    }),
  )

  // Verify Transactions page opens.
  expect(
    await screen.findByRole('heading', {
      name: 'Transaction explorer',
    }),
  ).toBeInTheDocument()

  expect(
    screen.getByLabelText('Search transactions'),
  ).toBeInTheDocument()
})

it('should open Transactions using View all in Recent transactions', async () => {
  const user = userEvent.setup()

  renderAuthenticatedApp()

  expect(
    await screen.findByText('Recent transactions'),
  ).toBeInTheDocument()

  await user.click(
    screen.getByRole('button', {
      name: /view all/i,
    }),
  )

  expect(
    await screen.findByRole('heading', {
      name: 'Transaction explorer',
    }),
  ).toBeInTheDocument()
})


  // Authentication tests

  it('should display the login screen initially', () => {
    renderApp()

    expect(
      screen.getByRole('heading', {
        name: 'Sign in to PayFlow AI',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText('Email address'),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText('Password'),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', { name: /sign in/i }),
    ).toBeInTheDocument()
  })

  it('should switch from login to registration', async () => {
    const user = userEvent.setup()

    renderApp()

    await user.click(
      screen.getByRole('button', { name: 'Create account' }),
    )

    expect(
      screen.getByRole('heading', {
        name: 'Create your account',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText('Full name'),
    ).toBeInTheDocument()
  })

  it('should display an error when login fails', async () => {
    const user = userEvent.setup()

    mockLogin.mockRejectedValue(
      new Error('Invalid email or password'),
    )

    renderApp()

    await user.type(
      screen.getByLabelText('Email address'),
      'user@example.com',
    )

    await user.type(
      screen.getByLabelText('Password'),
      'wrongpassword',
    )

    await user.click(
      screen.getByRole('button', { name: /sign in/i }),
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent('Invalid email or password')
  })

  it('should authenticate successfully after login', async () => {
    const user = userEvent.setup()

    mockLogin.mockResolvedValue({
      token: 'test-jwt-token',
    })

    renderApp()

    await user.type(
      screen.getByLabelText('Email address'),
      'user@example.com',
    )

    await user.type(
      screen.getByLabelText('Password'),
      'password123',
    )

    await user.click(
      screen.getByRole('button', { name: /sign in/i }),
    )

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'password123',
      })
    })

    await waitFor(() => {
      expect(sessionStorage.getItem('accessToken'))
        .toBe('test-jwt-token')

      expect(sessionStorage.getItem('userEmail'))
        .toBe('user@example.com')
    })

    expect(
      screen.queryByRole('heading', {
        name: 'Sign in to PayFlow AI',
      }),
    ).not.toBeInTheDocument()
  })

  it('should display an error when the server does not return a token', async () => {
    const user = userEvent.setup()

    mockLogin.mockResolvedValue({
      message: 'Login successful',
    })

    renderApp()

    await user.type(
      screen.getByLabelText('Email address'),
      'user@example.com',
    )

    await user.type(
      screen.getByLabelText('Password'),
      'password123',
    )

    await user.click(
      screen.getByRole('button', { name: /sign in/i }),
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'The server did not return an authentication token.',
    )
  })

  it('should prevent registration with a short password', async () => {
    const user = userEvent.setup()

    renderApp()

    await user.click(
      screen.getByRole('button', { name: 'Create account' }),
    )

    await user.type(
      screen.getByLabelText('Full name'),
      'Test User',
    )

    await user.type(
      screen.getByLabelText('Email address'),
      'user@example.com',
    )

    const passwordInput = screen.getByLabelText('Password')

    await user.type(passwordInput, '123')

    expect(passwordInput).toHaveAttribute('minlength', '6')

    await user.click(
      screen.getByRole('button', { name: 'Create account' }),
    )

    expect(mockRegister).not.toHaveBeenCalled()
  })

  it('should register successfully and return to login', async () => {
    const user = userEvent.setup()

    mockRegister.mockResolvedValue({
      message: 'Account created',
    })

    renderApp()

    await user.click(
      screen.getByRole('button', { name: 'Create account' }),
    )

    await user.type(
      screen.getByLabelText('Full name'),
      'Test User',
    )

    await user.type(
      screen.getByLabelText('Email address'),
      'user@example.com',
    )

    await user.type(
      screen.getByLabelText('Password'),
      'password123',
    )

    await user.click(
      screen.getByRole('button', { name: 'Create account' }),
    )

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        name: 'Test User',
        email: 'user@example.com',
        password: 'password123',
      })
    })

    expect(
      await screen.findByRole('status'),
    ).toHaveTextContent(
      'Account created successfully. You can now sign in.',
    )

    expect(
      screen.getByRole('heading', {
        name: 'Sign in to PayFlow AI',
      }),
    ).toBeInTheDocument()
  })

  // Dashboard tests

  it('should display the authenticated Dashboard', async () => {
    renderAuthenticatedApp()

    expect(
      await screen.findByText('Your payment command center'),
    ).toBeInTheDocument()

    expect(
      getNavigationButton('Transactions'),
    ).toBeInTheDocument()

    expect(
      getNavigationButton('Reconciliation'),
    ).toBeInTheDocument()

    await waitFor(() => {
      expect(mockGetAll).toHaveBeenCalled()
      expect(mockGetOverview).toHaveBeenCalled()
    })
  })

  it('should navigate to the Transactions page', async () => {
    const user = userEvent.setup()

    renderAuthenticatedApp()

    await user.click(
      getNavigationButton('Transactions'),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Transaction explorer',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText('Search transactions'),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText('Filter by transaction status'),
    ).toBeInTheDocument()
  })

  it('should navigate to the Reconciliation page', async () => {
    const user = userEvent.setup()

    renderAuthenticatedApp()

    await user.click(
      getNavigationButton('Reconciliation'),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Reconciliation center',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', { name: /refresh/i }),
    ).toBeInTheDocument()
  })

  it('should update the transaction search field', async () => {
    const user = userEvent.setup()

    renderAuthenticatedApp()

    await user.click(
      getNavigationButton('Transactions'),
    )

    const searchInput = await screen.findByLabelText(
      'Search transactions',
    )

    await user.type(searchInput, 'PAY-1001')

    expect(searchInput).toHaveValue('PAY-1001')
  })

  it('should update the transaction status filter', async () => {
    const user = userEvent.setup()

    renderAuthenticatedApp()

    await user.click(
      getNavigationButton('Transactions'),
    )

    const statusFilter = await screen.findByLabelText(
      'Filter by transaction status',
    )

    await user.selectOptions(statusFilter, 'SUCCESS')

    expect(statusFilter).toHaveValue('SUCCESS')
  })

  it('should sign out and return to the login screen', async () => {
  const user = userEvent.setup()

  sessionStorage.setItem('accessToken', 'test-jwt-token')
  sessionStorage.setItem('userEmail', 'user@example.com')

  renderAuthenticatedApp()

  await user.click(
    screen.getByRole('button', { name: /sign out/i }),
  )

  expect(
    await screen.findByRole('heading', {
      name: 'Sign in to PayFlow AI',
    }),
  ).toBeInTheDocument()

  expect(
    screen.queryByText('Your payment command center'),
  ).not.toBeInTheDocument()

  expect(sessionStorage.getItem('accessToken')).toBeNull()
  expect(sessionStorage.getItem('userEmail')).toBeNull()
})

it('should refresh transactions when Refresh is clicked', async () => {
  const user = userEvent.setup()

  renderAuthenticatedApp()

  await user.click(getNavigationButton('Transactions'))

  await waitFor(() => {
    expect(mockGetAll).toHaveBeenCalled()
  })

  const initialCallCount = mockGetAll.mock.calls.length

  await user.click(
    screen.getByRole('button', { name: /refresh/i }),
  )

  await waitFor(() => {
    expect(mockGetAll.mock.calls.length).toBeGreaterThan(
      initialCallCount,
    )
  })
})

it('should update transaction page size', async () => {
  const user = userEvent.setup()

  renderAuthenticatedApp()

  await user.click(getNavigationButton('Transactions'))

  const pageSizeSelect = screen.getByLabelText('Rows per page')

  await waitFor(() => {
    expect(pageSizeSelect).toBeEnabled()
  })

  await user.selectOptions(pageSizeSelect, '20')

  // Verify the selected page size.
  await waitFor(() => {
    expect(pageSizeSelect).toHaveValue('20')
  })

  // Verify API parameters and authentication token.
  await waitFor(() => {
    expect(mockGetPage).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        size: 20,
      }),
      'test-jwt-token',
    )
  })
})
})
