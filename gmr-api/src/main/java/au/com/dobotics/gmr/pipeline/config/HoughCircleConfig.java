package au.com.dobotics.gmr.pipeline.config;

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
public class HoughCircleConfig {
    private double dp ;     // Inverse ratio of accumulator resolution
    private double minDist;  // Fraction of image height
    private double minRadius;   // Fraction of image height
    private double maxRadius;   // Fraction of image height
    private double param1;      // upper threshold for the internal canny-edge detector
    private double param2;      // threshold for center detection
}
