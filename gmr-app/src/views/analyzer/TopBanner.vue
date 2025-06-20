<template>
  <q-card class="q-pa-md">
    <div class="row items-center justify-between">
      <div>
        <h1 class="text-h5 text-weight-bold q-ma-none">Gas Meter Video Analyzer</h1>
        <p class="text-caption text-grey-7 q-mt-sm">Enhanced with Machien Learning AI</p>
      </div>
      <div class="row items-center">
        <span class="text-caption q-mr-sm">{{ statusMessage }}</span>
        <q-badge
          :color="isSocketConnected ? 'positive' : 'negative'"
          rounded
          :title="isSocketConnected ? 'Connected' : 'Disconnected'"
        />
      </div>
    </div>
  </q-card>
</template>
<script setup lang="ts">
</script>
