import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './api' // axios base URL, auth header and 401 handling
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
