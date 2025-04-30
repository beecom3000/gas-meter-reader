<template>
  <div class="about">
    <h2>Real-time Gas Meter Reading</h2>
    <div class="video-container">
      <video ref="webcamRef" class="webcam" autoplay playsinline muted></video>
      <canvas ref="canvasRef" style="display: none"></canvas>
    </div>
    <div>
      <button @click="startProcessing" :class="startButtonClass" :disabled="isProcessing">Start</button>
      <button @click="stopProcessing" :class="stopButtonClass" :disabled="!isProcessing">Stop</button>
    </div>
    <div class="video-container">
      <img :src="processedFrameUrl" alt="loading processed frame" class="webcam"/>
    </div>

  </div>
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { io, Socket } from 'socket.io-client'
import { useUserMedia } from '@vueuse/core'

const socket = ref<Socket | null>(null);
const processedFrameUrl = ref<string>('');
const webcamRef = ref<HTMLVideoElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const isProcessing = ref<boolean>(false);
const processingInterval = ref<number | null>(null);

const startButtonClass = ref<string>('button-enabled');
const stopButtonClass = ref<string>('button-disabled');

const { stream, start, stop } = useUserMedia({
  constraints: { video: true, audio: false }
});

onMounted(async () => {
  await start();
  if (stream.value && webcamRef.value) {
    webcamRef.value.srcObject = stream.value;
  }

  // Connect to Socket.IO Server
  socket.value = io('http://localhost:9092/', { transports: ['websocket'], reconnection: true, forceNew: true });
  socket.value.on('processed-frame', (data: string) => {
    // console.debug('Received processed frame: ', JSON.stringify(data));
    drawProcessedFrame(JSON.parse(data) as ResponseData);
  });
})

onBeforeUnmount(() => {
  stopProcessing();
  stop();
  if (socket.value) {
    socket.value.disconnect();
  }
});

const startProcessing = () => {
  if (isProcessing.value) return;

  isProcessing.value = true;
  const canvas = canvasRef.value!;
  const webcam = webcamRef.value!;

  // Set canvas dimensions to match video
  canvas.width = webcam.videoWidth;
  canvas.height = webcam.videoHeight;

  // Send frames at 15fps (adjust as needed)
  processingInterval.value = setInterval(() => {
    captureAndSendFrame();
  }, 66); // ~15fps

  stopButtonClass.value = 'button-enabled';
  startButtonClass.value = 'button-disabled';
};

const stopProcessing = () => {
  isProcessing.value = false;
  if (processingInterval.value) {
    clearInterval(processingInterval.value);
    processingInterval.value = null;
  }
  stopButtonClass.value = 'button-disabled';
  startButtonClass.value = 'button-enabled';
};

const captureAndSendFrame = () => {
  const canvas = canvasRef.value!;
  const video = webcamRef.value!;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    console.warn('Could not get canvas context');
    return;
  }

  // Draw current video frame to canvas
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  // Get image data as JPEG (compressed to reduce bandwidth)
  const imageData = canvas.toDataURL('image/jpeg', 0.7);

  // Send frame to server
  socket.value!.emit('frame', {
    frame: imageData,
    width: canvas.width,
    height: canvas.height,
  });
};

interface ResponseData {
  frame: string;
}

const drawProcessedFrame = (data: ResponseData) => {
  const canvas = canvasRef.value!;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    console.warn('Could not get canvas context');
    return;
  }

  // Create an image from the processed data
  const img = new Image();
  img.onload = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
  };
  img.src = `data:image/jpeg;base64,${data.frame}`;
  processedFrameUrl.value = img.src;
};

// const arrayBufferToBase64 = (buffer: ArrayBuffer) => {
//   let binary = '';
//   const bytes = new Uint8Array(buffer);
//   const len = bytes.byteLength;
//   for (let i = 0; i < len; i++) {
//     binary += String.fromCharCode(bytes[i]);
//   }
//   return btoa(binary);
// }
//
// const initSocket = () => {
//   socket.on('connect', () => {
//     socketState.value.connected = true;
//     console.info('Connected to socket server');
//   });
//   socket.on('disconnect', () => { socketState.value.connected = false});
//
//   socket.on('processed-frame', buffer => {
//     console.info('Received processed frame: ', buffer.byteLength || buffer.length);
//     // const base64String = btoa(String.fromCharCode(...new Uint8Array(buffer)));
//
//     // const base64String = arrayBufferToBase64(buffer);
//
//     // const url = 'data:image/jpg;base64,' + btoa(String.fromCharCode(...new Uint8Array(buffer)));;
//     // console.info('Blob created', url);
//     // processedFrameUrl.value = url;
//
//     if (buffer.length <= 0) {
//       return;
//     }
//
//     // red dot
//     // data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P48/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==
//     const uint8 = new Uint8Array(buffer);
//     const blob = new Blob([uint8], { type: 'image/jpeg' });
//     const url = URL.createObjectURL(blob);
//     const reader = new FileReader();
//     reader.onload = () => {
//       // console.log('Data URL Preview: ', reader.result);
//     };
//     reader.readAsDataURL(blob);
//     processedFrameUrl.value = url;
//     console.log('Setting processedFrameUrl to:', url);
//   });
// }
//
// const captureFrames = () => {
//   if (!webcam.value || !canvas.value) {
//     console.error('Webcam or canvas not found');
//     throw new Error('Webcam or canvas not found');
//   }
//   const ctx = canvas.value.getContext('2d');
//
//   if (!ctx) {
//     console.error('Could not get canvas context');
//     return;
//   }
//
//   setInterval(() => {
//     const width = webcam.value!.videoWidth ?? 0;
//     const height = webcam.value!.videoHeight ?? 0;
//     if (width === 0 || height === 0) return
//
//     canvas.value!.width = width
//     canvas.value!.height = height
//     ctx.drawImage(webcam.value!, 0, 0, width, height)
//
//     canvas.value!.toBlob(blob => {
//       if (blob) {
//         blob.arrayBuffer().then(buffer => {
//           socket.emit('frame', buffer)
//         })
//       }
//     }, 'image/jpeg')
//   }, 100) // 10fps
// }
//
// const startCamera = async () => {
//   const OneSecond: number = 1000;
//   try {
//     const stream: MediaStream = await navigator.mediaDevices
//       .getUserMedia({ video: true });
//     webcam.value!.srcObject = stream;
//     captureFrames();
//     // const mediaRecorder = new MediaRecorder(stream, {
//     //   mimeType: 'video/webm;codecs=vp9',
//     // });
//     // mediaRecorder.ondataavailable = (event: BlobEvent) => {
//     //   const blob: Blob = event.data;
//     //   if (blob.size > 0 && socket) {
//     //     socket.emit('frame', blob);
//     //   }
//     // };
//     // mediaRecorder.start(OneSecond);  // Capture data every 1 second
//   } catch(error) {
//     console.error('Error accessing camera: ', error);
//   }
// }

</script>

<style>
@media (min-width: 1024px) {
  .about {
    min-height: 100vh;
    display: flex;
    align-items: center;
  }
}

.video-container {
  position: relative;
  width: 640px;
  height: 480px;
}

video, canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

button {
  margin: 10px;
  padding: 8px 16px;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.button-enabled {
  background: #42b983;
}

.button-disabled {
  background: #ea0175;
}

button:hover .button-enabled {
  background: #288c5f;
}

button:hover .button-disabled {
  background: #930449;
}

.webcam {
  width: 450px;
  height: 345px;
}
</style>
