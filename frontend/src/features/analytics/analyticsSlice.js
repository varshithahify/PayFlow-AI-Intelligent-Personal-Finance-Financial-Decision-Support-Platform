import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'

import { analyticsApi } from '../../services/api'

// ==========================================
// ASYNC THUNKS
// ==========================================

// Fetch overall analytics.
export const fetchAnalyticsOverview = createAsyncThunk(
  'analytics/fetchOverview',
  async (_, { rejectWithValue }) => {
    try {
      return await analyticsApi.getOverview()
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

// Fetch payment trends.
export const fetchPaymentTrends = createAsyncThunk(
  'analytics/fetchPaymentTrends',
  async (_, { rejectWithValue }) => {
    try {
      return await analyticsApi.getPaymentTrends()
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

// Fetch payment status distribution.
export const fetchPaymentStatus = createAsyncThunk(
  'analytics/fetchPaymentStatus',
  async (_, { rejectWithValue }) => {
    try {
      return await analyticsApi.getPaymentStatus()
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

// Fetch gateway performance.
export const fetchGatewayPerformance = createAsyncThunk(
  'analytics/fetchGatewayPerformance',
  async (_, { rejectWithValue }) => {
    try {
      return await analyticsApi.getGatewayPerformance()
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

// ==========================================
// INITIAL STATE
// ==========================================

const initialState = {
  overview: null,
  paymentTrends: [],
  paymentStatus: [],
  gatewayPerformance: [],

  loading: false,
  error: null,
}

// ==========================================
// ANALYTICS SLICE
// ==========================================

const analyticsSlice = createSlice({
  name: 'analytics',

  initialState,

  reducers: {
    clearAnalytics: (state) => {
      state.overview = null
      state.paymentTrends = []
      state.paymentStatus = []
      state.gatewayPerformance = []
      state.loading = false
      state.error = null
    },
  },

  extraReducers: (builder) => {

    // Overview
    builder
      .addCase(fetchAnalyticsOverview.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchAnalyticsOverview.fulfilled, (state, action) => {
        state.loading = false
        state.overview = action.payload
      })
      .addCase(fetchAnalyticsOverview.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload
      })

    // Payment trends
    builder
      .addCase(fetchPaymentTrends.fulfilled, (state, action) => {
        state.paymentTrends = action.payload
      })
      .addCase(fetchPaymentTrends.rejected, (state, action) => {
        state.error = action.payload
      })

    // Payment status
    builder
      .addCase(fetchPaymentStatus.fulfilled, (state, action) => {
        state.paymentStatus = action.payload
      })
      .addCase(fetchPaymentStatus.rejected, (state, action) => {
        state.error = action.payload
      })

    // Gateway performance
    builder
      .addCase(fetchGatewayPerformance.fulfilled, (state, action) => {
        state.gatewayPerformance = action.payload
      })
      .addCase(fetchGatewayPerformance.rejected, (state, action) => {
        state.error = action.payload
      })
  },
})

export const { clearAnalytics } = analyticsSlice.actions

export default analyticsSlice.reducer