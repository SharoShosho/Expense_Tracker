import { useCallback, useEffect, useState } from 'react'
import {
  DEFAULT_CURRENCY,
  getPreferredCurrency,
  onCurrencyChange,
} from '../services/currencyService'
import { convertAmount, getExchangeRate } from '../services/exchangeRateService'

export const useCurrencyConversion = ({ warningMessage }) => {
  const [currency, setCurrency] = useState(getPreferredCurrency())
  const [exchangeRate, setExchangeRate] = useState(1)
  const [rateWarning, setRateWarning] = useState('')

  useEffect(() => onCurrencyChange(setCurrency), [])

  useEffect(() => {
    let mounted = true

    const loadExchangeRate = async () => {
      try {
        const nextRate = await getExchangeRate(DEFAULT_CURRENCY, currency)
        if (!mounted) {
          return
        }
        setExchangeRate(nextRate)
        setRateWarning('')
      } catch {
        if (!mounted) {
          return
        }
        setExchangeRate(1)
        setRateWarning(warningMessage)
      }
    }

    loadExchangeRate()
    return () => {
      mounted = false
    }
  }, [currency, warningMessage])

  const convertFromBaseCurrency = useCallback(
    (amount) => convertAmount(amount, exchangeRate),
    [exchangeRate]
  )

  const convertToBaseCurrency = useCallback(async (amount) => {
    if (currency === DEFAULT_CURRENCY) {
      return Number(amount)
    }

    const latestRate = await getExchangeRate(DEFAULT_CURRENCY, currency)
    return convertAmount(amount, 1 / latestRate)
  }, [currency])

  return {
    currency,
    exchangeRate,
    rateWarning,
    convertFromBaseCurrency,
    convertToBaseCurrency,
  }
}

