<template>
    <q-card style="width: 450px" class="q-px-sm q-pb-md">
      <q-card-section class="bg-blue text-white">
        <div class="text-h6">Dial Detection Settings</div>
      </q-card-section>

      <q-item-label header>Detection Method</q-item-label>
      <q-item dense>
        <q-item-section>
          <q-select v-model="dialDetectionConfig.method" :options="detectionMethods" />
        </q-item-section>
      </q-item>

      <q-item-label header>Sensitivity: {{ dialDetectionConfig.param2 }}</q-item-label>
      <q-item dense>
        <q-item-section>
          <q-slider color="teal" v-model="dialDetectionConfig.param2" :step="0" />
        </q-item-section>
      </q-item>

      <q-item-label header>Min Radius: {{ dialDetectionConfig.minRadiusFactor.toFixed(2) }}</q-item-label>
      <q-item dense>
        <q-item-section>
          <q-slider color="teal" v-model="dialDetectionConfig.minRadiusFactor" :step="0" />
        </q-item-section>
      </q-item>

      <q-item-label header>Max Radius: {{ dialDetectionConfig.maxRadiusFactor.toFixed(2) }}</q-item-label>
      <q-item dense>
        <q-item-section>
          <q-slider color="teal" v-model="dialDetectionConfig.maxRadiusFactor" :step="0" />
        </q-item-section>
      </q-item>

      <div class="q-pa-md q-gutter-sm" @click="updateDialConfig">
        <q-btn label="Apply Change" icon="autorenew" color="primary" @click="updateDialConfig"/>
        <q-btn label="Reset Defaults" icon="replay" color="secondary" @click="resetDialConfig" />
      </div>
    </q-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { DialDetectionConfig } from '@/models/dial-detection-config.ts'
import { useNotification } from '@/composables/use-notification.ts'

const detectionMethods = [
  { label: 'Combined Approach', value: 'COMBINED' },
  { label: 'Hough Circle Detection', value: 'HOUGH' },
  { label: 'Contour Detection<', value: 'CONTOUR' },
  { label: 'Edge Detection', value: 'EDGE' },
]

const dialDetectionConfig = reactive<DialDetectionConfig>({
  method: 'COMBINED',
  param2: 30,
  minRadiusFactor: 0.3,
  maxRadiusFactor: 0.5,
  edgeDetectionThreshold: 150,
  contourCircularThreshold: 0.85
})

const {
  showNotification
} = useNotification();

const updateDialConfig = async () => {
  // Simulate API call
  await new Promise(resolve => setTimeout(resolve, 300));
  lastUpdate.value = new Date().toLocaleTimeString();
  showNotification('success', 'Failed to update dial configuration')
}

const resetDialConfig = () => {
  Object.assign(dialDetectionConfig, {
    method: 'COMBINED',
    param2: 30,
    minRadiusFactor: 0.3,
    maxRadiusFactor: 0.5,
    edgeDetectionThreshold: 150,
    contourCircularThreshold: 0.85
  })
}

</script>
