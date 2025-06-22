<template>
  <q-icon name="settings" class="q-mr-xs" /> Socket Status:
  <q-badge :color="store.isSocketConnected ? 'positive' : 'negative'">
    {{ store.isSocketConnected ? 'Connected' : 'Disconnected' }}
  </q-badge>
  | FPS: {{ store.fps }} | Processing: {{ store.isProcessing ? 'Active' : 'Inactive' }}
  | Server Mood: {{ store.serverStatusMessage }}
</template>

<script setup lang="ts">
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'

const store = useGasAnalyzerStore();
</script>
