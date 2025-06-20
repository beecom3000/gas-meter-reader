<template>
  <q-layout view="hHh Lpr lff">
    <q-page-container>
      <q-page class="q-pa-md">
        <div class="q-mb-md">
          <header></header>
        </div>

        <div class="row q-col-gutter-md">
          <!-- Left Column: Controls & Parameters -->
          <div class="col-12 col-md-4">
            <control-panel></control-panel>
          </div>

          <!-- Right Column: Video Displays -->
          <div class="col-12 col-md-8">
            <display-panel></display-panel>
          </div>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import ControlPanel from '@/views/analyzer/ControlPanel.vue'
import DisplayPanel from '@/views/analyzer/DisplayPanel.vue'

    // --- STATE MANAGEMENT ---

    // Connection and Status
    const socket = ref(null)
    const isSocketConnected = ref(false)
    const statusMessage = ref('Connecting to backend...')
    const BACKEND_URL = 'http://localhost:8080'

    // File and Video Handling
    const videoFile = ref(null)
    const videoSrc = ref('')
    const videoPlayer = ref(null)
    const canvasElement = ref(null)
    const isVideoReady = ref(false)

    // Processing Control
    const isProcessing = ref(false)

    const processingInterval = ref(null)

    // OpenCV Parameters
    const opencvParams = reactive({
      cannyThreshold1: 50,
      cannyThreshold2: 150,
      houghAccumulatorThreshold: 50,
    })

    // Backend Response Data
    const processedFrame = ref('')
    const recognizedValue = ref(null)

    // Gemini AI State
    const isSuggestingParams = ref(false)
    const geminiStatus = ref(null)

    // --- LIFECYCLE HOOKS ---

    onMounted(() => {
      console.log('Attempting to connect to backend at', BACKEND_URL)
      socket.value = io(BACKEND_URL, { transports: ['websocket'] })

      socket.value.on('connect', () => {
        console.log('Connected to backend. Socket ID:', socket.value.id)
        isSocketConnected.value = true
        statusMessage.value = 'Connected'
      })

      socket.value.on('disconnect', () => {
        console.warn('Disconnected from backend.')
        isSocketConnected.value = false
        statusMessage.value = 'Disconnected. Check backend server.'
        stopProcessing()
      })

      socket.value.on('processed_frame', (data) => {
        if (data.image) {
          processedFrame.value = `data:image/jpeg;base64,${data.image}`
        }
        if (data.value !== undefined) {
          recognizedValue.value = parseFloat(data.value).toFixed(4)
        }
      })

      socket.value.on('connect_error', (err) => {
        console.error('Connection Error:', err.message)
        statusMessage.value = 'Connection Failed'
      })
    })

    onBeforeUnmount(() => {
      if (socket.value) socket.value.disconnect()
      if (processingInterval.value) clearInterval(processingInterval.value)
    })

    // --- WATCHERS ---

    watch(
      opencvParams,
      (newParams) => {
        if (isSocketConnected.value) {
          socket.value.emit('update_settings', { params: newParams })
        }
      },
      { deep: true },
    )

    watch(fps, (newFps) => {
      if (isSocketConnected.value) {
        socket.value.emit('update_settings', { fps: newFps })
      }
      if (isProcessing.value) {
        stopProcessing()
        startProcessing()
      }
    })

    // --- METHODS ---



    const onVideoLoaded = () => {
      isVideoReady.value = true
    }

    const startProcessing = () => {
      if (!videoFile.value || !videoPlayer.value || !isSocketConnected.value) return
      isProcessing.value = true
      processedFrame.value = ''
      recognizedValue.value = null
      socket.value.emit('update_settings', { params: opencvParams, fps: fps.value })
      videoPlayer.value.play()
      processingInterval.value = setInterval(() => {
        if (videoPlayer.value.paused || videoPlayer.value.ended) {
          stopProcessing()
          return
        }
        captureAndSendFrame()
      }, 1000 / fps.value)
    }

    const stopProcessing = () => {
      if (processingInterval.value) clearInterval(processingInterval.value)
      if (videoPlayer.value) videoPlayer.value.pause()
      isProcessing.value = false
      processingInterval.value = null
      console.log('Processing stopped.')
    }

    const captureAndSendFrame = (asBase64Callback = null) => {
      const video = videoPlayer.value
      const canvas = canvasElement.value
      if (!video || !canvas) return

      const context = canvas.getContext('2d')
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      context.drawImage(video, 0, 0, canvas.width, canvas.height)

      if (asBase64Callback) {
        const base64Data = canvas.toDataURL('image/jpeg').split(',')[1]
        asBase64Callback(base64Data)
      } else {
        canvas.toBlob(
          (blob) => {
            if (socket.value && isSocketConnected.value) {
              socket.value.emit('video_frame', blob)
            }
          },
          'image/jpeg',
          0.9,
        )
      }
    }

    const suggestParameters = async () => {
      if (!videoFile.value || !isVideoReady.value) {
        alert('Please upload a video and wait for it to load.')
        return
      }

      isSuggestingParams.value = true
      geminiStatus.value = { message: 'Analyzing frame...', isError: false }

      videoPlayer.value.currentTime = 0

      setTimeout(() => {
        captureAndSendFrame(async (base64ImageData) => {
          if (!base64ImageData) {
            geminiStatus.value = { message: 'Frame capture failed.', isError: true }
            isSuggestingParams.value = false
            return
          }

          const apiKey = ''
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

            const jsonString = text
              .replace(/```json/g, '')
              .replace(/```/g, '')
              .trim()
            const suggestedParams = JSON.parse(jsonString)

            opencvParams.cannyThreshold1 = suggestedParams.cannyThreshold1
            opencvParams.cannyThreshold2 = suggestedParams.cannyThreshold2
            opencvParams.houghAccumulatorThreshold = suggestedParams.houghAccumulatorThreshold

            geminiStatus.value = { message: 'Parameters updated!', isError: false }
          } catch (error) {
            console.error('Gemini API Error:', error)
            geminiStatus.value = { message: 'AI suggestion failed.', isError: true }
          } finally {
            isSuggestingParams.value = false
          }
        })
      }, 200)
    }

</script>

<style scoped>
/* Custom sparkle animation */
.sparkle-button {
  position: relative;
  overflow: hidden;
}

/* Fade transition for processed frame */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
