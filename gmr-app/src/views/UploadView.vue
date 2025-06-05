<template>
  <div class="video-processor">
    <div class="row">
      <div class="col-5">
        <div class="row q-pb-md">
          <input type="file" accept="video/*" @change="handleFileUpload">
        </div>
        <div class="row q-pa-md">
          <video ref="videoPreview" controls muted v-if="videoFile" style="max-width: 100%"></video>
        </div>
      </div>
      <div class="col-2 q-pa-md">
        <div class="row">
          <div class="controls q-pb-sm">
            <button
              @click="processVideo"
              :disabled="!isConnected || !videoFile || isProcessing"
            >
              Process
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
              <q-radio
                :id="stage.id"
                :val="stage.value"
                v-model="currentStage"
                @update:model-value="updateStage"
                :label="stage.label"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="col-5">
        <div class="output">
          <h5 class="q-pb-md">Preview stage ({{ currentStage }})</h5>
          <canvas class="q-pa-md" ref="outputCanvas"></canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useSocketVideo } from '@/composables/use-socket-video.ts'
import type { Metadata } from '@/models/metadata.ts'

const {
  connect,
  sendVideo,
  updateStage,
  registerProcessor,
  cancelProcessing,
  disconnect,
  isConnected,
  currentStage,
  processingProgress,
  isProcessing
} = useSocketVideo()

const videoFile = ref<File | null>(null)
const videoPreview = ref<HTMLMediaElement | null>(null)
const outputCanvas = ref<HTMLCanvasElement | null>(null)

const stages = [
  { id: 'original', value: 'original', label: 'Original (No Processing)' },
  { id: 'grayscale', value: 'grayscale', label: 'Grayscale' },
  { id: 'blur', value: 'blur', label: 'Blur' },
  { id: 'canny_edge', value: 'canny_edge', label: 'Canny Edge' },
  { id: 'detection', value: 'detection', label: 'Object Detection' }
]

// Initialize
onMounted(() => {
  connect()

  registerProcessor((metadata: Metadata, data: ArrayBuffer) => {
    displayFrame(metadata, data)
  })
})

onUnmounted(() => {
  cancelProcessing()
  disconnect()
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
  if (videoFile.value && videoPreview.value) {
    isProcessing.value = true;
    const mediaElement: HTMLMediaElement = (videoPreview.value as HTMLMediaElement);
    const stream: MediaStream = mediaElement.captureStream()
    await sendVideo(stream)
  }
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

