<template>
  <div>
    <input type="file" accept="video/*" class="file-input" @change="handleFileUpload" />
    <button @click="startProcessing" :disabled="!videoFile">Process Video</button>
    <button @click="stopProcessing" :disabled="!isProcessing">Stop</button>
    Preview:
    <video ref="videoRef" controls style="max-width: 100%"></video>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSocketIo } from '@/composables/use-socket-io.ts'
import { useVideo } from '@/composables/use-video.ts'

const videoFile = ref<File>()
const videoRef = ref<HTMLMediaElement | null>(null);
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
  (videoRef.value as HTMLMediaElement).src = URL.createObjectURL(file);
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

  // Determine if need flip
  const needFlip = exifOrientation > 4

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
      readFramesAndSend(needFlip, readableStream);
    } else {
      console.error('MediaStreamTrackProcessor not supported in this browser')
      fallbackFrameCapture()
    }
  } catch (error) {
    console.error('Error processing video:', error)
    isProcessing.value = false
  }
}

const extractFrameAsArrayBuffer = async (needFlip: boolean, videoFrame: VideoFrame) => {
  // Convert frame to canvas to get image data
  const bitmap: ImageBitmap = await createImageBitmap(videoFrame)
  const canvas = new OffscreenCanvas(videoFrame.displayWidth, videoFrame.displayHeight)
  const ctx = canvas.getContext('2d');

  // Flip the canvas vertically
  if (needFlip) {
    ctx.translate(0, canvas.height);
    ctx.scale(1, -1);
  }

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
