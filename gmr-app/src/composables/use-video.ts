import EXIF from 'exif-js'


export const useVideo = () => {

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
    checkVideoOrientation,
    checkVideoExifOrientation
  }
}
