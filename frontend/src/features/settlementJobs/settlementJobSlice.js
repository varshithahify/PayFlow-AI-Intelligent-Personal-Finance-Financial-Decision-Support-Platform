import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { settlementJobApi } from '../../services/api'

// Fetch paginated settlement jobs
export const fetchSettlementJobs = createAsyncThunk(
  'settlementJobs/fetchPage',
  async (
    { page = 0, size = 20, status = '', token },
    { rejectWithValue },
  ) => {
    try {
      return await settlementJobApi.getPage(
        { page, size, status },
        token,
      )
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load settlement jobs.',
      )
    }
  },
)

// Create settlement job
export const createSettlementJob = createAsyncThunk(
  'settlementJobs/create',
  async ({ transactionIds, token }, { rejectWithValue }) => {
    try {
      return await settlementJobApi.create(transactionIds, token)
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to create settlement job.',
      )
    }
  },
)

// Fetch individual settlement job
export const fetchSettlementJobById = createAsyncThunk(
  'settlementJobs/fetchById',
  async ({ jobId, token }, { rejectWithValue }) => {
    try {
      return await settlementJobApi.getById(jobId, token)
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load settlement job details.',
      )
    }
  },
)

// Execute settlement job
export const executeSettlementJob = createAsyncThunk(
  'settlementJobs/execute',
  async ({ jobId, token }, { rejectWithValue }) => {
    try {
      return await settlementJobApi.execute(jobId, token)
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to execute settlement job.',
      )
    }
  },
)

// Fetch execution history
export const fetchSettlementJobHistory = createAsyncThunk(
  'settlementJobs/fetchHistory',
  async ({ jobId, token }, { rejectWithValue }) => {
    try {
      return await settlementJobApi.getHistory(jobId, token)
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load execution history.',
      )
    }
  },
)

const initialState = {
  items: [],
  selectedJob: null,
  executionHistory: [],

  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,

  loading: false,
  creating: false,
  executing: false,
  detailsLoading: false,
  historyLoading: false,

  error: null,
  creationError: null,
  executionError: null,
  detailsError: null,
  historyError: null,
}

const settlementJobSlice = createSlice({
  name: 'settlementJobs',
  initialState,

  reducers: {
    setSettlementJobPage: (state, action) => {
      state.page = action.payload
    },

    setSettlementJobPageSize: (state, action) => {
      state.size = action.payload
      state.page = 0
    },

    clearSelectedSettlementJob: (state) => {
      state.selectedJob = null
      state.executionHistory = []
      state.detailsError = null
      state.historyError = null
    },

    resetSettlementJobState: () => initialState,
  },

  extraReducers: (builder) => {
    builder
      // Fetch page
      .addCase(fetchSettlementJobs.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchSettlementJobs.fulfilled, (state, action) => {
        state.loading = false

        const response = action.payload || {}

        state.items = Array.isArray(response.content)
          ? response.content
          : []

        state.page = response.number ?? 0
        state.size = response.size ?? state.size
        state.totalElements = response.totalElements ?? 0
        state.totalPages = response.totalPages ?? 0
        state.first = response.first ?? true
        state.last = response.last ?? true
      })
      .addCase(fetchSettlementJobs.rejected, (state, action) => {
        state.loading = false
        state.error =
          action.payload || 'Unable to load settlement jobs.'
      })

      // Create job
      .addCase(createSettlementJob.pending, (state) => {
        state.creating = true
        state.creationError = null
      })
      .addCase(createSettlementJob.fulfilled, (state) => {
        state.creating = false
      })
      .addCase(createSettlementJob.rejected, (state, action) => {
        state.creating = false
        state.creationError =
          action.payload || 'Unable to create settlement job.'
      })

      // Fetch details
      .addCase(fetchSettlementJobById.pending, (state) => {
        state.detailsLoading = true
        state.detailsError = null
      })
      .addCase(fetchSettlementJobById.fulfilled, (state, action) => {
        state.detailsLoading = false
        state.selectedJob = action.payload
      })
      .addCase(fetchSettlementJobById.rejected, (state, action) => {
        state.detailsLoading = false
        state.detailsError =
          action.payload || 'Unable to load settlement job details.'
      })

      // Execute job
      .addCase(executeSettlementJob.pending, (state) => {
        state.executing = true
        state.executionError = null
      })
      .addCase(executeSettlementJob.fulfilled, (state, action) => {
        state.executing = false
        state.selectedJob = action.payload
      })
      .addCase(executeSettlementJob.rejected, (state, action) => {
        state.executing = false
        state.executionError =
          action.payload || 'Unable to execute settlement job.'
      })

      // Execution history
      .addCase(fetchSettlementJobHistory.pending, (state) => {
        state.historyLoading = true
        state.historyError = null
      })
      .addCase(fetchSettlementJobHistory.fulfilled, (state, action) => {
        state.historyLoading = false
        state.executionHistory = Array.isArray(action.payload)
          ? action.payload
          : []
      })
      .addCase(fetchSettlementJobHistory.rejected, (state, action) => {
        state.historyLoading = false
        state.historyError =
          action.payload || 'Unable to load execution history.'
      })
  },
})

export const {
  setSettlementJobPage,
  setSettlementJobPageSize,
  clearSelectedSettlementJob,
  resetSettlementJobState,
} = settlementJobSlice.actions

export default settlementJobSlice.reducer