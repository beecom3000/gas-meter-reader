<template>
  <div class="video-processor">
    <div class="row">
      <q-btn icon="settings" label="Dial Detection" @click="showDialConfigDialog = true"></q-btn>
      <q-dialog v-model="showDialConfigDialog">
        <DialDetectionSettingsView></DialDetectionSettingsView>
      </q-dialog>
    </div>
    <div class="row">
<!--      <q-input v-model="text" label="Preset name" />-->
      <q-btn icon="save" color="primary" label="Save Preset" @click="savePreset"></q-btn>
      <q-dialog v-model="showPresetDialog">
        <DialDetectionSettingsView></DialDetectionSettingsView>
      </q-dialog>
    </div>
    <div class="row">
      <div class="col-5">
        <div class="row q-pb-md">
          <q-file
              v-model="videoFile"
              label="Choose a Video"
              accept="video/*"
              outlined
              use-chips
              @update:model-value="handleFileUpload"
          ></q-file>
        </div>
        <div class="row q-pa-md">
          <video ref="videoPreview" controls muted v-if="videoFile" class="video-preview"></video>
        </div>
      </div>
      <div class="col-2 q-pa-md">
        <div class="row">
          <div class="controls q-pb-sm">
            <q-btn
                color="primary"
                @click="processVideo"
                :disabled="!isConnected || !videoFile || isProcessing"
            >
              Process
            </q-btn>
            <q-btn
              @click="cancelProcessing"
              :disabled="!isProcessing"
            >
              Cancel
            </q-btn>
          </div>
        </div>
        <div class="row">
          <div class="progress" v-if="isProcessing">
            Processing: {{ Math.round(processingProgress) }}%
            <progress :value="processingProgress" max="100"></progress>
          </div>

          <div class="stage-controls" v-show="isConnected && videoFile">
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
    <div class="row">
      <StatusBar
          :is-connected="isConnected"
      ></StatusBar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useSocketVideo } from '@/composables/use-socket-video.ts'
import type { Metadata } from '@/models/metadata.ts'
import DialDetectionSettingsView from "@/views/DialDetectionSettingsView.vue";
import StatusBar from "@/components/StatusBar.vue";

const {
  connect,
  captureFramesAndSend,
  updateStage,
  registerProcessor,
  cancelProcessing,
  disconnect,
  isConnected,
  currentStage,
  processingProgress,
  isProcessing,
  videoPreview
} = useSocketVideo()

const videoFile = ref<File | null>(null)
const outputCanvas = ref<HTMLCanvasElement | null>(null)
const showDialConfigDialog = ref<boolean>(false);
const showPresetDialog = ref<boolean>(false);

const stages = [
  { id: 'original', value: 'original', label: 'Original (No Processing)' },
  { id: 'grayscale', value: 'grayscale', label: 'Grayscale' },
  { id: 'blur', value: 'blur', label: 'Blur' },
  { id: 'threshold', value: 'threshold', label: 'Threshold' },
  { id: 'canny_edge', value: 'canny_edge', label: 'Canny Edge' },
  { id: 'detection', value: 'detection', label: 'Object Detection' },
  { id: 'final', value: 'final', label: 'Final' }
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

const savePreset = () => {
  alert('Not implemented yet')
}

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

const handleFileUpload = (file: File) => {
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

const processVideo = async () => {
  if (videoFile.value && videoPreview.value) {
    isProcessing.value = true;
    const mediaElement: HTMLMediaElement = (videoPreview.value as HTMLMediaElement);
    const stream: MediaStream = mediaElement.captureStream()
    await captureFramesAndSend(stream)
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

.video-preview {
  max-width: 100%
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

