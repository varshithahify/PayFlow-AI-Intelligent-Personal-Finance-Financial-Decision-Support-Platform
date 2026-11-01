import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'

import { store } from './app/store'
import { logout } from './features/auth/authSlice'
import { setUnauthorizedHandler } from './services/api'

import './index.css'
import './mobile.css'
import App from './App.jsx'

// Automatically log out when an authenticated API request returns HTTP 401.
setUnauthorizedHandler(() => {
  store.dispatch(logout())
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </StrictMode>,
)