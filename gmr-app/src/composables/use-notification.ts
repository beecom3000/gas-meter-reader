import { Notify } from 'quasar'

export const useNotification = () => {
  const showNotification = (type: string, message: string) => {
    Notify.create({
      type: type === 'success' ? 'positive' : 'negative',
      message: message,
      position: 'top-right',
      timeout: 3000
    })
  }

  return {
    showNotification
  }
}
