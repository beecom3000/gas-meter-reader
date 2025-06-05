package au.com.dobotics.gmr.pipeline;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Builder
@Accessors(fluent = true) @Getter @Setter
public class CircleDetectionConfig {
    private double dp = 1.0;
    private double minDistFactor = 0.125;  // Fraction of image height
    private double cannyEdgeThreshold1 = 100;           // Canny edge threshold
    private double cannyEdgeThreshold2 = 30;            // Accumulator threshold
    private double minRadiusFactor = 0.3;  // Fraction of image height
    private double maxRadiusFactor = 0.5;  // Fraction of image height
    private int blurKernelSize = 9;        // Gaussian blur kernel size
    private int thresholdValue = 150;      // Binary threshold value
}
