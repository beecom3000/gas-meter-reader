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
        <div class="row justify-between items-center q-mb-sm">
          <div class="text-caption">4. OpenCV Parameters</div>
          <q-badge
            :color="store.geminiStatus?.isError ? 'negative' : 'positive'"
            rounded
            v-if="store.geminiStatus"
          >
            {{ store.geminiStatus.message }}
          </q-badge>
        </div>
        <q-card class="q-pa-sm bg-grey-2">
          <q-btn
            @click="store.suggestParameters(videoPlayer, canvasElement)"
            :disable="!store.videoFile || store.isSuggestingParams"
            color="deep-purple"
            class="sparkle-button q-mb-sm full-width"
            :loading="store.isSuggestingParams"
          >
            ✨ Suggest with Gemini
          </q-btn>

          <!-- Canny Edge Detection Threshold 1 -->
          <div class="q-mb-sm">
            <div class="text-caption">Canny Threshold 1</div>
            <div class="row items-center">
              <q-slider
                v-model="store.opencvParams.cannyThreshold1"
                :min="0"
                :max="255"
                :disable="store.isProcessing"
                class="q-mr-sm"
              />
              <q-badge color="white" text-color="black" class="q-px-sm">
                {{ store.opencvParams.cannyThreshold1 }}
              </q-badge>
            </div>
          </div>

          <!-- Canny Edge Detection Threshold 2 -->
          <div class="q-mb-sm">
            <div class="text-caption">Canny Threshold 2</div>
            <div class="row items-center">
              <q-slider
                v-model="store.opencvParams.cannyThreshold2"
                :min="0"
                :max="255"
                :disable="store.isProcessing"
                class="q-mr-sm"
              />
              <q-badge color="white" text-color="black" class="q-px-sm">
                {{ store.opencvParams.cannyThreshold2 }}
              </q-badge>
            </div>
          </div>

          <!-- Hough Circle Accumulator Threshold -->
          <div>
            <div class="text-caption">Hough Circle Threshold</div>
            <div class="row items-center">
              <q-slider
                v-model="store.opencvParams.houghAccumulatorThreshold"
                :min="10"
                :max="200"
                :disable="store.isProcessing"
                class="q-mr-sm"
              />
              <q-badge color="white" text-color="black" class="q-px-sm">
                {{ store.opencvParams.houghAccumulatorThreshold }}
              </q-badge>
            </div>
          </div>
        </q-card>
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
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'
import { Stage } from '@/models/stage.ts'
import { ref, watch } from 'vue'

const store = useGasAnalyzerStore();
interface Option {
  label: string;
  value: Stage;
}

const selectedProcessingStage = ref<Stage>(Stage.final);
watch(selectedProcessingStage, (newValue) => {
  store.updateProcessingStage(newValue)
})

const processingStageOptions: Option[] =
  Object.entries(Stage)
    .map(([key, value]) => ({ label: key, value: value }));
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
