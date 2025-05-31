import { io, Socket } from 'socket.io-client'
import { ref, onUnmounted } from 'vue'
import type { ProcessingCallback } from '@/models/processing-callback.ts'
import type { Metadata } from '@/models/metadata.ts'

export type Stage = 'original' | 'grayscale' | 'blur' | 'detection' | 'final'

export function useSocketVideo() {
  const socket = ref<Socket | null>(null)
  const isConnected = ref<boolean>(false)
  const currentStage = ref<Stage>('original')
  const processingProgress = ref<number>(0)
  const isProcessing = ref<boolean>(false)

  const connect = (hostname: string = 'localhost', port: number = 9092, namespace: string = '', secure: boolean = false) => {
    const protocol = secure ? 'https' : 'http';
    const url = `${protocol}://${hostname}:${port}/${namespace}`

    console.info(`[Socket.io] Connecting to ${url} ...`);

    socket.value = io(url, {
      transports: ['websocket'],
      forceBase64: false, // Force binary transmission
      reconnection: true,
      // upgrade: false
    })

    socket.value.on('connect', () => {
      isConnected.value = true
      updateStage()
      console.info('[Socket.io] Connected to server');
    })

    socket.value.on('disconnect', () => {
      isConnected.value = false
      console.info('[Socket.io] Disconnected from server');
    })

    socket.value.on('processed-frame', (metadata: Metadata, data: ArrayBuffer) => {
      if (processor.value) {
        processor.value(metadata, data)
      }
    })

    socket.value.on('processing-progress', (progress) => {
      processingProgress.value = progress
    })

    socket.value.on('processing-complete', () => {
      isProcessing.value = false
    })
  }

  const processor = ref<ProcessingCallback | null>(null)

  const registerProcessor = (callback: ProcessingCallback) => {
    processor.value = callback
  }

  const updateStage = (): void => {
    if (isConnected.value && socket.value && currentStage.value) {
      socket.value.emit('stage-change', currentStage.value)
    }
  }

  const setStage = (stage: Stage) => {
    currentStage.value = stage
    updateStage()
  }

  const sendVideo = async (file: File): Promise<boolean> => {
    if (!isConnected.value || !file) return false
    socket.value.emit('processing-start')

    isProcessing.value = true
    processingProgress.value = 0

    try {
      const video = await createVideoElement(file)
      const duration = video.duration
      const canvas: HTMLCanvasElement = document.createElement('canvas')
      const ctx: CanvasRenderingContext2D | null = canvas.getContext('2d')

      video.addEventListener('seeked', async () => {
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        ctx.drawImage(video, 0, 0)

        const blob: Blob = await new Promise(resolve =>
          canvas.toBlob(resolve, 'image/jpeg', 0.8)
        )

        const arrayBuffer = await new Promise(resolve => {
          const reader = new FileReader()
          reader.onload = () => resolve(reader.result)
          reader.readAsArrayBuffer(blob)
        })

        // socket.value.emit('feed', arrayBuffer)
        socket.value.emit('feed', {
          frame: arrayBuffer,
          width: video.videoWidth,
          height: video.videoHeight,
          timestamp: performance.now(),
        })
      })

      // Process frame by frame
      const fps = 30
      const frameCount = Math.floor(duration * fps)

      for (let i = 0; i < frameCount; i++) {
        if (!isProcessing.value) break // Stop if processing was cancelled

        const time = i / fps
        video.currentTime = time

        // Wait for seek to complete
        await new Promise(resolve => {
          const checkReady = () => {
            if (!video.seeking) {
              video.removeEventListener('seeked', checkReady)
              resolve()
            }
          }
          video.addEventListener('seeked', checkReady)
        })

        processingProgress.value = (i / frameCount) * 100
      }

      socket.value.emit('processing-complete')
    } catch (err) {
      console.error('Video processing error:', err)
      isProcessing.value = false
      return false
    }

    return true
  }

  const cancelProcessing = () => {
    isProcessing.value = false
    if (isConnected.value) {
      socket.value.emit('processing-cancelled')
    }
  }

  const createVideoElement = (file: File): Promise<HTMLVideoElement> => {
    return new Promise((resolve, reject) => {
      const video: HTMLVideoElement = document.createElement('video')
      video.preload = 'metadata'

      video.onloadedmetadata = () => {
        URL.revokeObjectURL(video.src)
        resolve(video)
      }

      video.onerror = () => {
        reject(new Error('Invalid video file'))
      }

      video.src = URL.createObjectURL(file)
    })
  }

  onUnmounted(() => {
    if (socket.value) {
      socket.value.disconnect()
    }
  })

  return {
    connect,
    sendVideo,
    setStage,
    registerProcessor,
    cancelProcessing,
    isConnected,
    currentStage,
    processingProgress,
    isProcessing
  }
}
