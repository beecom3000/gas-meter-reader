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
            <control-panel
              :video-player="videoPlayer"
              :canvas-element="canvasElement"
            />
          </div>

          <!-- Right Column: Video Displays -->
          <div class="col-12 col-md-8">
            <display-panel ref="displayPanel"></display-panel>
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
import { computed, onBeforeUnmount, onMounted, type UnwrapRef, useTemplateRef, watch } from 'vue'
import { type State, useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'
import HeaderPanel from '@/views/analyzer/HeaderPanel.vue'
import DisplayPanel from '@/views/analyzer/DisplayPanel.vue'
import ControlPanel from '@/views/analyzer/ControlPanel.vue'
import StatusBarPanel from '@/views/analyzer/StatusBarPanel.vue'
import type { SubscriptionCallbackMutation } from 'pinia'
import { DebuggerEventExtraInfo } from '@vue/reactivity'

const store = useGasAnalyzerStore();

const displayPanelRef = useTemplateRef('displayPanel')!

const videoPlayer = computed(() => displayPanelRef.value && displayPanelRef.value!.videoPlayer)
const canvasElement = computed(() => displayPanelRef.value &&  displayPanelRef.value!.canvasElement)

onMounted(() => {
  store.initSocket();

  // Watch for changes in OpenCV parameters and FPS
  // (mutation: SubscriptionCallbackMutation<>, state: UnwrapRef<>)
  store.$subscribe((mutation: SubscriptionCallbackMutation<State>, state: UnwrapRef<State>) => {
    const eventKey = (mutation.events as DebuggerEventExtraInfo).key
    if (eventKey === 'opencvParams' || eventKey === 'fps') {
      if (store.isSocketConnected && store.socket) {
        store.socket.emit('update_settings', {
          params: store.opencvParams,
          fps: store.fps
        });
      }
    }
  });

  /**
   *     $subscribe(callback: SubscriptionCallback<S>, options?: {
   *         detached?: boolean;
   *     } & WatchOptions): () => void;
   */
});

onBeforeUnmount(() => {
  if (store.socket) store.socket.disconnect();
  if (store.processingInterval) clearInterval(store.processingInterval);
});
</script>

<style scoped>
</style>
