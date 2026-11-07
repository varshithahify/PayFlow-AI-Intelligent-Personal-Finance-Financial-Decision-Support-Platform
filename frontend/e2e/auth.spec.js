import { test, expect } from '@playwright/test'

test.describe('PayFlow AI Authentication', () => {

  test('should display the login page', async ({ page }) => {
    await page.goto('/')

    await expect(
      page.getByRole('heading', { name: 'Sign in to PayFlow AI' })
    ).toBeVisible()

    await expect(
      page.getByLabel('Email address')
    ).toBeVisible()

    await expect(
      page.getByLabel('Password')
    ).toBeVisible()

    await expect(
      page.getByRole('button', { name: 'Sign in' })
    ).toBeVisible()
  })

  test('should navigate between transaction pages', async ({ page }) => {
    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.route('**/api/**', async (route) => {
      const url = new URL(route.request().url())

      let response = []

      if (url.pathname.endsWith('/analytics/overview')) {
        response = {
          totalTransactions: 100,
          successfulTransactions: 80,
          pendingTransactions: 5,
          failedTransactions: 10,
          refundedTransactions: 5,
          successRate: 80,
          paymentVolumeByCurrency: { INR: 50000 },
        }
      } else if (url.pathname.endsWith('/transactions/page')) {
        const requestedPage = Number(url.searchParams.get('page') || 0)

        response = {
          content: [
            {
              id: requestedPage === 0 ? 1001 : 1011,
              paymentMethod: 'UPI',
              amount: 1500,
              currency: 'INR',
              status: 'SUCCESS',
              createdAt: '2026-11-07T10:00:00',
            },
          ],
          number: requestedPage,
          size: 10,
          totalElements: 20,
          totalPages: 2,
          first: requestedPage === 0,
          last: requestedPage === 1,
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(response),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await page.getByRole('navigation', { name: 'Main navigation' })
      .getByRole('button', { name: /Transactions/ })
      .click()

    await expect(
      page.getByRole('heading', { name: 'Transaction explorer' })
    ).toBeVisible()

    await expect(page.getByText('TXN-1001')).toBeVisible()
    await expect(
      page.getByText('Page 1 of 2', { exact: true })
    ).toBeVisible()

    const secondPageResponse = page.waitForResponse((response) => {
      const url = new URL(response.url())

      return (
        url.pathname.endsWith('/transactions/page') &&
        url.searchParams.get('page') === '1'
      )
    })

    await page.getByRole('button', { name: 'Next' }).click()

    const response = await secondPageResponse
    expect(response.status()).toBe(200)

    await expect(page.getByText('TXN-1011')).toBeVisible()
    await expect(
      page.getByText('Page 2 of 2', { exact: true })
    ).toBeVisible()

    const firstPageResponse = page.waitForResponse((response) => {
      const url = new URL(response.url())

      return (
        url.pathname.endsWith('/transactions/page') &&
        url.searchParams.get('page') === '0'
      )
    })

    await page.getByRole('button', { name: 'Previous' }).click()

    const previousResponse = await firstPageResponse
    expect(previousResponse.status()).toBe(200)

    await expect(page.getByText('TXN-1001')).toBeVisible()
    await expect(
      page.getByText('Page 1 of 2', { exact: true })
    ).toBeVisible()
  })

  test('should open the registration form', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(
      page.getByRole('heading', { name: 'Create your account' })
    ).toBeVisible()

    await expect(
      page.getByLabel('Full name')
    ).toBeVisible()
  })

  test('should display reconciliation records and filter by status', async ({ page }) => {

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.route('**/api/**', async (route) => {
      const url = new URL(route.request().url())

      let response = []

      if (url.pathname.endsWith('/analytics/overview')) {
        response = {
          totalTransactions: 100,
          successfulTransactions: 80,
          pendingTransactions: 5,
          failedTransactions: 10,
          refundedTransactions: 5,
          successRate: 80,
          paymentVolumeByCurrency: { INR: 50000 },
        }
      } else if (url.pathname.endsWith('/transactions/page')) {
        response = {
          content: [
            {
              id: 1001,
              paymentMethod: 'UPI',
              amount: 1500,
              currency: 'INR',
              status: 'SUCCESS',
              createdAt: '2026-11-07T10:00:00',
            },
          ],
          number: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }
      } else if (url.pathname.endsWith('/reconciliation/status/MATCHED')) {
        response = [
          {
            id: 501,
            transactionId: 1001,
            externalRecordId: 'EXT-501',
            internalAmount: 1500,
            externalAmount: 1500,
            internalStatus: 'SUCCESS',
            externalStatus: 'SUCCESS',
            status: 'MATCHED',
            reconciledAt: '2026-11-07T10:30:00',
          },
        ]
      } else if (url.pathname.endsWith('/reconciliation/status/MISMATCH')) {
        response = [
          {
            id: 502,
            transactionId: 1002,
            externalRecordId: 'EXT-502',
            internalAmount: 2000,
            externalAmount: 1800,
            internalStatus: 'SUCCESS',
            externalStatus: 'SUCCESS',
            status: 'MISMATCH',
            reconciledAt: '2026-11-07T10:20:00',
          },
        ]
      } else if (
        url.pathname.endsWith('/reconciliation/status/MISSING_EXTERNAL') ||
        url.pathname.endsWith('/reconciliation/status/MISSING_INTERNAL')
      ) {
        response = []
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(response),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await page.getByRole('navigation', { name: 'Main navigation' })
      .getByRole('button', { name: /Reconciliation/ })
      .click()

    await expect(
      page.getByRole('heading', { name: 'Reconciliation center' })
    ).toBeVisible()

    await expect(page.getByText('REC-501')).toBeVisible()
    await expect(page.getByText('REC-502')).toBeVisible()

    await expect(page.getByText('Total reconciliations')).toBeVisible()

    await expect(
      page.locator('.metrics-grid').getByText('Matched', { exact: true })
    ).toBeVisible()

    await expect(
      page.locator('.metrics-grid').getByText('Mismatches', { exact: true })
    ).toBeVisible()

    await page.getByLabel('Filter reconciliation status')
      .selectOption('MATCHED')

    await expect(page.getByText('REC-501')).toBeVisible()
    await expect(page.getByText('REC-502')).not.toBeVisible()

    await page.getByLabel('Filter reconciliation status')
      .selectOption('MISMATCH')

    await expect(page.getByText('REC-502')).toBeVisible()
    await expect(page.getByText('REC-501')).not.toBeVisible()

    await page.getByLabel('Filter reconciliation status')
      .selectOption('ALL')

    await expect(page.getByText('REC-501')).toBeVisible()
    await expect(page.getByText('REC-502')).toBeVisible()
  })

  test('should reconcile a transaction and refresh records', async ({ page }) => {
    let transactionFetchCount = 0
    let reconciliationFetchCount = 0

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.route('**/api/**', async (route) => {
      const url = new URL(route.request().url())
      const method = route.request().method()

      let response = []

      if (url.pathname.endsWith('/analytics/overview')) {
        response = {
          totalTransactions: 100,
          successfulTransactions: 80,
          pendingTransactions: 5,
          failedTransactions: 10,
          refundedTransactions: 5,
          successRate: 80,
          paymentVolumeByCurrency: { INR: 50000 },
        }
      } else if (
        url.pathname === '/api/transactions' &&
        method === 'GET'
      ) {
        transactionFetchCount++

        response = [
          {
            id: 1001,
            paymentMethod: 'UPI',
            amount: 1500,
            currency: 'INR',
            status: 'SUCCESS',
            createdAt: '2026-11-07T10:00:00',
          },
        ]
      } else if (
        url.pathname === '/api/reconciliation/transactions/1001' &&
        method === 'POST'
      ) {
        response = {
          id: 501,
          transactionId: 1001,
          status: 'MATCHED',
        }
      } else if (url.pathname.includes('/reconciliation/status/')) {
        reconciliationFetchCount++

        if (url.pathname.endsWith('/MATCHED')) {
          response = [
            {
              id: 501,
              transactionId: 1001,
              externalRecordId: 'EXT-501',
              internalAmount: 1500,
              externalAmount: 1500,
              internalStatus: 'SUCCESS',
              externalStatus: 'SUCCESS',
              status: 'MATCHED',
              reconciledAt: '2026-11-07T10:30:00',
            },
          ]
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(response),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await page.getByRole('navigation', { name: 'Main navigation' })
      .getByRole('button', { name: /Reconciliation/ })
      .click()

    await expect(
      page.getByRole('heading', { name: 'Reconciliation center' })
    ).toBeVisible()

    await expect(
      page.getByRole('button', { name: 'Reconcile', exact: true })
    ).toBeVisible()

    const reconcileResponsePromise = page.waitForResponse((response) => (
      new URL(response.url()).pathname ===
        '/api/reconciliation/transactions/1001' &&
      response.request().method() === 'POST'
    ))

    await page.getByRole('button', { name: 'Reconcile', exact: true }).click()

    const reconcileResponse = await reconcileResponsePromise
    expect(reconcileResponse.status()).toBe(200)

    await expect(page.getByRole('status')).toContainText(
      'Transaction 1001 reconciled successfully'
    )

    await expect(page.getByRole('status')).toContainText('MATCHED')

    await expect.poll(() => transactionFetchCount).toBeGreaterThan(1)

    await expect.poll(() => reconciliationFetchCount).toBeGreaterThan(4)
  })

  test('should authenticate successfully and open the dashboard', async ({ page }) => {

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(
      page.getByRole('heading', { name: 'Your payment command center' })
    ).toBeVisible()

    await expect(
      page.getByText('test@payflow.com')
    ).toBeVisible()

    const token = await page.evaluate(() =>
      sessionStorage.getItem('accessToken')
    )

    expect(token).toBe('e2e-test-jwt-token')
  })

  test('should prevent registration with empty required fields', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('button', { name: 'Create account' }).click()

    const nameInput = page.getByLabel('Full name')

    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(nameInput).toHaveJSProperty(
      'validity.valueMissing',
      true,
    )
  })

  test('should display an error for invalid login', async ({ page }) => {
    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'text/plain',
        body: 'Invalid email or password',
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('wrong@example.com')
    await page.getByLabel('Password').fill('wrongpassword')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
  })

  test('should navigate through dashboard pages and sign out', async ({ page }) => {

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.route('**/api/**', async (route) => {
      const url = new URL(route.request().url())

      let response = []

      if (url.pathname.endsWith('/analytics/overview')) {
        response = {
          totalTransactions: 100,
          successfulTransactions: 80,
          pendingTransactions: 5,
          failedTransactions: 10,
          refundedTransactions: 5,
          successRate: 80,
          paymentVolumeByCurrency: { INR: 50000 },
        }
      } else if (url.pathname.endsWith('/transactions/page')) {
        response = {
          content: [],
          number: 0,
          size: 10,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(response),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(
      page.getByRole('heading', { name: 'Your payment command center' })
    ).toBeVisible()

    await page.getByRole('navigation', { name: 'Main navigation' })
      .getByRole('button', { name: /Transactions/ })
      .click()

    await expect(
      page.getByRole('heading', { name: 'Transaction explorer' })
    ).toBeVisible()

    await page.getByRole('button', { name: 'Reconciliation' }).click()

    await expect(
      page.getByRole('heading', { name: 'Reconciliation center' })
    ).toBeVisible()

    await expect(
      page.getByLabel('Filter reconciliation status')
    ).toBeVisible()

    await page.getByRole('button', { name: 'Sign out' }).click()

    await expect(
      page.getByRole('heading', { name: 'Sign in to PayFlow AI' })
    ).toBeVisible()
  })

  test('should search, filter and sort transactions using backend parameters', async ({ page }) => {

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.route('**/api/**', async (route) => {
      const url = new URL(route.request().url())

      let response = []

      if (url.pathname.endsWith('/analytics/overview')) {
        response = {
          totalTransactions: 100,
          successfulTransactions: 80,
          pendingTransactions: 5,
          failedTransactions: 10,
          refundedTransactions: 5,
          successRate: 80,
          paymentVolumeByCurrency: { INR: 50000 },
        }
      } else if (url.pathname.endsWith('/transactions/page')) {
        response = {
          content: [
            {
              id: 1001,
              paymentMethod: 'UPI',
              amount: 1500,
              currency: 'INR',
              status: 'SUCCESS',
              createdAt: '2026-11-07T10:00:00',
            },
          ],
          number: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(response),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await page.getByRole('navigation', { name: 'Main navigation' })
      .getByRole('button', { name: /Transactions/ })
      .click()

    await expect(
      page.getByRole('heading', { name: 'Transaction explorer' })
    ).toBeVisible()

    await page.getByLabel('Filter by transaction status')
      .selectOption('SUCCESS')

    await page.getByLabel('Filter by payment method')
      .selectOption('UPI')

    await page.getByLabel('Sort transactions by')
      .selectOption('amount')

    await page.getByLabel('Sort direction')
      .selectOption('asc')

    const filteredResponse = page.waitForResponse((response) => {
      const url = new URL(response.url())

      return (
        url.pathname.endsWith('/transactions/page') &&
        url.searchParams.get('search') === 'TXN-1001' &&
        url.searchParams.get('status') === 'SUCCESS' &&
        url.searchParams.get('paymentMethod') === 'UPI' &&
        url.searchParams.get('sortBy') === 'amount' &&
        url.searchParams.get('direction') === 'asc'
      )
    })

    await page.getByLabel('Search transactions').fill('TXN-1001')

    const response = await filteredResponse

    expect(response.status()).toBe(200)

    await expect(page.getByText('TXN-1001')).toBeVisible()
  })

  test('should open and close transaction details', async ({ page }) => {

    await page.route('**/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'e2e-test-jwt-token',
        }),
      })
    })

    await page.route('**/api/**', async (route) => {
      const url = new URL(route.request().url())

      let response = []

      if (url.pathname.endsWith('/analytics/overview')) {
        response = {
          totalTransactions: 100,
          successfulTransactions: 80,
          pendingTransactions: 5,
          failedTransactions: 10,
          refundedTransactions: 5,
          successRate: 80,
          paymentVolumeByCurrency: { INR: 50000 },
        }
      } else if (url.pathname.endsWith('/transactions/page')) {
        response = {
          content: [
            {
              id: 1001,
              paymentMethod: 'UPI',
              amount: 1500,
              currency: 'INR',
              status: 'SUCCESS',
              createdAt: '2026-11-07T10:00:00',
            },
          ],
          number: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        }
      } else if (url.pathname.endsWith('/transactions/1001')) {
        response = {
          id: 1001,
          paymentMethod: 'UPI',
          amount: 1500,
          currency: 'INR',
          status: 'SUCCESS',
          createdAt: '2026-11-07T10:00:00',
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(response),
      })
    })

    await page.goto('/')

    await page.getByLabel('Email address').fill('test@payflow.com')
    await page.getByLabel('Password').fill('password123')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await page.getByRole('navigation', { name: 'Main navigation' })
      .getByRole('button', { name: /Transactions/ })
      .click()

    await expect(
      page.getByRole('heading', { name: 'Transaction explorer' })
    ).toBeVisible()

    await expect(page.getByText('TXN-1001')).toBeVisible()

    await page.getByRole('button', { name: 'View', exact: true }).click()

    const dialog = page.getByRole('dialog')

    await expect(dialog).toBeVisible()

    await expect(
      dialog.getByRole('heading', { name: 'TXN-1001' })
    ).toBeVisible()

    await page.getByLabel('Close transaction details').click()

    await expect(dialog).not.toBeVisible()
  })

})