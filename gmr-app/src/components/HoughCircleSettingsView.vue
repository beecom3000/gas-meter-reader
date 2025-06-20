<template>
  <q-item-label header>DP: {{ model.dp }}</q-item-label>
  <q-item dense>
    <q-item-section>
      <q-slider color="teal" v-model="model.dp" :min="0.0" :max="1.0" :step="0.1" />
    </q-item-section>
  </q-item>

  <q-item-label header>Min Dist: {{ model.minDist }}</q-item-label>
  <q-item dense>
    <q-item-section>
      <q-slider color="teal" v-model="model.minDist" :step="0" />
    </q-item-section>
  </q-item>

  <q-item-label header>Param1: {{ model.param1 }}</q-item-label>
  <q-item dense>
    <q-item-section>
      <q-slider color="teal" v-model="model.param1" :step="0" />
    </q-item-section>
  </q-item>

  <q-item-label header>Param2: {{ model.param2 }}</q-item-label>
  <q-item dense>
    <q-item-section>
      <q-slider color="teal" v-model="model.param2" :step="0" />
    </q-item-section>
  </q-item>

  <q-item-label header>Min Radius: {{ model.minRadius.toFixed(2) }}</q-item-label>
  <q-item dense>
    <q-item-section>
      <q-slider color="teal" v-model="model.minRadius" :step="0" />
    </q-item-section>
  </q-item>

  <q-item-label header>Max Radius: {{ model.maxRadius.toFixed(2) }}</q-item-label>
  <q-item dense>
    <q-item-section>
      <q-slider color="teal" v-model="model.maxRadius" :step="0" />
    </q-item-section>
  </q-item>
</template>

<script setup lang="ts">
import type { HoughCircleConfig } from '@/models/dial-detection-config.ts'

const model = defineModel<HoughCircleConfig>()
</script>
