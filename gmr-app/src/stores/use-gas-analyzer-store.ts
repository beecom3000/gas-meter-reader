import { defineStore } from 'pinia'
import { io, Socket } from 'socket.io-client'
import type { Metadata } from '@/models/metadata.ts'
import { nextTick } from 'vue'
import type { Stage } from '@/models/stage.ts'
import { useNotification } from '@/composables/use-notification.ts'

declare const MediaStreamTrackProcessor: any;

export interface State {
  socket: Socket | null;
  isSocketConnected: boolean;
  connectionStatusMessage: string;
  serverStatusMessage: string;
  BACKEND_URL: string;

  videoFile: File | null;
  videoSrc: string;
  isVideoReady: boolean;
  mediaStream: MediaStream | null;
  trackProcessor: typeof MediaStreamTrackProcessor | null;
  reader: ReadableStreamReader<VideoFrame> | null;

  isProcessing: boolean;
  fps: number;
  processingInterval: number | undefined

  opencvParams: OpencvParams;

  processedFrame: HTMLCanvasElement | null;

}

export interface OpencvParams {
  cannyThreshold1: number;
  cannyThreshold2: number;
  houghAccumulatorThreshold: number;
}

 const { showNotification } = useNotification()

export const useGasAnalyzerStore = defineStore('gasAnalyzerStore', {
  state: (): State => ({
    // Connection and Status
    socket: null,
    isSocketConnected: false,
    connectionStatusMessage: 'Connecting to backend...',
    serverStatusMessage: 'Happy',
    BACKEND_URL: 'http://localhost:9092/',

    // File and Video Handling
    videoFile: null,
    videoSrc: '',
    isVideoReady: false,
    mediaStream: null,

    // Processing Control
    isProcessing: false,
    fps: 5,
    processingInterval: undefined,

    // OpenCV Parameters
    opencvParams: {
      cannyThreshold1: 50,
      cannyThreshold2: 150,
      houghAccumulatorThreshold: 50,
    },

    // Backend Response Data
    processedFrame: null,
    recognizedValue: null,

    // Gemini AI State
    isSuggestingParams: false,
    geminiStatus: null,
  }),
  actions: {
    initSocket() {
      console.log('[Socket.i] Attempting to connect to %s', this.BACKEND_URL)
      this.socket = io(this.BACKEND_URL, {
        transports: ['websocket'],
        forceBase64: false, // Force binary transmission
        reconnection: true,
      })

      this.socket.on('connect', () => {
        console.log('[Socket.io] Connected to backend. Socket ID:', this.socket!.id)
        this.isSocketConnected = true
        this.connectionStatusMessage = 'Connected'
      })

      this.socket.on('disconnect', () => {
        console.warn('[Socket.io] Disconnected from backend.')
        this.isSocketConnected = false
        this.connectionStatusMessage = 'Disconnected. Check backend server.'
        this.stopProcessing()
      })

      this.socket.on('processed-frame', this.renderProcessedFrame)

      // this.socket.on('processed-frame', (data) => {
      //   // if (data.image) {
      //   //   this.processedFrame = `data:image/jpeg;base64,${data.image}`;
      //   // }
      //   // if (data.value !== undefined) {
      //   //   this.recognizedValue = parseFloat(data.value).toFixed(4);
      //   // }
      // });

      this.socket.on('connect_error', (err) => {
        console.error('Connection Error:', err.message)
        this.connectionStatusMessage = 'Connection Failed'
      })

      this.socket.on('server-error', (err) => {
        console.error('Server Error:', err.message)
        showNotification('error', err.message)
      })
    },

    updateProcessingStage(stage: Stage) {
      this.socket && this.socket.emit('update-stage', stage)
    },

    renderProcessedFrame(metadata: Metadata, data: ArrayBuffer) {
      if (!this.processedFrame) return
      const blob = new Blob([new Uint8Array(data.slice(1))], { type: 'image/jpeg' })
      const img = new Image()
      img.onload = () => {
        if (this.processedFrame) {
          this.processedFrame.width = img.width
          this.processedFrame.height = img.height
          const ctx: CanvasRenderingContext2D | null = this.processedFrame.getContext('2d')
          if (ctx) {
            ctx.drawImage(img, 0, 0)
          }
          URL.revokeObjectURL(img.src)
        }
      }
      img.src = URL.createObjectURL(blob)
    },

    handleFileUpload(file: File) {
      if (file && file.type.startsWith('video/')) {
        this.videoFile = file
        this.videoSrc = URL.createObjectURL(file)
        this.processedFrame = null
        this.recognizedValue = null
        this.isVideoReady = false
        this.geminiStatus = null
      } else {
        this.videoFile = null
        this.videoSrc = ''
        alert('Please select a valid video file.')
      }
    },

    onVideoLoaded() {
      this.isVideoReady = true
    },

    async startProcessing(videoElement: HTMLVideoElement | null) {
      if (!this.videoFile || !videoElement || !this.isSocketConnected) return
      this.isProcessing = true
      this.socket!.emit('processing-start')
      await nextTick()
      this.recognizedValue = null

      if (this.socket) {
        this.socket.emit('update_settings', {
          params: this.opencvParams,
          fps: this.fps,
        })
      }

      videoElement.play()
      this.mediaStream = videoElement.captureStream()

      // Create a MediaStreamTrackProcessor to read frames
      // Note: The MediaStreamTrackProcessor interface of the Insertable Streams for MediaStreamTrack API
      // consumes a video MediaStreamTrack object's source and generates a stream of VideoFrame objects.
      if ('MediaStreamTrackProcessor' in window) {
        // Get video track
        const videoTrack: MediaStreamTrack = this.mediaStream!.getVideoTracks()[0]
        this.captureAndSend(videoTrack)
      } else {
        console.warn('MediaStreamTrackProcessor not supported in this browser')
        this.processingInterval = setInterval(() => {
          if (videoElement.paused || videoElement.ended) {
            this.stopProcessing()
            return
          }
          this.fallbackCaptureAndSend(videoElement)
        }, 1000 / this.fps)
      }
    },

    async captureAndSend(videoTrack: MediaStreamTrack): Promise<void> {
      this.trackProcessor = new MediaStreamTrackProcessor({ track: videoTrack })
      try {
        // The getReader() method of the ReadableStream interface creates a reader and locks
        // the stream to it. While the stream is locked, no other reader can be acquired until
        // this one is released.
        const readable = this.trackProcessor.readable
        const reader: ReadableStreamReader<VideoFrame> = readable.getReader()
        while (this.isProcessing) {
          // Result objects contain two properties:
          // done  - true if the stream has already given you all its data.
          // value - some data. Always undefined when done is true.
          const { done, value } = await reader.read()
          if (done) {
            console.info('Stream completed and done reading frames')
            break
          }

          const arrayBuffer: ArrayBuffer = await this.extractFrameAsArrayBuffer(value)

          // Send to server
          if (this.socket && this.socket.connected) {
            this.socket.emit('feed', {
              frame: arrayBuffer,
              width: (value as VideoFrame).displayWidth,
              height: (value as VideoFrame).displayHeight,
              timestamp: performance.now(),
            })
          }

          ;(value as VideoFrame).close()
        }
      } catch (error) {
        console.error('Error reading frames:', error)
      } finally {
        this.stopProcessing()
      }
    },

    async extractFrameAsArrayBuffer(videoFrame: VideoFrame): Promise<ArrayBuffer> {
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

      ctx.drawImage(bitmap, 0, 0, videoFrame.displayWidth, videoFrame.displayHeight)

      // Get image data as blob
      const blob = await canvas.convertToBlob({ quality: 1.0, type: 'image/jpeg' })
      return await blob.arrayBuffer()

      // const uint8Array = new Uint8Array(result)
      // const checkJpeg = isJpeg(uint8Array)
      // console.log(`${uint8Array[0]}, ${uint8Array[1]}, ${uint8Array[2]}`)

      // Convert blob to array buffer
      // return result
    },

    stopProcessing(videoPlayer?: HTMLVideoElement | null) {
      if (videoPlayer) {
        videoPlayer.pause()
      }

      if (this.processingInterval) clearInterval(this.processingInterval)
      this.isProcessing = false
      this.processingInterval = undefined
      if (this.reader) {
        this.reader.cancel()
      }
      if (this.trackProcessor) {
        this.trackProcessor.readable.cancel()
      }
      if (this.mediaStream) {
        this.mediaStream.getTracks().forEach((track: MediaStreamTrack) => track.stop())
      }
      this.reader = null
      this.trackProcessor = null
      this.mediaStream = null
      console.log('Processing stopped.')
    },

    fallbackCaptureAndSend(videoElement: HTMLVideoElement, asBase64Callback = null) {
      const canvas = document.createElement('canvas')
      const context = canvas.getContext('2d')

      canvas.width = videoElement.videoWidth
      canvas.height = videoElement.videoHeight
      context.drawImage(videoElement, 0, 0, canvas.width, canvas.height)

      if (asBase64Callback) {
        const base64Data = canvas.toDataURL('image/jpeg').split(',')[1]
        asBase64Callback(base64Data)
      } else {
        canvas.toBlob(
          (blob) => {
            if (this.socket && this.isSocketConnected) {
              this.socket.emit('feed', blob)
            }
          },
          'image/jpeg',
          0.9,
        )
      }
    },

    async suggestParameters(videoElement, canvasElement) {
      if (!this.videoFile || !this.isVideoReady) {
        alert('Please upload a video and wait for it to load.')
        return
      }

      this.isSuggestingParams = true
      this.geminiStatus = { message: 'Analyzing frame...', isError: false }

      // Go to the first frame
      videoElement.currentTime = 0

      // Use a short delay to ensure the frame is drawn before capturing
      setTimeout(() => {
        this.fallbackCaptureAndSend(videoElement, async (base64ImageData) => {
          if (!base64ImageData) {
            this.geminiStatus = { message: 'Frame capture failed.', isError: true }
            this.isSuggestingParams = false
            return
          }

          const apiKey = '' // The platform will inject the key here
          const apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`

          const prompt =
            "Analyze this image of a gas meter dial. It might be blurry, have glare, or be at an angle. Based on visual characteristics, suggest optimal integer values for these OpenCV parameters: cannyThreshold1 (between 10-100), cannyThreshold2 (between 100-200), and houghAccumulatorThreshold (for circle detection, between 20-100). Provide your response ONLY as a valid JSON object with keys: 'cannyThreshold1', 'cannyThreshold2', and 'houghAccumulatorThreshold'. Do not include any other text or markdown formatting."

          const payload = {
            contents: [
              {
                parts: [
                  { text: prompt },
                  { inlineData: { mimeType: 'image/jpeg', data: base64ImageData } },
                ],
              },
            ],
          }

          try {
            const response = await fetch(apiUrl, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(payload),
            })

            if (!response.ok) {
              throw new Error(`API Error: ${response.statusText}`)
            }

            const result = await response.json()
            const text = result.candidates[0].content.parts[0].text

            // Clean the response to ensure it's valid JSON
            const jsonString = text
              .replace(/```json/g, '')
              .replace(/```/g, '')
              .trim()
            const suggestedParams = JSON.parse(jsonString)

            // Update the parameters
            this.opencvParams.cannyThreshold1 = suggestedParams.cannyThreshold1
            this.opencvParams.cannyThreshold2 = suggestedParams.cannyThreshold2
            this.opencvParams.houghAccumulatorThreshold = suggestedParams.houghAccumulatorThreshold

            this.geminiStatus = { message: 'Parameters updated!', isError: false }
          } catch (error) {
            console.error('Gemini API Error:', error)
            this.geminiStatus = { message: 'AI suggestion failed.', isError: true }
          } finally {
            this.isSuggestingParams = false
          }
        })
      }, 200)
    },
  },
})
