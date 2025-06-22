<template>
  <q-card>
    <div class="panel-header">
      <div class="text-subtitle1">Processing Controls</div>
    </div>
    <div class="panel-content">
      <!-- File Upload -->
      <div class="q-mb-md">
        <q-file
          filled
          label="1. Upload Video File"
          accept="video/*"
          @update:model-value="store.handleFileUpload"
          :model-value="store.videoFile"
          class="q-mb-md"
        />
      </div>

      <!-- Processing Stage -->
      <div class="q-mb-md">
        <div class="text-caption q-mb-xs">2. Processing Stage</div>
        <q-chip
          v-for="option in processingStageOptions"
          :key="option.value"
          :label="option.label"
          :color="selectedProcessingStage === option.value ? 'primary' : 'grey-4'"
          :text-color="selectedProcessingStage === option.value ? 'white' : 'dark'"
          :outline="selectedProcessingStage !== option.value"
          clickable
          @click="selectedProcessingStage = option.value"
        />
      </div>

      <!-- Frame Rate Control -->
      <div class="q-mb-md">
        <div class="text-caption q-mb-xs">3. Frames Per Second (FPS) to Send</div>
        <div class="row items-center">
          <q-slider
            v-model="store.fps"
            :min="1"
            :max="30"
            :disable="store.isProcessing"
            class="q-mr-md"
          />
          <q-badge color="primary" class="q-px-sm">{{ store.fps }}</q-badge>
        </div>
      </div>

      <!-- OpenCV Parameters -->
      <div class="q-mb-md">
        <OpenCVParametersView
          :video-player="videoPlayer"
          :canvas-element="canvasElement"
        />
      </div>

      <!-- Action Buttons -->
      <div class="q-pt-md">
        <q-btn
          @click="store.startProcessing(videoPlayer)"
          :disable="!store.videoFile || store.isProcessing || !store.isSocketConnected"
          color="primary"
          class="full-width q-mb-sm"
          size="lg"
        >
          {{ store.isProcessing ? 'Processing...' : 'Start Processing' }}
        </q-btn>
        <q-btn
          @click="store.stopProcessing(videoPlayer)"
          :disable="!store.isProcessing"
          color="negative"
          class="full-width"
        >
          Stop
        </q-btn>
      </div>
    </div>
  </q-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'
import { Stage, toStage } from '@/models/stage.ts'
import OpenCVParametersView from '@/components/OpenCVParametersView.vue'

interface Props {
  videoPlayer: HTMLVideoElement | null;
  canvasElement: HTMLCanvasElement | null;
}

defineProps<Props>();

const store = useGasAnalyzerStore();
interface Option {
  label: string;
  value: string;
}

const selectedProcessingStage = ref<string>(Stage.final);
watch(selectedProcessingStage, (newValue: string) => {
  store.updateProcessingStage(toStage(newValue))
})

const processingStageOptions: Option[] =
  Object.entries(Stage)
    .map(([key, value]) => ({ label: value.toString(), value: key }));
</script>

<style scoped>
/* Custom sparkle animation */
.sparkle-button {
  position: relative;
  overflow: hidden;
}

.panel-header {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4edf5 100%);
  border-radius: 10px 10px 0 0;
  padding: 12px 20px;
  border-bottom: 1px solid #e0e6ed;
}

.panel-content {
  padding: 20px;
  background: white;
  border-radius: 0 0 10px 10px;
}
</style>
