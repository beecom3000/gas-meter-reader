<template>
  <div class="frame-uploader">
    <h2>Video Frame Uploader</h2>

    <div v-if="!isProcessing">
      <input
        type="file"
        accept="video/*"
        @change="handleFileChange"
        class="file-input"
      >
      <button @click="startProcessing" :disabled="!videoFile">
        Process Video
      </button>
    </div>

    <div v-else>
      <div class="progress-container">
        <progress :value="progress" max="100"></progress>
        <span>{{ progress }}%</span>
        <span> | Frame {{ currentFrame }} of {{ totalFrames }}</span>
      </div>

      <button @click="cancelProcessing" v-if="isProcessing">
        Cancel
      </button>

      <canvas ref="canvas" style="display: none;"></canvas>
    </div>

    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div v-if="successMessage" class="success-message">
      {{ successMessage }}
    </div>

    <video
      v-if="videoUrl"
      ref="videoElement"
      controls
      class="video-preview"
    ></video>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeUnmount } from 'vue';
import axios from 'axios';

// Reactive state
const videoFile = ref(null);
const videoUrl = ref('');
const isProcessing = ref(false);
const progress = ref(0);
const currentFrame = ref(0);
const totalFrames = ref(0);
const errorMessage = ref('');
const successMessage = ref('');
const videoElement = ref(null);
const canvas = ref(null);
const processingInterval = ref(null);
const abortController = ref(null);

// Handle file selection
const handleFileChange = (event) => {
  const file = event.target.files[0];

  if (!file) return;

  if (!file.type.startsWith('video/')) {
    errorMessage.value = 'Please select a valid video file';
    return;
  }

  videoFile.value = file;
  videoUrl.value = URL.createObjectURL(file);
  errorMessage.value = '';
  successMessage.value = '';
};

// Start processing video frames
const startProcessing = async () => {
  if (!videoFile.value || !videoElement.value) return;

  try {
    isProcessing.value = true;
    progress.value = 0;
    currentFrame.value = 0;
    abortController.value = new AbortController();

    // Wait for video metadata to load
    await new Promise((resolve) => {
      videoElement.value.onloadedmetadata = resolve;
      videoElement.value.load();
    });

    const video = videoElement.value;
    const canvasEl = canvas.value;
    const ctx = canvasEl.getContext('2d');

    // Set canvas dimensions to match video
    canvasEl.width = video.videoWidth;
    canvasEl.height = video.videoHeight;

    // Calculate frame count based on duration and target FPS
    const targetFPS = 10; // Adjust as needed
    totalFrames.value = Math.floor(video.duration * targetFPS);

    // Process frames sequentially
    for (let i = 0; i < totalFrames.value; i++) {
      if (abortController.value.signal.aborted) break;

      // Seek to the frame time
      video.currentTime = i / targetFPS;

      // Wait for seek to complete and frame to update
      await new Promise((resolve) => {
        video.onseeked = () => {
          // Draw frame to canvas
          ctx.drawImage(video, 0, 0, canvasEl.width, canvasEl.height);

          // Convert canvas to byte array
          canvasEl.toBlob(async (blob) => {
            const arrayBuffer = await blob.arrayBuffer();
            const byteArray = new Uint8Array(arrayBuffer);

            // Send frame to server
            await sendFrameToServer(byteArray, {
              frameNumber: i,
              totalFrames: totalFrames.value,
              timestamp: video.currentTime,
              width: canvasEl.width,
              height: canvasEl.height
            });

            // Update progress
            currentFrame.value = i + 1;
            progress.value = Math.round((currentFrame.value / totalFrames.value) * 100);
            resolve();
          }, 'image/jpeg', 0.8); // Adjust quality as needed
        };
      });
    }

    if (!abortController.value.signal.aborted) {
      successMessage.value = `All ${totalFrames.value} frames processed successfully!`;
    }
  } catch (error) {
    if (error.name !== 'CanceledError') {
      errorMessage.value = 'Processing failed: ' + error.message;
      console.error('Error:', error);
    }
  } finally {
    isProcessing.value = false;
  }
};

// Send frame to server
const sendFrameToServer = async (frameData, metadata) => {
  try {
    const response = await axios.post('your-server-endpoint/frames', {
      frameData: Array.from(frameData), // Convert Uint8Array to regular array
      metadata: metadata
    }, {
      signal: abortController.value.signal
    });

    return response.data;
  } catch (error) {
    if (error.name !== 'CanceledError') {
      throw error;
    }
  }
};

// Cancel processing
const cancelProcessing = () => {
  if (abortController.value) {
    abortController.value.abort();
  }
  isProcessing.value = false;
  errorMessage.value = 'Processing canceled';
};

// Clean up
onBeforeUnmount(() => {
  cancelProcessing();
  if (videoUrl.value) {
    URL.revokeObjectURL(videoUrl.value);
  }
});
</script>

<style scoped>
.frame-uploader {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.file-input {
  display: block;
  margin-bottom: 15px;
}

.video-preview {
  max-width: 100%;
  margin-top: 20px;
  display: block;
}

.error-message {
  color: red;
  margin-top: 10px;
}

.success-message {
  color: green;
  margin-top: 10px;
}

progress {
  width: 100%;
  margin-right: 10px;
}

.progress-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}
</style>
