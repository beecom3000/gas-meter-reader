<template>
  <div class="container">
    <div class="row">
      <input type="file" accept="video/*" class="file-input" @change="handleFileUpload" />
      <div>
      <button @click="startProcessing" :disabled="!videoFile">Process Video</button>
      <button @click="stopProcessing" :disabled="!isProcessing">Stop</button>
      </div>
    </div>
    <div class="row">
      <div class="col-6">
        <video-player
          :src="videoSrc"
          ref="videoPlayer"
          :width="videoDimensions.width"
          :height="videoDimensions.height"
          object-fit="cover"
          @loaded="onVideoLoaded"
        >
        </video-player>
      </div>
      <div class="col-6">
        <div>
          <canvas
            ref="previewCanvas"
            :width="canvasDimension.width"
            :height="canvasDimension.height"
            class="preview">
          </canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useSocketIo } from '@/composables/use-socket-io.ts'
import VideoPlayer from '@/views/VideoPlayer.vue'
import type { VideoDimensions } from '@/models/video-dimensions.ts'
import { useVideoMedia } from '@/composables/use-video-media.ts'

declare const MediaStreamTrackProcessor: any;

const videoFile = ref<File>()
const videoPlayer = ref<InstanceType<typeof VideoPlayer> | null>(null)
const videoSrc = ref<string>('')
const isProcessing = ref<boolean>(false)
const mediaStream = ref<MediaStream | null>(null)
const trackProcessor = ref<InstanceType<typeof MediaStreamTrackProcessor> | null>(null)
const reader = ref<ReadableStreamReader<VideoFrame> | null>(null)
const videoDimensions = ref<VideoDimensions>({
  width: 400,
  height: 300,
})

const { socket, initSocket, terminateSocket } = useSocketIo()
const { previewCanvas, handleResponse } = useVideoMedia()

// Handle file upload
const handleFileUpload = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) {
    console.warn('No file selected')
    return
  }
  const file: File = input.files[0]
  console.debug('File selected:', file);
  videoFile.value = file
  if (videoPlayer.value && videoPlayer.value.videoRef) {
    videoPlayer.value.videoRef.src = URL.createObjectURL(file)
  }
}

const onVideoLoaded = (dimensions: VideoDimensions) => {
  console.debug('Video loaded:', dimensions)
  videoDimensions.value = {
    width: dimensions.width / 2,
    height: dimensions.height / 2
  }
}

const canvasDimension = computed(() => ({
  width: videoDimensions.value.width,
  height: videoDimensions.value.height,
}));

// const {
//   checkVideoOrientation,
//   checkVideoExifOrientation
// } = useVideo();

// Start processing video frames
const startProcessing = async () => {
  if (!videoFile.value) {
    console.warn('No video file selected')
    return
  }

  // Check video metadata
  // const orientationInfo = await checkVideoOrientation(videoFile.value);
  // const exifOrientation = await checkVideoExifOrientation(videoFile.value)
  // console.log(
  //   'Video orientation:',
  //   orientationInfo,
  //   'Exif orientation:',
  //   exifOrientation,
  // )

  // // Determine if need flip
  // const needFlip = exifOrientation > 4

  await initSocket()

  socket.value!.on('processed-frame', handleResponse)
  isProcessing.value = true

  try {
    // Create a MediaStream from the video file
    const stream: MediaStream = (videoPlayer.value!.videoRef as HTMLCanvasElement).captureStream()
    mediaStream.value = stream

    // Get video track
    const videoTrack: MediaStreamTrack = stream.getVideoTracks()[0]

    // Create a MediaStreamTrackProcessor to read frames
    // Note: The MediaStreamTrackProcessor interface of the Insertable Streams for MediaStreamTrack API
    // consumes a video MediaStreamTrack object's source and generates a stream of VideoFrame objects.
    if ('MediaStreamTrackProcessor' in window) {
      trackProcessor.value = new MediaStreamTrackProcessor({ track: videoTrack })
      const readableStream: ReadableStream = trackProcessor.value.readable
      readFramesAndSend(readableStream)
    } else {
      console.error('MediaStreamTrackProcessor not supported in this browser')
      fallbackFrameCapture()
    }
  } catch (error) {
    console.error('Error processing video:', error)
    isProcessing.value = false
  }
}

const fallbackFrameCapture = () => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = videoPlayer.value!.videoWidth
  canvas.height = videoPlayer.value!.videoHeight

  const captureInterval = setInterval(() => {
    if (!isProcessing.value) {
      clearInterval(captureInterval)
      return
    }

    ctx.drawImage(videoPlayer.value, 0, 0, canvas.width, canvas.height)

    canvas.toBlob(
      async (blob: Blob) => {
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

const isJpeg = (buffer: Uint8Array) => {
  if (!buffer || buffer.length < 3) {
    return false
  }

  return buffer[0] === 255 && buffer[1] === 216 && buffer[2] === 255
  // return uint8Array[0] === 0xFF && uint8Array[1] === 0xD8 && uint8Array[2] === 0xFF;
}

const extractFrameAsArrayBuffer = async (videoFrame: VideoFrame): Promise<ArrayBuffer> => {
  // Convert frame to canvas to get image data
  console.debug('Video format: ', videoFrame.format)
  const bitmap: ImageBitmap = await createImageBitmap(videoFrame)
  // console.log(`Width: ${videoFrame.displayWidth}, Height: ${videoFrame.displayHeight}`)
  const canvas = new OffscreenCanvas(videoFrame.displayWidth, videoFrame.displayHeight)
  const ctx: OffscreenCanvasRenderingContext2D | null = canvas.getContext('2d')

  if (!ctx) {
    console.warn('No context found')
    return Promise.reject('No context found')
  }

  ctx.drawImage(bitmap, 0, 0,
    videoFrame.displayWidth, videoFrame.displayHeight )

  // Get image data as blob
  const blob = await canvas.convertToBlob({ quality: 1.0, type: 'image/jpeg' })
  const result = await blob.arrayBuffer()

  // const uint8Array = new Uint8Array(result)
  // const checkJpeg = isJpeg(uint8Array)
  // console.log(`${uint8Array[0]}, ${uint8Array[1]}, ${uint8Array[2]}`)

  // Convert blob to array buffer
  return result
}

// Read frames and send to server
const readFramesAndSend = async (readableStream: ReadableStream<VideoFrame>) => {
  try {
    // The getReader() method of the ReadableStream interface creates a reader and locks
    // the stream to it. While the stream is locked, no other reader can be acquired until
    // this one is released.
    reader.value = readableStream.getReader()
    while (isProcessing.value) {
      // Result objects contain two properties:
      // done  - true if the stream has already given you all its data.
      // value - some data. Always undefined when done is true.
      const { done, value: videoFrame } = await reader.value.read()
      if (done) {
        console.log('Stream complete and done reading frames')
        break
      }

      const arrayBuffer: ArrayBuffer = await extractFrameAsArrayBuffer(videoFrame)

      // Send to server
      if (socket.value && socket.value.connected) {
        socket.value.emit('feed', {
          frame: arrayBuffer,
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
  terminateSocket()
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

.preview {
  border: 1px solid black
}
</style>
