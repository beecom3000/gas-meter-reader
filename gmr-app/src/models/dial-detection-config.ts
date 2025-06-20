export interface DialDetectionConfig {
  method: DetectionMethodType;

  adaptiveThreshold: boolean;
  morphology: boolean;

  cannyEdgeThreshold1: number;
  cannyEdgeThreshold2: number;

  blurKernelSize: number;
  thresholdValue: number;


  edgeDetectionThreshold: number;
  contourCircularThreshold: number;
}

export interface HoughCircleConfig {
  dp: number;
  minDist: number;
  param1: number;
  param2: number;
  minRadius: number;
  maxRadius: number;
}

export type DetectionMethodType = 'COMBINED' | 'HOUGH' | 'CONTOUR' | 'EDGE'
