const PRIMARY_EXCHANGE_API_BASE_URL = 'https://api.frankfurter.app'
const FALLBACK_EXCHANGE_API_BASE_URL = 'https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies'
const CACHE_TTL_MS = 30 * 60 * 1000
const rateCache = new Map()

const getCacheKey = (fromCurrency, toCurrency) => `${fromCurrency}->${toCurrency}`

const normalizeCurrency = (currency) => String(currency || '').trim().toUpperCase()

export const convertAmount = (amount, rate) => {
  const numericAmount = Number(amount ?? 0)
  if (!Number.isFinite(numericAmount) || !Number.isFinite(rate)) {
    return 0
  }
  return numericAmount * rate
}

export const getExchangeRate = async (fromCurrency, toCurrency) => {
  const from = normalizeCurrency(fromCurrency)
  const to = normalizeCurrency(toCurrency)

  if (!from || !to) {
    throw new Error('Currency code is required')
  }

  if (from === to) {
    return 1
  }

  const cacheKey = getCacheKey(from, to)
  const cached = rateCache.get(cacheKey)
  const now = Date.now()

  if (cached && now - cached.updatedAt < CACHE_TTL_MS) {
    return cached.rate
  }

  const rate = await fetchRateWithFallback(from, to)

  rateCache.set(cacheKey, { rate, updatedAt: now })
  return rate
}

const fetchRateWithFallback = async (from, to) => {
  try {
    return await fetchRateFromFrankfurter(from, to)
  } catch {
    return fetchRateFromFawaz(from, to)
  }
}

const fetchRateFromFrankfurter = async (from, to) => {
  const response = await fetch(
    `${PRIMARY_EXCHANGE_API_BASE_URL}/latest?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
  )

  if (!response.ok) {
    throw new Error('Primary provider failed')
  }

  const data = await response.json()
  const rate = Number(data?.rates?.[to])

  if (!Number.isFinite(rate) || rate <= 0) {
    throw new Error('Primary provider returned invalid rate')
  }

  return rate
}

const fetchRateFromFawaz = async (from, to) => {
  const fromLower = from.toLowerCase()
  const toLower = to.toLowerCase()

  const response = await fetch(
    `${FALLBACK_EXCHANGE_API_BASE_URL}/${encodeURIComponent(fromLower)}.json`
  )

  if (!response.ok) {
    throw new Error('Fallback provider failed')
  }

  const data = await response.json()
  const rate = Number(data?.[fromLower]?.[toLower])

  if (!Number.isFinite(rate) || rate <= 0) {
    throw new Error('Fallback provider returned invalid rate')
  }

  return rate
}

