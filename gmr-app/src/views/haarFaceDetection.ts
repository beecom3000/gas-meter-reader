import { loadDataFile } from './cvDataFile';
import cv from '@techstark/opencv-js';

let faceCascade: cv.CascadeClassifier;

export const loadHaarFaceModels = async () => {
  console.log('Start loading HaarFace models');
  try {
    await loadDataFile('haarcascade_frontalface_default.xml', 'models/haarcascade_frontalface_default.xml');
    const response: string = await delay(2000)
    console.log(response);
  } catch(error) {
    console.error(error);
  }
}

/**
 * Detect faces from the input image.
 * See https://docs.opencv.org/master/d2/d99/tutorial_js_face_detection.html
 * @param {cv.Mat} img Input image
 * @returns the modified image with detected faces drawn on it.
 */
export const detectHaarFace = async (img: cv.Mat) => {
  const msize = new cv.Size(0, 0);

  // const newImg = img.clone();
  const newImg = img;

  const gray = new cv.Mat();
  cv.cvtColor(newImg, gray, cv.COLOR_RGBA2GRAY, 0);

  const faces = new cv.RectVector();

  // detect faces
  faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, msize, msize);
  for (let i = 0; i < faces.size(); ++i) {
    const point1 = new cv.Point(faces.get(i).x, faces.get(i).y);
    const point2 = new cv.Point(
      faces.get(i).x + faces.get(i).width,
      faces.get(i).y + faces.get(i).height
    );
    cv.rectangle(newImg, point1, point2, [255, 0, 0, 255]);
  }

  gray.delete();
  faces.delete();

  return newImg;
}


const delay = (ms: number) =>
  new Promise<string>((resolve) => {
    setTimeout(() => {
      faceCascade = new cv.CascadeClassifier();
      faceCascade.load('haarcascade_frontalface_default.xml');
      resolve('Successfully load HaarFace models');
    }, ms)
  })

