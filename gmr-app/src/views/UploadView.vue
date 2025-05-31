<template>
  <div class="video-processor">
    <div class="row">
      <div class="col-5">
        <div class="file-upload">
          <input type="file" accept="video/*" @change="handleFileUpload">
          <video ref="videoPreview" controls muted v-if="videoFile" style="max-width: 100%"></video>
        </div>
      </div>
      <div class="col-2">
        <div class="row">
          <div class="controls">
            <button
              @click="processVideo"
              :disabled="!isConnected || !videoFile || isProcessing"
            >
              Process Video
            </button>
            <button
              @click="cancelProcessing"
              :disabled="!isProcessing"
            >
              Cancel
            </button>
          </div>
          <div>
            <span>Status: {{ isConnected ? 'Connected' : 'Disconnected' }}</span>
          </div>
        </div>
        <div class="row">
          <div class="progress" v-if="isProcessing">
            Processing: {{ Math.round(processingProgress) }}%
            <progress :value="processingProgress" max="100"></progress>
          </div>

          <div class="stage-controls" v-show="videoFile">
            <h6>Processing Stage:</h6>
            <div v-for="stage in stages" :key="stage.value" class="radio-option">
              <input
                type="radio"
                :id="stage.value"
                :value="stage.value"
                v-model="currentStage"
                @change="setStage(currentStage)"
              >
              <label :for="stage.value">{{ stage.label }}</label>
            </div>
          </div>
        </div>
      </div>
      <div class="col-5">
        <div class="output">
          <h5>Preview stage ({{ currentStage }})</h5>
          <canvas ref="outputCanvas"></canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useSocketVideo } from '@/composables/use-socket-video.ts'
import type { Metadata } from '@/models/metadata.ts'

const {
  connect,
  sendVideo,
  setStage,
  registerProcessor,
  cancelProcessing,
  isConnected,
  currentStage,
  processingProgress,
  isProcessing
} = useSocketVideo()

const videoFile = ref<File | null>(null)
const videoPreview = ref<HTMLVideoElement | null>(null)
const outputCanvas = ref<HTMLCanvasElement | null>(null)

const stages = [
  { value: 'original', label: 'Original (No Processing)' },
  { value: 'grayscale', label: 'Grayscale' },
  { value: 'blur', label: 'Blur' },
  { value: 'canny_edge', label: 'Canny Edge' },
  { value: 'detection', label: 'Object Detection' }
]

// Initialize
onMounted(() => {
  connect()

  registerProcessor((metadata: Metadata, data: ArrayBuffer) => {
    displayFrame(metadata, data)
  })
})

const displayFrame = (metadata: Metadata, data: ArrayBuffer) => {
  if (!outputCanvas.value) return

  const blob = new Blob([new Uint8Array(data.slice(1))], { type: 'image/jpeg' })
  const img = new Image()
  img.onload = () => {
    if (outputCanvas.value) {
      outputCanvas.value.width = img.width
      outputCanvas.value.height = img.height
      const ctx: CanvasRenderingContext2D | null = outputCanvas.value.getContext('2d')
      if (ctx) {
        ctx.drawImage(img, 0, 0)
      }
      URL.revokeObjectURL(img.src)
    }
  }
  img.src = URL.createObjectURL(blob)
}

const handleFileUpload = (event: Event) => {
  let file: File;
  const fileInput = event.target as HTMLInputElement;
  if (fileInput && fileInput.files && fileInput.files.length > 0) {
    file = fileInput.files[0]
    if (!file) return
    videoFile.value = file

    // Create preview
    const video: HTMLVideoElement = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      if (videoPreview.value) {
        videoPreview.value.src = URL.createObjectURL(file)
        URL.revokeObjectURL(video.src)
      }
    }
    video.src = URL.createObjectURL(file)
  }
}

const processVideo = async () => {
  if (!videoFile.value) return
  await sendVideo(videoFile.value)
}
</script>

<style scoped>
.video-processor {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 80%;
  margin: 0 auto;
  padding: 2rem;
}

.controls {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.progress {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

progress {
  width: 100%;
}

canvas {
  max-width: 100%;
  background: #000;
}

.stage-controls {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
</style>

