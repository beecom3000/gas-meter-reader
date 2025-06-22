<template>
  <q-card class="q-pa-sm bg-grey-2">
    <q-btn
      @click="store.applyParamsChange(videoPlayer, canvasElement)"
      :disable="!store.videoFile || store.isApplyingParams"
      color="deep-purple"
      class="sparkle-button q-mb-sm full-width"
      :loading="store.isApplyingParams"
    >
      ✨ Apply Changes
    </q-btn>

    <div class="q-mb-sm" v-for="field in fields" :key="field.name">
      <div class="text-caption">{{ field.label }}</div>
      <div class="row items-center">
        <q-slider
          :id="field.name"
          v-model="modelValue[field.name]"
          :min="field.min"
          :max="field.max"
          :step="field.step"
          :disable="store.isProcessing"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ modelValue[field.name] }}
        </q-badge>
      </div>
    </div>
  </q-card>
</template>

<script setup lang="ts">
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'

export interface HoughCircleConfig {
  dp: number
  minDist: number
  param1: number
  param2: number
  minRadius: number
  maxRadius: number
}

const store = useGasAnalyzerStore()

interface Field {
  name: string;
  label: string;
  min: number;
  max: number;
  step: number;
}

interface Props {
  fields: Field[],
  modelValue: any
}

const props = defineProps<Props>();

const emit = defineEmits(['update:modelValue']);

function updateField(fieldName: string, value: object) {
  // Create a new object with updated value
  const updatedModel = {
    ...props.modelValue,
    [fieldName]: value
  };

  // Emit the updated model to parent
  emit('update:modelValue', updatedModel);
}
</script>
