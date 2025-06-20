<template>
  <q-layout view="hHh Lpr lff">
    <q-page-container>
      <q-page class="q-pa-md">
        <div class="q-mb-md">
          <header-panel></header-panel>
        </div>

        <div class="row q-col-gutter-md">
          <!-- Left Column: Controls & Parameters -->
          <div class="col-12 col-md-4">
            <control-panel></control-panel>
          </div>

          <!-- Right Column: Video Displays -->
          <div class="col-12 col-md-8">
            <display-panel></display-panel>
          </div>
        </div>

        <!-- Status Bar -->
        <div class="q-mt-md text-center text-caption text-grey-6">
          <status-bar-panel></status-bar-panel>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'
import HeaderPanel from '@/views/analyzer/HeaderPanel.vue'
import DisplayPanel from '@/views/analyzer/DisplayPanel.vue'
import ControlPanel from '@/views/analyzer/ControlPanel.vue'
import StatusBarPanel from '@/views/analyzer/StatusBarPanel.vue'

const store = useGasAnalyzerStore();

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
</script>

<style scoped>
</style>
