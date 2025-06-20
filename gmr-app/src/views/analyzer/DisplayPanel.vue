<template>
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
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'

const store = useGasAnalyzerStore();
const videoPlayer = ref<HTMLVideoElement | null>(null);
const canvasElement = ref<HTMLCanvasElement | null>(null);
const processedFrameCanvas = ref<HTMLCanvasElement | null>(null);

// Watch when processed frame is returned
watch(processedFrameCanvas, (newValue) => {
  if (newValue) {
    store.processedFrame = newValue;
  }
});
</script>

<style scoped>
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
