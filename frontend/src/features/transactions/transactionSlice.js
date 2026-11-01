import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { transactionApi } from '../../services/api'

// Fetch all transactions (existing functionality)
export const fetchTransactions = createAsyncThunk(
  'transactions/fetchAll',
  async (token, { rejectWithValue }) => {
    try {
      const data = await transactionApi.getAll(token)

      const records = Array.isArray(data)
        ? data
        : Array.isArray(data?.value)
          ? data.value
          : []

      return records
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load transactions.',
      )
    }
  },
)

// Fetch paginated transactions with server-side filtering and sorting
export const fetchTransactionsPage = createAsyncThunk(
  'transactions/fetchPage',
  async (
    {
      page = 0,
      size = 10,
      search = '',
      status = '',
      paymentMethod = '',
      sortBy = 'createdAt',
      direction = 'desc',
      token,
    } = {},
    { rejectWithValue },
  ) => {
    try {
      return await transactionApi.getPage(
        {
          page,
          size,
          search,
          status,
          paymentMethod,
          sortBy,
          direction,
        },
        token,
      )
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load transaction page.',
      )
    }
  },
)

// Fetch a particular transaction
export const fetchTransactionById = createAsyncThunk(
  'transactions/fetchById',
  async ({ id, token }, { rejectWithValue }) => {
    try {
      return await transactionApi.getById(id, token)
    } catch (error) {
      return rejectWithValue(
        error.message || 'Could not load transaction details.',
      )
    }
  },
)

const initialState = {
  // Complete transaction list used by Dashboard and Reconciliation
  items: [],

  // Transactions belonging to the current filtered page
  pageItems: [],

  // Pagination metadata
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,

  loading: false,
  paginationLoading: false,
  error: null,
  selectedTransaction: null,
}

const transactionSlice = createSlice({
  name: 'transactions',
  initialState,

  reducers: {
    clearSelectedTransaction: (state) => {
      state.selectedTransaction = null
    },

    // Update page size and return to first page
    setPageSize: (state, action) => {
      state.size = action.payload
      state.page = 0
    },

    // Update current page; the Dashboard effect fetches that page.
    setPage: (state, action) => {
      state.page = action.payload
    },

    // Reset pagination
    resetPagination: (state) => {
      state.page = 0
      state.size = 10
      state.totalElements = 0
      state.totalPages = 0
      state.first = true
      state.last = true
      state.pageItems = []
    },
  },

  extraReducers: (builder) => {
    builder
      .addCase(fetchTransactions.pending, (state) => {
        state.loading = true
        state.error = null
      })

      .addCase(fetchTransactions.fulfilled, (state, action) => {
        state.loading = false
        state.items = Array.isArray(action.payload)
          ? action.payload
          : []
      })

      .addCase(fetchTransactions.rejected, (state, action) => {
        state.loading = false
        state.error =
          action.payload || 'Unable to load transactions.'
      })

      .addCase(fetchTransactionsPage.pending, (state) => {
        state.paginationLoading = true
        state.error = null
      })

      .addCase(fetchTransactionsPage.fulfilled, (state, action) => {
        state.paginationLoading = false

        const response = action.payload || {}

        state.pageItems = Array.isArray(response.content)
          ? response.content
          : []

        state.page = response.number ?? 0
        state.size = response.size ?? state.size
        state.totalElements = response.totalElements ?? 0
        state.totalPages = response.totalPages ?? 0
        state.first = response.first ?? true
        state.last = response.last ?? true
      })

      .addCase(fetchTransactionsPage.rejected, (state, action) => {
        state.paginationLoading = false
        state.error =
          action.payload || 'Unable to load transaction page.'
      })

      .addCase(fetchTransactionById.fulfilled, (state, action) => {
        if (action.payload && typeof action.payload === 'object') {
          state.selectedTransaction = action.payload
        }
      })
  },
})

export const {
  clearSelectedTransaction,
  setPageSize,
  setPage,
  resetPagination,
} = transactionSlice.actions

export default transactionSlice.reducer
