import { createContext, useContext, useEffect, useState } from 'react'
import api from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('mediflow_user')) || null
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('mediflow_token')
    if (!token) {
      setLoading(false)
      return
    }
    // Re-validate the stored session against the backend
    api
      .get('/auth/me')
      .then((res) => {
        setUser(res.data)
        localStorage.setItem('mediflow_user', JSON.stringify(res.data))
      })
      .catch(() => {
        localStorage.removeItem('mediflow_token')
        localStorage.removeItem('mediflow_user')
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])

  const login = async (email, password) => {
    const res = await api.post('/auth/login', { email, password })
    localStorage.setItem('mediflow_token', res.data.token)
    localStorage.setItem('mediflow_user', JSON.stringify(res.data.user))
    setUser(res.data.user)
    return res.data.user
  }

  const logout = () => {
    localStorage.removeItem('mediflow_token')
    localStorage.removeItem('mediflow_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
