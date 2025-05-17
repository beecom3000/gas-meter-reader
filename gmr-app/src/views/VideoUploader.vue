<template>
  <div>
    <input type="file" accept="video/*" class="file-input" @change="handleFileUpload" />
    <button @click="startProcessing" :disabled="!videoFile">Process Video</button>
    <button @click="stopProcessing" :disabled="!isProcessing">Stop</button>
  </div>
  <div>Preview:</div>
  <video-player
    :src="videoSrc"
    ref="videoPlayerRef"
    :width="videoDimensions.width"
    :height="videoDimensions.height"
    object-fit="cover"
    @loaded="onVideoLoaded"
  >
  </video-player>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSocketIo } from '@/composables/use-socket-io.ts'
import { useVideo } from '@/composables/use-video.ts'
import VideoPlayer from '@/views/VideoPlayer.vue'
import type { VideoDimensions } from '@/models/video-dimensions.ts'

const videoFile = ref<File>()
const videoPlayerRef = ref<VideoPlayer | null>(null);
const videoSrc = ref<string>('');
const isProcessing = ref<boolean>(false)
const mediaStream = ref<MediaStream | null>(null)
const trackProcessor = ref<MediaStreamTrackProcessor | null>(null)
const reader = ref<ReadableStreamReader | null>(null)

const { socket, initSocket, terminateSocket } = useSocketIo()

// Handle file upload
const handleFileUpload = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) {
    console.warn('No file selected')
    return
  }
  const file: File = input.files[0];
  videoFile.value = file;
  // videoRef.value.src = URL.createObjectURL(file);
  videoPlayerRef.value.videoRef.src = URL.createObjectURL(file);
}

const videoDimensions = ref<VideoDimensions>({
  width: 800,
  height: 600
});

const onVideoLoaded = (dimensions: VideoDimensions) => {
  console.log('Video loaded:', dimensions);
  videoDimensions.value = {
    width: dimensions.width / 2,
    height: dimensions.height / 2
  };
}

const {
  checkVideoOrientation,
  checkVideoExifOrientation
} = useVideo();

// Start processing video frames
const startProcessing = async () => {
  if (!videoFile.value) {
    console.warn('No video file selected')
    return
  }

  // Check video metadata
  const orientationInfo = await checkVideoOrientation(videoFile.value);
  const exifOrientation = await checkVideoExifOrientation(videoFile.value)
  console.log(
    'Video orientation:',
    orientationInfo,
    'Exif orientation:',
    exifOrientation,
  )

  // // Determine if need flip
  // const needFlip = exifOrientation > 4

  initSocket();
  isProcessing.value = true;

  try {
    // Create a MediaStream from the video file
    const stream: MediaStream = (videoRef.value as HTMLMediaElement).captureStream();
    mediaStream.value = stream

    // Get video track
    const videoTrack: MediaStreamTrack = stream.getVideoTracks()[0]

    // Create a MediaStreamTrackProcessor to read frames
    // Note: The MediaStreamTrackProcessor interface of the Insertable Streams for MediaStreamTrack API
    // consumes a video MediaStreamTrack object's source and generates a stream of VideoFrame objects.
    if ('MediaStreamTrackProcessor' in window) {
      trackProcessor.value = new MediaStreamTrackProcessor({ track: videoTrack })
      const readableStream: ReadableStream = trackProcessor.value.readable
      readFramesAndSend(readableStream);
    } else {
      console.error('MediaStreamTrackProcessor not supported in this browser')
      fallbackFrameCapture()
    }
  } catch (error) {
    console.error('Error processing video:', error)
    isProcessing.value = false
  }
}

const extractFrameAsArrayBuffer = async (videoFrame: VideoFrame) => {
  // Convert frame to canvas to get image data
  const bitmap: ImageBitmap = await createImageBitmap(videoFrame)
  console.log(`Width: ${videoFrame.displayWidth}, Height: ${videoFrame.displayHeight}`);
  const canvas = new OffscreenCanvas(videoFrame.displayWidth, videoFrame.displayHeight)
  const ctx: OffscreenCanvasRenderingContext2D | null = canvas.getContext('2d');

  if (!ctx) {
    console.log('No context found');
    return;
  }

  // // Flip the canvas vertically
  // if (needFlip) {
  //   ctx.translate(0, canvas.height);
  //   ctx.scale(1, -1);
  // }

  ctx.drawImage(bitmap, 0, 0);

  // Get image data as blob
  const blob = await canvas.convertToBlob({ quality: 0.8, type: 'image/jpeg' })

  // Convert blob to array buffer
  return await blob.arrayBuffer();
}

// Read frames and send to server
const readFramesAndSend = async (needFlip: boolean, readableStream: ReadableStream<VideoFrame>) => {
  try {
    // The getReader() method of the ReadableStream interface creates a reader and locks
    // the stream to it. While the stream is locked, no other reader can be acquired until
    // this one is released.
    reader.value = readableStream.getReader();
    while (isProcessing.value) {
      // Result objects contain two properties:
      // done  - true if the stream has already given you all its data.
      // value - some data. Always undefined when done is true.
      const { done, value: videoFrame } = await reader.value.read()
      if (done) {
        console.log('Stream complete and done reading frames')
        break
      }

      const arrayBuffer = await extractFrameAsArrayBuffer(needFlip, videoFrame)

      // Send to server
      if (socket.value && socket.value.connected) {
        socket.value.emit('video-frame', {
          frameData: arrayBuffer,
          width: videoFrame.displayWidth,
          height: videoFrame.displayHeight,
          timestamp: performance.now(),
        })
      }

      videoFrame.close()
    }
  } catch (error) {
    console.error('Error reading frames:', error)
  } finally {
    stopProcessing()
  }
}

// Fallback for browsers without MediaStreamTrackProcessor
const fallbackFrameCapture = () => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = videoRef.value.videoWidth
  canvas.height = videoRef.value.videoHeight

  const captureInterval = setInterval(() => {
    if (!isProcessing.value) {
      clearInterval(captureInterval)
      return
    }

    ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)

    canvas.toBlob(
      async (blob) => {
        const arrayBuffer = await blob.arrayBuffer()

        if (socket.value && socket.value.connected) {
          socket.value.emit('video-frame', {
            frameData: arrayBuffer,
            width: canvas.width,
            height: canvas.height,
            timestamp: performance.now(),
          })
        }
      },
      'image/jpeg',
      0.8,
    )
  }, 1000 / 30) // 30 FPS
}

// Stop processing
const stopProcessing = () => {
  isProcessing.value = false
  if (reader.value) {
    reader.value.cancel()
    reader.value = null
  }
  if (trackProcessor.value) {
    trackProcessor.value.readable.cancel()
    trackProcessor.value = null
  }
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach((track) => track.stop())
    mediaStream.value = null
  }
  terminateSocket();
}
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
  height: auto;
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
</style>
