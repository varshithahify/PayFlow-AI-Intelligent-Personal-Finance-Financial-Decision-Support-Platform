import { configureStore } from '@reduxjs/toolkit'

import authReducer from '../features/auth/authSlice'
import transactionReducer from '../features/transactions/transactionSlice'
import analyticsReducer from '../features/analytics/analyticsSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    transactions: transactionReducer,
    analytics: analyticsReducer,
  },
})