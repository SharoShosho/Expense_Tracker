const STORAGE_KEY = 'preferredCurrency'

export const SUPPORTED_CURRENCIES = ['EUR', 'USD', 'SEK', 'DKK', 'GBP', 'AED', 'JPY', 'INR', 'RUB']

export const DEFAULT_CURRENCY = 'EUR'

const CURRENCY_CHANGED_EVENT = 'expense-tracker:currency-changed'

export const getPreferredCurrency = () => {
  const storedCurrency = localStorage.getItem(STORAGE_KEY)
  if (storedCurrency === 'NOK') {
    return 'DKK'
  }
  return SUPPORTED_CURRENCIES.includes(storedCurrency) ? storedCurrency : DEFAULT_CURRENCY
}

export const setPreferredCurrency = (currency) => {
  const safeCurrency = SUPPORTED_CURRENCIES.includes(currency) ? currency : DEFAULT_CURRENCY
  localStorage.setItem(STORAGE_KEY, safeCurrency)
  window.dispatchEvent(new CustomEvent(CURRENCY_CHANGED_EVENT, { detail: { currency: safeCurrency } }))
  return safeCurrency
}

export const onCurrencyChange = (listener) => {
  const handler = (event) => listener(event.detail.currency)
  window.addEventListener(CURRENCY_CHANGED_EVENT, handler)
  return () => window.removeEventListener(CURRENCY_CHANGED_EVENT, handler)
}

export const formatCurrency = (value, currency = DEFAULT_CURRENCY) => {
  const numericValue = Number(value || 0)
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericValue)
}

