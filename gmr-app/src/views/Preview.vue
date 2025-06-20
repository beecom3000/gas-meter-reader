<template>
  <q-layout view="hHh Lpr lff">
    <q-page-container>
      <q-page class="q-pa-md">
        <div class="q-mb-md">
          <q-card class="panel-header">
            <div class="row items-center justify-between">
              <div>
                <h1 class="text-h5 text-weight-bold q-ma-none">Gas Meter Video Analyzer</h1>
                <p class="text-caption text-grey-7 q-mt-sm">Enhanced with Gemini AI</p>
              </div>
              <div class="row items-center status-indicator">
                  <span class="text-caption q-mr-sm">{{ store.connectionStatusMessage }}</span>
                  <q-badge :color="store.isSocketConnected ? 'positive' : 'negative'"
                           rounded
                           :title="store.isSocketConnected ? 'Connected' : 'Disconnected'" />
              </div>
            </div>
          </q-card>
        </div>

        <div class="row q-col-gutter-md">
          <!-- Left Column: Controls & Parameters -->
          <div class="col-12 col-md-4">
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
          </div>

          <!-- Right Column: Video Displays -->
          <div class="col-12 col-md-8">
            <q-card>
              <div class="panel-header">
                <div class="text-subtitle1">Video Analysis</div>
              </div>
              <div class="panel-content">
                <div v-if="!store.videoSrc" class="flex flex-center" style="height: 640px">
                  <p class="text-grey-6">Video preview will appear here</p>
                </div>

                <div v-else class="row q-col-gutter-md">
                  <!-- Original Video -->
                  <div class="col-12 col-sm-6">
                    <div class="text-weight-bold text-center q-mb-sm">Original Video</div>
                    <video ref="videoPlayer"
                           :src="store.videoSrc"
                           controls
                           muted
                           @loadeddata="store.onVideoLoaded"
                           class="full-width rounded-borders shadow-1">
                    </video>
                    <!-- Hidden canvas for frame grabbing -->
                    <canvas ref="canvasElement" class="hidden"></canvas>
                  </div>

                  <!-- Processed Frame -->
                  <div class="col-12 col-sm-6">
                    <div class="text-weight-bold text-center q-mb-sm">Processed by Backend</div>
                    <div class="bg-grey-3 rounded-borders shadow-1 flex flex-center"
                         :style="store.isProcessing ? 'height: auto' : 'height: 480px'">
                      <transition name="fade" mode="out-in">
                        <canvas v-if="store.isProcessing"
                                ref="processedFrameCanvas"
                                :class="!store.isProcessing ? 'hidden' : 'full-height full-width'"
                                style="object-fit: contain">
                        </canvas>
<!--                        <img v-if="store.processedFrame" :src="store.processedFrame" alt="Processed Frame" class="full-height full-width" style="object-fit: contain">-->
                        <p v-else class="text-grey-6 text-caption">Waiting for processed frames...</p>
                      </transition>
                    </div>
                    <div v-if="store.recognizedValue" class="q-mt-md bg-blue-1 rounded-borders q-pa-sm border-left-blue">
                      <p class="text-weight-bold q-ma-none">Recognized Value:</p>
                      <p class="meter-value text-center q-ma-none">{{ store.recognizedValue }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </q-card>
          </div>
        </div>

        <!-- Status Bar -->
        <div class="q-mt-md text-center text-caption text-grey-6">
          <q-icon name="settings" class="q-mr-xs" /> Socket Status:
          <q-badge :color="store.isSocketConnected ? 'positive' : 'negative'">
            {{ store.isSocketConnected ? 'Connected' : 'Disconnected' }}
          </q-badge>
          | FPS: {{ store.fps }} | Processing: {{ store.isProcessing ? 'Active' : 'Inactive' }}
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Stage } from '@/models/stage.ts'
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'

const store = useGasAnalyzerStore();
const videoPlayer = ref<HTMLVideoElement | null>(null);
const canvasElement = ref<HTMLCanvasElement | null>(null);
const processedFrameCanvas = ref<HTMLCanvasElement | null>(null);

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

onMounted(() => {
  store.initSocket();

  // Watch for changes in OpenCV parameters and FPS
  // (mutation: SubscriptionCallbackMutation<>, state: UnwrapRef<>)
  store.$subscribe((mutation, state) => {
    if (mutation.events?.key === 'opencvParams' || mutation.events?.key === 'fps') {
      if (store.isSocketConnected && store.socket) {
        store.socket.emit('update_settings', {
          params: store.opencvParams,
          fps: store.fps
        });
      }
    }
  });
});

onBeforeUnmount(() => {
  if (store.socket) store.socket.disconnect();
  if (store.processingInterval) clearInterval(store.processingInterval);
});

// Watch when processed frame is returned
watch(processedFrameCanvas, (newValue) => {
  if (newValue) {
    store.processedFrame = newValue;
  }
});
</script>

<style scoped>
  /* Custom sparkle animation */
.sparkle-button {
  position: relative;
  overflow: hidden;
}

/* Fade transition for processed frame */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* Custom meter display */
.meter-value {
  font-family: 'Courier New', monospace;
  font-size: 2rem;
  font-weight: bold;
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.status-indicator {
  transition: all 0.3s ease;
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
