// Shared form validators. Keep in sync with com.mediflow.dto.Patterns on the backend.

export const PHONE_RE = /^(?:0|\+94)[1-9][0-9]{8}$/
export const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/
export const PERSON_NAME_RE = /^[\p{L}\p{M}][\p{L}\p{M} .'-]*$/u
export const HAS_LETTER_RE = /\p{L}/u

/** Lets the user type only digits (and a single leading +), max 12 chars (+94 + 9 digits). */
export const sanitizePhone = (v) =>
  (v || '').replace(/[^\d+]/g, '').replace(/(?!^)\+/g, '').slice(0, 12)

export const phoneError = (v) => {
  const s = (v || '').trim()
  if (!s) return ''
  return PHONE_RE.test(s) ? '' : 'Enter a valid number, e.g. 0771234567 or +94771234567'
}

export const emailError = (v) => {
  const s = (v || '').trim()
  if (!s) return ''
  return EMAIL_RE.test(s) ? '' : 'Enter a valid email, e.g. name@example.com'
}

export const requiredNameError = (v, label = 'Name') => {
  const s = (v || '').trim()
  if (!s) return `${label} is required`
  return HAS_LETTER_RE.test(s) ? '' : `${label} must contain letters`
}

export const optionalNameError = (v, label = 'Name') => {
  const s = (v || '').trim()
  if (!s) return ''
  return HAS_LETTER_RE.test(s) ? '' : `${label} must contain letters`
}

export const personNameError = (v, label) => {
  const s = (v || '').trim()
  if (!s) return `${label} is required`
  return PERSON_NAME_RE.test(s) ? '' : `${label} may contain only letters, spaces, . ' and -`
}
