import { io, Socket } from 'socket.io-client'
import { ref } from 'vue'
import type { ProcessingCallback } from '@/models/processing-callback.ts'
import type { Metadata } from '@/models/metadata.ts'

declare const MediaStreamTrackProcessor: any;

export type Stage = 'original' | 'grayscale' | 'blur' | 'detection' | 'final'

export function useSocketVideo() {
  const socket = ref<Socket | null>(null)
  const isConnected = ref<boolean>(false)
  const currentStage = ref<Stage>('original')
  const processingProgress = ref<number>(0)
  const isProcessing = ref<boolean>(false)
  const mediaStream = ref<MediaStream | null>(null)
  const trackProcessor = ref<InstanceType<typeof MediaStreamTrackProcessor> | null>(null)
  const reader = ref<ReadableStreamReader<VideoFrame> | null>(null)

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

    socket.value.on('stage-change-completed', (newStage: Stage) => {
      currentStage.value = newStage;
    })
  }

  const disconnect = () => {
    cancelProcessing()
    stopProcessing()
    if (socket.value) {
      socket.value.disconnect()
    }
  }

  const processor = ref<ProcessingCallback | null>(null)

  const registerProcessor = (callback: ProcessingCallback) => {
    processor.value = callback
  }

  const updateStage = (stage: Stage = 'original'): void => {
    if (isConnected.value && socket.value) {
      socket.value.emit('stage-change', stage)
    }
  }

  const sendVideo = async (stream: MediaStream) => {
    try {
      if (socket.value) {
        socket.value.emit('processing-start')
        isProcessing.value = true
      }
      // Create a MediaStream from the video file
      mediaStream.value = stream

      // Get video track
      const videoTrack: MediaStreamTrack = stream.getVideoTracks()[0]

      // Create a MediaStreamTrackProcessor to read frames
      // Note: The MediaStreamTrackProcessor interface of the Insertable Streams for MediaStreamTrack API
      // consumes a video MediaStreamTrack object's source and generates a stream of VideoFrame objects.
      if ('MediaStreamTrackProcessor' in window) {
        trackProcessor.value = new MediaStreamTrackProcessor({ track: videoTrack })
        const readableStream: ReadableStream = trackProcessor.value.readable
        readFramesAndSend(readableStream)
      } else {
        console.error('MediaStreamTrackProcessor not supported in this browser')
        fallbackFrameCapture()
      }
    } catch (error) {
      console.error('Error processing video:', error)
      isProcessing.value = false
    }
  }

  const fallbackFrameCapture = () => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    canvas.width = videoPreview.value!.videoWidth
    canvas.height = videoPreview.value!.videoHeight

    const captureInterval = setInterval(() => {
      if (!isProcessing.value) {
        clearInterval(captureInterval)
        return
      }

      ctx.drawImage(videoPreview.value, 0, 0, canvas.width, canvas.height)

      canvas.toBlob(
        async (blob: Blob) => {
          const arrayBuffer = await blob.arrayBuffer()

          if (socket.value && socket.value.connected) {
            socket.value.emit('video-frame', {
              frameData: arrayBuffer,
              width: canvas.width,
              height: canvas.height,
              timestamp: performance.now(),
            })
          }
        },
        'image/jpeg',
        0.8,
      )
    }, 1000 / 30) // 30 FPS
  }

// Read frames and send to server
  const readFramesAndSend = async (readableStream: ReadableStream<VideoFrame>) => {
    try {
      // The getReader() method of the ReadableStream interface creates a reader and locks
      // the stream to it. While the stream is locked, no other reader can be acquired until
      // this one is released.
      reader.value = readableStream.getReader()
      while (isProcessing.value) {
        // Result objects contain two properties:
        // done  - true if the stream has already given you all its data.
        // value - some data. Always undefined when done is true.
        const { done, value: videoFrame } = await reader.value.read()
        if (done) {
          console.log('Stream completed and done reading frames')
          break
        }

        const arrayBuffer: ArrayBuffer = await extractFrameAsArrayBuffer(videoFrame)

        // Send to server
        if (socket.value && socket.value.connected) {
          socket.value.emit('feed', {
            frame: arrayBuffer,
            width: videoFrame.displayWidth,
            height: videoFrame.displayHeight,
            timestamp: performance.now(),
          })
        }

        videoFrame.close()
      }
    } catch (error) {
      console.error('Error reading frames:', error)
    } finally {
      stopProcessing()
    }
  }

  const extractFrameAsArrayBuffer = async (videoFrame: VideoFrame): Promise<ArrayBuffer> => {
    // Convert frame to canvas to get image data
    // console.debug('Video format: ', videoFrame.format)
    const bitmap: ImageBitmap = await createImageBitmap(videoFrame)
    // console.log(`Width: ${videoFrame.displayWidth}, Height: ${videoFrame.displayHeight}`)
    const canvas = new OffscreenCanvas(videoFrame.displayWidth, videoFrame.displayHeight)
    const ctx: OffscreenCanvasRenderingContext2D | null = canvas.getContext('2d')

    if (!ctx) {
      console.warn('No context found')
      return Promise.reject('No context found')
    }

    ctx.drawImage(bitmap, 0, 0,
      videoFrame.displayWidth, videoFrame.displayHeight )

    // Get image data as blob
    const blob = await canvas.convertToBlob({ quality: 1.0, type: 'image/jpeg' })
    const result = await blob.arrayBuffer()

    // const uint8Array = new Uint8Array(result)
    // const checkJpeg = isJpeg(uint8Array)
    // console.log(`${uint8Array[0]}, ${uint8Array[1]}, ${uint8Array[2]}`)

    // Convert blob to array buffer
    return result
  }

  const stopProcessing = (readableStream?: ReadableStream) => {
    isProcessing.value = false
    if (reader.value) {
      reader.value.cancel()
      reader.value = null
    }
    if (trackProcessor.value && !readableStream?.locked) {
      trackProcessor.value.readable.cancel()
      trackProcessor.value = null
    }
    if (mediaStream.value) {
      mediaStream.value.getTracks().forEach((track) => track.stop())
      mediaStream.value = null
    }
  }

  const cancelProcessing = () => {
    if (isConnected.value && socket.value) {
      socket.value.emit('processing-cancelled')
      isProcessing.value = false
    }
  }

  return {
    connect,
    disconnect,
    sendVideo,
    updateStage,
    registerProcessor,
    cancelProcessing,
    isConnected,
    currentStage,
    processingProgress,
    isProcessing
  }
}
