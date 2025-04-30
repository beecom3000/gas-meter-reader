import cv from '@techstark/opencv-js';

export const loadDataFile = async (cvFilePath: string, url: string) => {
  const response = await fetch(url);
  const buffer = await response.arrayBuffer();
  const data = new Uint8Array(buffer);
  cv.FS_createDataFile('/', cvFilePath, data, true, false, false);
}
