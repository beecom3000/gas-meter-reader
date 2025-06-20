package au.com.dobotics.gmr.pipeline.config;

import lombok.Builder;

@Builder
public class NeedleDetectionConfig {
    private double cannyThreshold1 = 50;
    private double cannyThreshold2 = 150;
    private int houghThreshold = 50;
    private double minLineLengthFactor = 0.4;  // Fraction of circle radius
    private double maxLineGapFactor = 0.1;     // Fraction of circle radius
    private double centerProximityFactor = 0.1; // Fraction of circle radius
    private double minLineLengthThreshold = 0.7; // Fraction of circle radius
    private double maskShrinkFactor = 0.95;     // Fraction of circle radius
}
