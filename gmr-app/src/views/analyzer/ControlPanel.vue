<template>
  <q-card class="q-pa-md column" style="height: fit-content">
    <!-- File Upload -->
    <div class="q-mb-md">
      <q-file
        filled
        label="1. Upload Video File"
        accept="video/*"
        @update:model-value="handleFileUpload"
        :model-value="videoFile"
        class="q-mb-md"
      />
    </div>

    <!-- Frame Rate Control -->
    <div class="q-mb-md">
      <div class="text-caption q-mb-xs">2. Frames Per Second (FPS) to Send</div>
      <div class="row items-center">
        <q-slider
          v-model="fps"
          :min="1"
          :max="30"
          :disable="isProcessing"
          class="q-mr-md"
        />
        <q-badge color="primary" class="q-px-sm">{{ fps }}</q-badge>
      </div>
    </div>

    <!-- OpenCV Parameters -->
    <div class="q-mb-md">
      <div class="row justify-between items-center q-mb-sm">
        <div class="text-caption">3. OpenCV Parameters</div>
        <q-badge
          :color="geminiStatus?.isError ? 'negative' : 'positive'"
          rounded
          v-if="geminiStatus"
        >
          {{ geminiStatus.message }}
        </q-badge>
      </div>
      <q-card class="q-pa-sm bg-grey-2">
        <q-btn
          @click="suggestParameters"
          :disable="!videoFile || isSuggestingParams"
          color="deep-purple"
          class="sparkle-button q-mb-sm full-width"
          :loading="isSuggestingParams"
        >
          ✨ Suggest with Gemini
        </q-btn>

        <!-- Canny Edge Detection Threshold 1 -->
        <div class="q-mb-sm">
          <div class="text-caption">Canny Threshold 1</div>
          <div class="row items-center">
            <q-slider
              v-model="opencvParams.cannyThreshold1"
              :min="0"
              :max="255"
              :disable="isProcessing"
              class="q-mr-sm"
            />
            <q-badge color="white" text-color="black" class="q-px-sm">
              {{ opencvParams.cannyThreshold1 }}
            </q-badge>
          </div>
        </div>

        <!-- Canny Edge Detection Threshold 2 -->
        <div class="q-mb-sm">
          <div class="text-caption">Canny Threshold 2</div>
          <div class="row items-center">
            <q-slider
              v-model="opencvParams.cannyThreshold2"
              :min="0"
              :max="255"
              :disable="isProcessing"
              class="q-mr-sm"
            />
            <q-badge color="white" text-color="black" class="q-px-sm">
              {{ opencvParams.cannyThreshold2 }}
            </q-badge>
          </div>
        </div>

        <!-- Hough Circle Accumulator Threshold -->
        <div>
          <div class="text-caption">Hough Circle Threshold</div>
          <div class="row items-center">
            <q-slider
              v-model="opencvParams.houghAccumulatorThreshold"
              :min="10"
              :max="200"
              :disable="isProcessing"
              class="q-mr-sm"
            />
            <q-badge color="white" text-color="black" class="q-px-sm">
              {{ opencvParams.houghAccumulatorThreshold }}
            </q-badge>
          </div>
        </div>
      </q-card>
    </div>

    <!-- Action Buttons -->
    <div class="q-pt-md">
      <q-btn
        @click="startProcessing"
        :disable="!videoFile || isProcessing || !isSocketConnected"
        color="primary"
        class="full-width q-mb-sm"
        size="lg"
      >
        {{ isProcessing ? 'Processing...' : 'Start Processing' }}
      </q-btn>
      <q-btn
        @click="stopProcessing"
        :disable="!isProcessing"
        color="negative"
        class="full-width"
      >
        Stop
      </q-btn>
    </div>
  </q-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const fps = ref<number>(5)

const videoFile = ref<File | null>(null)
const videoSrc = ref('')

const handleFileUpload = (file: File) => {
  if (file && file.type.startsWith('video/')) {
    videoFile.value = file
    videoSrc.value = URL.createObjectURL(file)
    processedFrame.value = ''
    recognizedValue.value = null
    isVideoReady.value = false
    geminiStatus.value = null
  } else {
    videoFile.value = null
    videoSrc.value = ''
    alert('Please select a valid video file.')
  }
}
</script>
