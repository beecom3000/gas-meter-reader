export interface DialDetectionConfig {
  method: DetectionMethodType;
  param2: number;
  minRadiusFactor: number;
  maxRadiusFactor: number;
  edgeDetectionThreshold: number;
  contourCircularThreshold: number;
}

export type DetectionMethodType = 'COMBINED' | 'HOUGH' | 'CONTOUR' | 'EDGE'
