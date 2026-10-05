import { createContext, useCallback, useContext, useState } from 'react'

const ToastContext = createContext(null)

let nextId = 1

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const push = useCallback((message, type = 'success') => {
    const id = nextId++
    setToasts((t) => [...t, { id, message, type }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4200)
  }, [])

  const value = {
    success: (m) => push(m, 'success'),
    error: (m) => push(m, 'danger'),
    warning: (m) => push(m, 'warning'),
    info: (m) => push(m, 'info'),
  }

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="mf-toast-stack">
        {toasts.map((t) => (
          <div key={t.id} className={`alert alert-${t.type} shadow-sm py-2 px-3 mb-2`} role="alert">
            <i
              className={`bi me-2 ${
                t.type === 'success'
                  ? 'bi-check-circle-fill'
                  : t.type === 'danger'
                    ? 'bi-exclamation-octagon-fill'
                    : t.type === 'warning'
                      ? 'bi-exclamation-triangle-fill'
                      : 'bi-info-circle-fill'
              }`}
            ></i>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
