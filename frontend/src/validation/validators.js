const asNumber = (value) => Number(value)

const firstFailedRule = (rules) => {
  const failedRule = rules.find((rule) => !rule.check())
  return failedRule ? failedRule.message : ''
}

export const validateExpenseAmount = (amount) => firstFailedRule([
  {
    check: () => String(amount ?? '').trim() !== '',
    message: 'Please enter an amount',
  },
  {
    check: () => Number.isFinite(asNumber(amount)),
    message: 'Please enter a valid number',
  },
  {
    check: () => asNumber(amount) > 0,
    message: 'Please enter a valid amount greater than 0',
  },
])

export const validateRegistration = ({ password, confirmPassword }) => firstFailedRule([
  {
    check: () => password === confirmPassword,
    message: 'Passwords do not match',
  },
  {
    check: () => String(password || '').length >= 6,
    message: 'Password must be at least 6 characters',
  },
])

