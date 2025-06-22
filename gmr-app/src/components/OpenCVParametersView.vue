<template>
  <div class="row justify-between items-center q-mb-sm">
    <div class="text-caption">4. OpenCV Parameters</div>

    <q-badge
      :color="'positive'"
      rounded
    >
      Test
    </q-badge>
  </div>
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

    <div class="q-mb-sm">
      <div class="text-caption">DP</div>
      <div class="row items-center">
        <q-slider
          v-model="store.opencvParams.houghCircleConfig!.dp"
          :min="0"
          :max="1"
          :step="0.1"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ store.opencvParams.houghCircleConfig!.dp }}
        </q-badge>
      </div>
    </div>
    <div class="q-mb-sm">
      <div class="text-caption">Min Dist</div>
      <div class="row items-center">
        <q-slider
          v-model="store.opencvParams.houghCircleConfig!.minDist"
          :min="0"
          :max="255"
          :step="0.1"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ store.opencvParams.houghCircleConfig!.minDist }}
        </q-badge>
      </div>
    </div>
    <div class="q-mb-sm">
      <div class="text-caption">Param1</div>
      <div class="row items-center">
        <q-slider
          v-model="store.opencvParams.houghCircleConfig!.param1"
          :min="0"
          :max="1000"
          :step="10"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ store.opencvParams.houghCircleConfig!.param1 }}
        </q-badge>
      </div>
    </div>

    <div class="q-mb-sm">
      <div class="text-caption">Param2</div>
      <div class="row items-center">
        <q-slider
          v-model="store.opencvParams.houghCircleConfig!.param2"
          :min="0"
          :max="1000"
          :step="10"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ store.opencvParams.houghCircleConfig!.param2 }}
        </q-badge>
      </div>
    </div>

    <div class="q-mb-sm">
      <div class="text-caption">Min Radius</div>
      <div class="row items-center">
        <q-slider
          v-model="store.opencvParams.houghCircleConfig!.minRadius"
          :min="0"
          :max="1000"
          :step="10"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ store.opencvParams.houghCircleConfig!.minRadius }}
        </q-badge>
      </div>
    </div>

    <div class="q-mb-sm">
      <div class="text-caption">Max Radius</div>
      <div class="row items-center">
        <q-slider
          v-model="store.opencvParams.houghCircleConfig!.maxRadius"
          :min="0"
          :max="1000"
          :step="10"
          class="q-mr-sm"
        />
        <q-badge color="white" text-color="black" class="q-px-sm">
          {{ store.opencvParams.houghCircleConfig!.maxRadius }}
        </q-badge>
      </div>
    </div>
  </q-card>
</template>

<script setup lang="ts">
import { useGasAnalyzerStore } from '@/stores/use-gas-analyzer-store.ts'

const store = useGasAnalyzerStore();
interface Props {
  videoPlayer: HTMLVideoElement | null;
  canvasElement: HTMLCanvasElement | null;
}

defineProps<Props>();
</script>
