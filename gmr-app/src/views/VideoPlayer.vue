<template>
  <div>
    <button @click="toggleFlip">Toggle Flip</button>
  </div>
  <div
    class="video-wrapper"
    :style="{
      width: width + 'px',
      height: height + 'px'
    }"
  >
    <video
      ref="videoRef"
      :src="src"
      class="fixed-video"
      controls
      muted
      @loadedmetadata="handleMetadataLoaded"
      :style="videoStyle"
    >
      Your browser does not support the video tag.
    </video>
    <div v-if="dimensions">
      Video dimensions: {{ dimensions.width }} × {{ dimensions.height }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { VideoDimensions } from '@/models/video-dimensions.ts'
import { useTemplateRef } from 'vue';

const videoRef = useTemplateRef<HTMLVideoElement>('videoRef');

defineExpose({
  videoRef
});

const props = withDefaults(defineProps<{
  src: string;
  width?: number;
  height?: number;
  objectFit?: 'cover' | 'contain' | 'fill' | 'none' | 'scale-down';
}>(), {
  width: 800,
  height: 450,
  objectFit: 'cover'
});

const dimensions = ref<VideoDimensions | null>(null);

const emit = defineEmits<{
  (e: 'loaded', value: VideoDimensions): void
}>();

const isFlipped = ref(false);

const videoStyle = computed(() => ({
  objectFit: props.objectFit,
  transform: isFlipped.value ? 'scaleX(-1) scaleY(-1)' : '',
}));

const toggleFlip = () => {
  isFlipped.value = !isFlipped.value;
};

const handleMetadataLoaded = () => {
  if (videoRef.value) {
    dimensions.value = {
      width: videoRef.value.videoWidth,
      height: videoRef.value.videoHeight
    };
    console.log('Native video dimensions:', dimensions.value);
    emit('loaded', dimensions.value);
  }
};


</script>

<style scoped>
.video-wrapper {
  position: relative;
  overflow: hidden;
}

.fixed-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.flipped-video {
  transform: scaleY(-1); /* Flips vertically */
  /* OR */
  transform: rotate(180deg); /* Rotates 180 degrees */
}
</style>
