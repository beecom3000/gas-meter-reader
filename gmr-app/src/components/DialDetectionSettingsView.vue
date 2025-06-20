<template>
    <q-card style="width: 450px" class="q-px-sm q-pb-md">
      <q-card-section class="bg-blue text-white">
        <div class="text-h6">Dial Detection Settings</div>
      </q-card-section>

      <q-item-label header>General</q-item-label>
      <q-item tag="label">
        <q-item-section>
          <q-checkbox v-model="dialDetectionConfig.adaptiveThreshold" />
        </q-item-section>
        <q-item-section>
          <q-item-label>Adaptive Threshold</q-item-label>
        </q-item-section>
      </q-item>

      <q-item tag="label">
        <q-item-section>
          <q-checkbox v-model="dialDetectionConfig.morphology" />
        </q-item-section>
        <q-item-section>
          <q-item-label>Morphology</q-item-label>
        </q-item-section>
      </q-item>

      <q-item tag="label">
        <q-item-section>
          <q-input v-model="dialDetectionConfig.blurKernelSize"
                   label="Blur Kernel Size"
                   />
        </q-item-section>
      </q-item>

      <q-item tag="label">
        <q-item-section>
          <q-input v-model="dialDetectionConfig.thresholdValue"
                   label="Threshold Value"
          />
        </q-item-section>
      </q-item>

      <q-separator spaced />

      <q-item-label header>Detection Method</q-item-label>
      <q-item dense>
        <q-item-section>
          <q-select :model-value="dialDetectionConfig.method"
                    @update:model-value="onDetectionMethodChange"
                    :options="detectionMethodOptions"
          />
        </q-item-section>
      </q-item>

      <template v-if="dialDetectionConfig.method === 'HOUGH'">

        <q-item-label header>DP: {{ houghCircleConfig.dp }}</q-item-label>
        <q-item dense>
          <q-item-section>
            <q-slider color="teal" v-model="houghCircleConfig.dp" :min="0.0" :max="1.0" :step="0.1" />
          </q-item-section>
        </q-item>

        <q-item-label header>Min Dist: {{ houghCircleConfig.minDist }}</q-item-label>
        <q-item dense>
          <q-item-section>
            <q-slider color="teal" v-model="houghCircleConfig.minDist" :step="0" />
          </q-item-section>
        </q-item>

        <q-item-label header>Param1: {{ houghCircleConfig.param1 }}</q-item-label>
        <q-item dense>
          <q-item-section>
            <q-slider color="teal" v-model="houghCircleConfig.param1" :step="0" />
          </q-item-section>
        </q-item>

        <q-item-label header>Param2: {{ houghCircleConfig.param2 }}</q-item-label>
        <q-item dense>
          <q-item-section>
            <q-slider color="teal" v-model="houghCircleConfig.param2" :step="0" />
          </q-item-section>
        </q-item>

        <q-item-label header>Min Radius: {{ houghCircleConfig.minRadius.toFixed(2) }}</q-item-label>
        <q-item dense>
          <q-item-section>
            <q-slider color="teal" v-model="houghCircleConfig.minRadius" :step="0" />
          </q-item-section>
        </q-item>

        <q-item-label header>Max Radius: {{ houghCircleConfig.maxRadius.toFixed(2) }}</q-item-label>
        <q-item dense>
          <q-item-section>
            <q-slider color="teal" v-model="houghCircleConfig.maxRadius" :step="0" />
          </q-item-section>
        </q-item>
      </template>

      <div class="q-pa-md q-gutter-sm">
        <q-btn label="Apply Change" icon="autorenew" color="primary" @click="save"/>
        <q-btn label="Reset Defaults" icon="replay" color="secondary" @click="reset" />
      </div>
    </q-card>
</template>

<script setup lang="ts">
import { inject, reactive } from 'vue'
import {
  type DetectionMethodType,
  type DialDetectionConfig,
  type HoughCircleConfig
} from '@/models/dial-detection-config.ts'
import { useNotification } from '@/composables/use-notification.ts'
import type { Emitter } from 'mitt'
import { emitterKey, type Events } from '@/models/event-bus.ts'
import axios from 'axios'

const emitter: Emitter<Events> = inject(emitterKey)!

interface DetectionMethodOption {
  label: string;
  value: string;
}

const detectionMethodOptions: DetectionMethodOption[] = [
  { label: 'Combined Approach', value: 'COMBINED' },
  { label: 'Hough Circle Detection', value: 'HOUGH' },
  { label: 'Contour Detection', value: 'CONTOUR' },
  { label: 'Edge Detection', value: 'EDGE' },
]

const dialDetectionConfig = reactive<DialDetectionConfig>({
  method: 'COMBINED',
  morphology: false,
  adaptiveThreshold: false,
  cannyEdgeThreshold1: 20.0,
  cannyEdgeThreshold2: 100.0,

  blurKernelSize: 5,
  thresholdValue: 150,

  edgeDetectionThreshold: 5,
  contourCircularThreshold: 10
})

const houghCircleConfig = reactive<HoughCircleConfig>({
  dp: 1.0,
  minDist: 100.0,
  param1: 50,
  param2: 30,
  minRadius: 100,
  maxRadius: 250,
})

const {
  showNotification
} = useNotification();

const onDetectionMethodChange = (selectedOption: DetectionMethodOption) => {
  dialDetectionConfig.method = selectedOption.value as DetectionMethodType
}

const save = async () => {
  try {
    let response = await axios.put(
      'http://localhost:8080/gas/api/v1/config/dial',
      { ...dialDetectionConfig }
    )
    response = await axios.put(
      'http://localhost:8080/gas/api/v1/config/dial/hough',
      { ...houghCircleConfig }
    )
    emitter.emit('last-update', { timestamp: new Date() })
    showNotification('success', `Config has been saved successfully.`);
  } catch (error: Error | unknown) {
    showNotification(
      'error',
      `Error during save dial configuration: ${error instanceof Error ? error.message : error}`
    );
  }
}

const reset = () => {
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
