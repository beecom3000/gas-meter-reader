<template>
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
</template>

<script setup lang="ts">
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'

const store = useGasAnalyzerStore();
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
