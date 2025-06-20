<template>
  <q-card class="q-pa-md">
    <div v-if="!videoSrc" class="flex flex-center" style="height: 480px">
      <p class="text-grey-6">Video preview will appear here</p>
    </div>

    <div v-else class="row q-col-gutter-md">
      <!-- Original Video -->
      <div class="col-12 col-sm-6">
        <div class="text-weight-bold text-center q-mb-sm">Original Video</div>
        <video
          ref="videoPlayer"
          :src="videoSrc"
          controls
          @loadeddata="onVideoLoaded"
          class="full-width rounded-borders shadow-1"
        ></video>
        <!-- Hidden canvas for frame grabbing -->
        <canvas ref="canvasElement" class="hidden"></canvas>
      </div>

      <!-- Processed Frame -->
      <div class="col-12 col-sm-6">
        <div class="text-weight-bold text-center q-mb-sm">Processed by Backend</div>
        <div
          class="bg-grey-3 rounded-borders shadow-1 flex flex-center"
          style="height: 270px"
        >
          <transition name="fade" mode="out-in">
            <img
              v-if="processedFrame"
              :src="processedFrame"
              alt="Processed Frame"
              class="full-height full-width"
              style="object-fit: contain"
            />
            <p v-else class="text-grey-6 text-caption">Waiting for processed frames...</p>
          </transition>
        </div>
        <div
          v-if="recognizedValue"
          class="q-mt-md bg-blue-1 rounded-borders q-pa-sm border-left-blue"
        >
          <p class="text-weight-bold q-ma-none">Recognized Value:</p>
          <p class="text-h6 text-center q-ma-none text-monospace">
            {{ recognizedValue }}
          </p>
        </div>
      </div>
    </div>
  </q-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const videoFile = ref(null)
const videoSrc = ref('')
const videoPlayer = ref(null)
const canvasElement = ref(null)
const isVideoReady = ref(false)
</script>
