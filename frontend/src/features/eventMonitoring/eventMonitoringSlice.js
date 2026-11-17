import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { eventMonitoringApi } from '../../services/api'

// Fetch event overview
export const fetchEventOverview = createAsyncThunk(
  'eventMonitoring/fetchOverview',
  async (token, { rejectWithValue }) => {
    try {
      return await eventMonitoringApi.getOverview(token)
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load event overview.',
      )
    }
  },
)

// Fetch processed events
export const fetchProcessedEvents = createAsyncThunk(
  'eventMonitoring/fetchProcessed',
  async (
    {
      page = 0,
      size = 20,
      eventType = '',
      fromDate = '',
      toDate = '',
      token,
    },
    { rejectWithValue },
  ) => {
    try {
      return await eventMonitoringApi.getProcessed(
        { page, size, eventType, fromDate, toDate },
        token,
      )
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load processed events.',
      )
    }
  },
)

// Fetch failed events
export const fetchFailedEvents = createAsyncThunk(
  'eventMonitoring/fetchFailed',
  async (
    {
      page = 0,
      size = 20,
      eventType = '',
      fromDate = '',
      toDate = '',
      token,
    },
    { rejectWithValue },
  ) => {
    try {
      return await eventMonitoringApi.getFailed(
        { page, size, eventType, fromDate, toDate },
        token,
      )
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load failed events.',
      )
    }
  },
)

// Fetch processed event details
export const fetchProcessedEventById = createAsyncThunk(
  'eventMonitoring/fetchProcessedById',
  async ({ eventId, token }, { rejectWithValue }) => {
    try {
      return {
        event: await eventMonitoringApi.getProcessedById(eventId, token),
        eventCategory: 'PROCESSED',
      }
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load processed event details.',
      )
    }
  },
)

// Fetch failed event details
export const fetchFailedEventById = createAsyncThunk(
  'eventMonitoring/fetchFailedById',
  async ({ eventId, token }, { rejectWithValue }) => {
    try {
      return {
        event: await eventMonitoringApi.getFailedById(eventId, token),
        eventCategory: 'FAILED',
      }
    } catch (error) {
      return rejectWithValue(
        error.message || 'Unable to load failed event details.',
      )
    }
  },
)

const initialState = {
  overview: {
    totalEvents: 0,
    totalProcessedEvents: 0,
    totalFailedEvents: 0,
  },

  processedEvents: [],
  failedEvents: [],

  processedPage: 0,
  processedSize: 20,
  processedTotalElements: 0,
  processedTotalPages: 0,

  failedPage: 0,
  failedSize: 20,
  failedTotalElements: 0,
  failedTotalPages: 0,

  selectedEvent: null,
  selectedEventCategory: null,

  overviewLoading: false,
  processedLoading: false,
  failedLoading: false,
  detailsLoading: false,

  overviewError: null,
  processedError: null,
  failedError: null,
  detailsError: null,
}

const eventMonitoringSlice = createSlice({
  name: 'eventMonitoring',
  initialState,

  reducers: {
    setProcessedEventPage: (state, action) => {
      state.processedPage = action.payload
    },

    setFailedEventPage: (state, action) => {
      state.failedPage = action.payload
    },

    clearSelectedEvent: (state) => {
      state.selectedEvent = null
      state.selectedEventCategory = null
      state.detailsError = null
    },

    resetEventMonitoringState: () => initialState,
  },

  extraReducers: (builder) => {
    builder

      // Event overview
      .addCase(fetchEventOverview.pending, (state) => {
        state.overviewLoading = true
        state.overviewError = null
      })
      .addCase(fetchEventOverview.fulfilled, (state, action) => {
        state.overviewLoading = false

        state.overview = {
          totalEvents: action.payload?.totalEvents ?? 0,
          totalProcessedEvents:
            action.payload?.totalProcessedEvents ?? 0,
          totalFailedEvents:
            action.payload?.totalFailedEvents ?? 0,
        }
      })
      .addCase(fetchEventOverview.rejected, (state, action) => {
        state.overviewLoading = false
        state.overviewError =
          action.payload || 'Unable to load event overview.'
      })

      // Processed events
      .addCase(fetchProcessedEvents.pending, (state) => {
        state.processedLoading = true
        state.processedError = null
      })
      .addCase(fetchProcessedEvents.fulfilled, (state, action) => {
        state.processedLoading = false

        const response = action.payload || {}

        state.processedEvents = Array.isArray(response.content)
          ? response.content
          : []

        state.processedPage = response.number ?? 0
        state.processedSize = response.size ?? state.processedSize
        state.processedTotalElements =
          response.totalElements ?? 0
        state.processedTotalPages = response.totalPages ?? 0
      })
      .addCase(fetchProcessedEvents.rejected, (state, action) => {
        state.processedLoading = false
        state.processedError =
          action.payload || 'Unable to load processed events.'
      })

      // Failed events
      .addCase(fetchFailedEvents.pending, (state) => {
        state.failedLoading = true
        state.failedError = null
      })
      .addCase(fetchFailedEvents.fulfilled, (state, action) => {
        state.failedLoading = false

        const response = action.payload || {}

        state.failedEvents = Array.isArray(response.content)
          ? response.content
          : []

        state.failedPage = response.number ?? 0
        state.failedSize = response.size ?? state.failedSize
        state.failedTotalElements =
          response.totalElements ?? 0
        state.failedTotalPages = response.totalPages ?? 0
      })
      .addCase(fetchFailedEvents.rejected, (state, action) => {
        state.failedLoading = false
        state.failedError =
          action.payload || 'Unable to load failed events.'
      })

      // Processed event details
      .addCase(fetchProcessedEventById.pending, (state) => {
        state.detailsLoading = true
        state.detailsError = null
        state.selectedEvent = null
        state.selectedEventCategory = null
      })
      .addCase(fetchProcessedEventById.fulfilled, (state, action) => {
        state.detailsLoading = false
        state.selectedEvent = action.payload.event
        state.selectedEventCategory = action.payload.eventCategory
      })
      .addCase(fetchProcessedEventById.rejected, (state, action) => {
        state.detailsLoading = false
        state.detailsError =
          action.payload || 'Unable to load processed event details.'
      })

      // Failed event details
      .addCase(fetchFailedEventById.pending, (state) => {
        state.detailsLoading = true
        state.detailsError = null
        state.selectedEvent = null
        state.selectedEventCategory = null
      })
      .addCase(fetchFailedEventById.fulfilled, (state, action) => {
        state.detailsLoading = false
        state.selectedEvent = action.payload.event
        state.selectedEventCategory = action.payload.eventCategory
      })
      .addCase(fetchFailedEventById.rejected, (state, action) => {
        state.detailsLoading = false
        state.detailsError =
          action.payload || 'Unable to load failed event details.'
      })
  },
})

export const {
  setProcessedEventPage,
  setFailedEventPage,
  clearSelectedEvent,
  resetEventMonitoringState,
} = eventMonitoringSlice.actions

export default eventMonitoringSlice.reducer