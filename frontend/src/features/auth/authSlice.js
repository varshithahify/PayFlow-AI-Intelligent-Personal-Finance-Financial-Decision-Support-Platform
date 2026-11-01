import { createSlice } from '@reduxjs/toolkit'

// Restore authentication from the current browser session.
const storedToken = sessionStorage.getItem('accessToken')
const storedEmail = sessionStorage.getItem('userEmail')

const initialState = {
  token: storedToken || null,
  userEmail: storedEmail || '',
  isAuthenticated: Boolean(storedToken),
}

const authSlice = createSlice({
  name: 'auth',

  initialState,

  reducers: {
    loginSuccess: (state, action) => {
      const { token, email } = action.payload

      state.token = token
      state.userEmail = email
      state.isAuthenticated = true

      sessionStorage.setItem('accessToken', token)
      sessionStorage.setItem('userEmail', email)
    },

    logout: (state) => {
      state.token = null
      state.userEmail = ''
      state.isAuthenticated = false

      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('userEmail')
    },
  },
})

export const { loginSuccess, logout } = authSlice.actions

export default authSlice.reducer