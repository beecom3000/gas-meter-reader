<template>
  <div>
    <div class="video-container">
      <h2>Real-time Gas Meter Reading</h2>
      <video ref="webcamRef" class="webcam" autoplay playsinline muted></video>
      <canvas
        ref="previewCanvas"
        :width="canvasWidth"
        :height="canvasHeight"
      ></canvas>
      <div class="controls">
        <button @click="startProcessing" :disabled="isProcessing">Start</button>
        <button @click="stopProcessing" :disabled="!isProcessing">Stop</button>
        <div class="stats">
          <span>FPS: {{ currentFPS.toFixed(1) }}</span>
          <span>Latency: {{ processingLatency }}ms</span>
        </div>
      </div>
    </div>

    <div>Status Message: {{ systemMessage }}</div>
  </div>
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserMedia } from '@vueuse/core'
import { useSocketIo } from '@/composables/use-socket-io.ts'
import type { Metadata } from '@/models/metadata.ts'
import { useVideoMedia } from '@/composables/use-video-media.ts'

const webcamRef = ref<HTMLVideoElement | null>(null)
const isProcessing = ref<boolean>(false)
const processingInterval = ref<number | null>(null)

const canvasWidth = ref<number>(640)
const canvasHeight = ref<number>(480)

const processingLatency = ref<number>(0)

const startButtonClass = ref<string>('button-enabled')
const stopButtonClass = ref<string>('button-disabled')

const systemMessage = ref<string>('None')

const { stream, start, stop } = useUserMedia({
  constraints: { video: true, audio: false },
})

const { socket, initSocket , terminateSocket} = useSocketIo();
const { previewCanvas, currentFPS, handleResponse } = useVideoMedia();

onMounted(async () => {
  await start()
  if (stream.value && webcamRef.value) {
    webcamRef.value.srcObject = stream.value
  }

  await initSocket('localhost', 9092, 'api/v1/face', false);

  // forceBase64: false, // Critical for binary transfer
  //   parser: {
  //   decodeResponse: false // Prevent auto-parsing
  // }
  // socket.value.emit('test-load', 'Hello From The Client');
  socket.value.on('processed-frame', (metadata: Metadata, data: ArrayBuffer) => {
    handleResponse(metadata, data);
  })
  // socket.value.on('test-frame', (metadata, data) => {
  //   const uint8Array = new Uint8Array(data.slice(1))
  //   const isJpg = isJpeg(uint8Array);
  //   renderImage(metadata, uint8Array);
  // });

  systemMessage.value = 'Connected and Ready'
})

onBeforeUnmount(() => {
  stopProcessing()
  stop()
  terminateSocket();
})

const startProcessing = () => {
  if (isProcessing.value) return

  isProcessing.value = true
  const canvas1 = previewCanvas.value!
  const webcam = webcamRef.value!

  // Set canvas dimensions to match video
  canvas1.width = webcam.videoWidth
  canvas1.height = webcam.videoHeight

  // Send frames at 15fps (adjust as needed)
  processingInterval.value = setInterval(() => {
    captureAndSendFrame(canvas1)
  }, 66) // ~15fps

  stopButtonClass.value = 'button-enabled'
  startButtonClass.value = 'button-disabled'
}

const stopProcessing = () => {
  isProcessing.value = false
  if (processingInterval.value) {
    clearInterval(processingInterval.value)
    processingInterval.value = null
  }
  stopButtonClass.value = 'button-disabled'
  startButtonClass.value = 'button-enabled'
}

const assert = <T,>(obj: T | null, message: string) => {
  if (!obj) {
    console.warn(message)
    systemMessage.value = message
    throw new Error(message)
  }
}

const captureAndSendFrame = (canvas: HTMLCanvasElement) => {
  const video = webcamRef.value!

  assert<HTMLCanvasElement>(canvas, 'Could not get canvas element')

  const ctx: CanvasRenderingContext2D | null = canvas!.getContext('2d')

  assert<CanvasRenderingContext2D>(ctx, 'Could not get canvas context')

  // Draw current video frame to canvas
  ctx!.drawImage(video, 0, 0, canvas!.width, canvas!.height)

  // Get image as Blob (binary data)
  canvas!.toBlob(
    (blob: Blob | null) => {
      if (!blob) return

      const reader = new FileReader()

      reader.onload = () => {
        // Send ArrayBuffer directly to the server
        socket.value!.emit('frame', {
          frame: reader.result, // ArrayBuffer
          width: canvas!.width,
          height: canvas!.height,
          timestamp: Date.now(),
        })
      }

      // Quality parameter (0.7 = 70% quality)
      reader.readAsArrayBuffer(blob)
    },
    'image/jpeg',
    0.7,
  ) // Adjust quality as needed
}

interface ResponseData {
  frame: ArrayBuffer
  timestamp: number
}

const isJpeg = (buffer: Uint8Array) => {
  if (!buffer || buffer.length < 3) {
    return false
  }

  return buffer[0] === 255 && buffer[1] === 216 && buffer[2] === 255
  // return uint8Array[0] === 0xFF && uint8Array[1] === 0xD8 && uint8Array[2] === 0xFF;
}

</script>

<style scoped>
.video-container {
  position: relative;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
}

video,
canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

canvas {
  z-index: 10;
}

.controls {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 16px;
  align-items: center;
  z-index: 20;
}

button {
  padding: 10px 20px;
  background: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}

button:disabled {
  background: #cccccc;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  background: #45a049;
}

.stats {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: monospace;
}

.stats span {
  margin: 0 8px;
}
</style>
