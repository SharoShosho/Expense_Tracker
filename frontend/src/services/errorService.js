export const getErrorMessage = (error, fallbackMessage) => {
  const candidateMessages = [
    error?.response?.data?.message,
    error?.message,
    fallbackMessage,
  ]

  return candidateMessages.find((message) => typeof message === 'string' && message.trim().length > 0) || fallbackMessage
}

