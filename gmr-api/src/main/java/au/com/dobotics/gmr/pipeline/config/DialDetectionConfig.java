package au.com.dobotics.gmr.pipeline.config;

import au.com.dobotics.gmr.pipeline.DetectionMethodType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DialDetectionConfig {

    private DetectionMethodType method;

    private boolean adaptiveThreshold;
    private boolean morphology;

    private double cannyEdgeThreshold1;           // Canny edge threshold
    private double cannyEdgeThreshold2;            // Accumulator threshold

    private int blurKernelSize;             // Gaussian blur kernel size
    private int thresholdValue;          // Binary threshold value

    private double edgeDetectionThreshold;
    private double contourCircularThreshold;

}
