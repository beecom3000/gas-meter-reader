import type { Metadata } from '@/models/metadata.ts'
import { ref } from 'vue'
import EXIF from 'exif-js'

export const useVideoMedia = () => {

  const currentFPS = ref<number>(0);
  const frameTimes = ref<number[]>([]);
  const previewCanvas = ref<HTMLCanvasElement | null>(null);

  // Calculate current FPS
  const calculateFPS = () => {
    if (frameTimes.value.length < 2) {
      currentFPS.value = 0
      return
    }

    const first = frameTimes.value[0]
    const last = frameTimes.value[frameTimes.value.length - 1]
    const averageInterval = (last - first) / (frameTimes.value.length - 1)
    currentFPS.value = 1000 / averageInterval
  }


  const renderImage = (metadata: Metadata, uint8Array: Uint8Array) => {
    // Create image from binary data
    const blob = new Blob([uint8Array], { type: 'image/jpeg' })
    const url = URL.createObjectURL(blob)

    const img: HTMLImageElement = new Image()

    // new Promise<void>((resolve, reject) => {
    img.onload = () => {

      if (previewCanvas.value) {

        const ctx: CanvasRenderingContext2D = previewCanvas.value.getContext('2d')!

        // Ensure canvas matches image dimensions
        // if (previewCanvas.value.width !== metadata.width || previewCanvas.value.height !== metadata.height) {
        //   previewCanvas.value.width = metadata.width / 2;
        //   previewCanvas.value.height = metadata.height / 2;
        // }

        // const aspectRatio = metadata.width / metadata.height;
        // let drawWidth = previewCanvas.value.width;
        // let drawHeight = previewCanvas.value.height / aspectRatio;
        //
        // if (drawHeight > previewCanvas.value.height) {
        //   drawHeight = previewCanvas.value.height;
        //   drawWidth = previewCanvas.value.width * aspectRatio;
        // }

        const drawWidth = previewCanvas.value.width
        const drawHeight = previewCanvas.value.height

        const x = (previewCanvas.value.width - metadata.width) / 2;
        const y = (previewCanvas.value.height - metadata.height) / 2;

        // const x = (canvas.value.width - )

        console.debug('Canvas width, height:', previewCanvas.value.width, previewCanvas.value.height)
        ctx.clearRect(0, 0, previewCanvas.value.width, previewCanvas.value.height)
        ctx.drawImage(img, 0, 0, drawWidth, drawHeight)
        URL.revokeObjectURL(url)
      }
      // resolve();
    }

    img.onerror = (error: Event | string) => {
      console.error('Image load failed: ', error)
      URL.revokeObjectURL(url)
      // reject(new Error('Image load failed: ' + error));
    }

    img.src = url
    return url
  }

  const handleResponse = async (metadata: Metadata, data: ArrayBuffer) => {
    // 1. Verify we received binary data
    if (!(data instanceof ArrayBuffer)) {
      console.error('Expected ArrayBuffer, got:', typeof data)
      return
    }

    const wrapped = data.slice(1);
    // const wrapped = data;
    const uint8Array = new Uint8Array(wrapped)
    // const uint8Array = new Uint8Array(data);

    // 2. Log received data details
    // console.log(`Client received - Size: ${uint8Array.length} bytes`)
    // console.log('Header:',
    //   Array.from(uint8Array.slice(0, 4))
    //     .map((b) => b.toString(16).padStart(2, '0'))
    //     .join(' '),
    // )

    // 3. Verify JPEG signature
    if (uint8Array.length < 2 || uint8Array[0] !== 0xff || uint8Array[1] !== 0xd8) {
      console.error(
        'Invalid JPEG signature:',
        Array.from(uint8Array.slice(0, 3)).map((b) => b.toString(16)),
      )
      return
    }

    // Calculate FPS
    calculateFPS()

    console.debug('Metadata: ', JSON.stringify(metadata))
    renderImage(metadata, uint8Array);
    // processedFrameUrl.value = url
  }

  // Fallback for browsers without MediaStreamTrackProcessor
  // const fallbackFrameCapture = () => {
  //   const canvas = document.createElement('canvas')
  //   const ctx = canvas.getContext('2d')
  //   canvas.width = videoPlayerRef.value.videoWidth
  //   canvas.height = videoPlayerRef.value.videoHeight
  //
  //   const captureInterval = setInterval(() => {
  //     if (!isProcessing.value) {
  //       clearInterval(captureInterval)
  //       return
  //     }
  //
  //     ctx.drawImage(videoPlayerRef.value, 0, 0, canvas.width, canvas.height)
  //
  //     canvas.toBlob(
  //       async (blob: Blob) => {
  //         const arrayBuffer = await blob.arrayBuffer()
  //
  //         if (socket.value && socket.value.connected) {
  //           socket.value.emit('video-frame', {
  //             frameData: arrayBuffer,
  //             width: canvas.width,
  //             height: canvas.height,
  //             timestamp: performance.now(),
  //           })
  //         }
  //       },
  //       'image/jpeg',
  //       0.8,
  //     )
  //   }, 1000 / 30) // 30 FPS
  // }

  const checkVideoOrientation = async (videoFile: File) => {
    // Create a URL for the video file
    const videoUrl = URL.createObjectURL(videoFile);

    return new Promise((resolve) => {
      const video = document.createElement('video');
      video.src = videoUrl;

      video.onloadedmetadata = () => {
        // Check for orientation through video dimensions
        const isPortrait = video.videoHeight > video.videoWidth;

        // Mobile browsers often handle orientation automatically
        // So the rendered dimensions might already be correct

        // Clean up
        URL.revokeObjectURL(videoUrl);

        resolve({
          isPortrait,
          videoWidth: video.videoWidth,
          videoHeight: video.videoHeight,
          displayWidth: video.width,
          displayHeight: video.height
        });
      };

      video.onerror = () => {
        URL.revokeObjectURL(videoUrl);
        resolve(null);
      };
    });
  };

  const checkVideoExifOrientation = (videoFile: File) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          EXIF.getData(videoFile, () => {
            const orientation = EXIF.getTag(this, 'Orientation');
            resolve(orientation || 1);  // Default to 1 (normal) if no orientation
          });
        } catch(error) {
          console.error('Error reading video EXIF data:', error);
          resolve(1);
        }
      }
      reader.readAsArrayBuffer(videoFile);
    });
  }

  return {
    previewCanvas,
    currentFPS,
    handleResponse,
    // fallbackFrameCapture,
    checkVideoOrientation,
    checkVideoExifOrientation
  }
}
