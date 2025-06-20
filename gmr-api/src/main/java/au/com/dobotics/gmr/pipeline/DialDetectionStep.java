package au.com.dobotics.gmr.pipeline;

import au.com.dobotics.gmr.model.Circle;
import au.com.dobotics.gmr.model.ProcessingStage;
import au.com.dobotics.gmr.pipeline.config.DialDetectionConfig;
import au.com.dobotics.gmr.pipeline.config.HoughCircleConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.Vec4fVector;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Getter
@Slf4j
public class DialDetectionStep extends BaseProcessingStep implements ImageProcessingStep {

    private final ConfigManager configManager;

    public DialDetectionStep(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public DialDetectionConfig config() {
        return this.configManager.get(DialDetectionConfig.class);
    }

    public HoughCircleConfig houghConfig() {
        return this.configManager.get(HoughCircleConfig.class);
    }

    @Override
    public Mat process(Mat inputImage, Context context) {
        try (Mat gray = new Mat();
             Mat blurred = new Mat();
             Mat binary = new Mat();
             Vec4fVector circles = new Vec4fVector()) {

            // Convert to grayscale
            cvtColor(inputImage, gray, opencv_imgproc.COLOR_BGR2GRAY);

            if (is(context, ProcessingStage.GRAYSCALE)){
                return gray.clone();
            }

            // Apply Gaussian blur
            GaussianBlur(gray, blurred,
                    new Size(config().getBlurKernelSize(), config().getBlurKernelSize()), 2);
            if (is(context, ProcessingStage.BLURRED)) {
                return blurred.clone();
            }

            // Apply a threshold
            threshold(blurred, binary, config().getThresholdValue(), 255,
                    THRESH_BINARY);

            if (is(context, ProcessingStage.THRESHOLD)) {
                return binary.clone();
            }

            // Calculate absolute values from factors
            double minDist = gray.rows() * houghConfig().getMinDist();
            int minRadius = (int) (gray.rows() * houghConfig().getMinRadius());
            int maxRadius = (int) (gray.rows() * houghConfig().getMaxRadius());

            // Detect circles
            HoughCircles(
                    binary,
                    circles,
                    HOUGH_GRADIENT,
                    houghConfig().getDp(),       // dp: Inverse ratio of accumulator resolution
                    minDist,                // minDist: minimum distance between detected centers
                    houghConfig().getParam1(),   // param1: upper threshold for the internal canny edge detector
                    houghConfig().getParam2(),   // param2: threshold for center detection
                    minRadius,              // min radius
                    maxRadius               // max radius
            );

            // Store detected circle
            if (!circles.empty()) {
                Scalar4f[] circleData = circles.get();
                if (circleData.length >= 3) {
                    float x = circleData[0].get();
                    float y = circleData[1].get();
                    float radius = circleData[2].get();
//                Point center = new Point(Math.round(x), Math.round(y));
                    context.put(Context.Key.CIRCLE, new Circle(x, y, radius));
                }
            } else {
                log.warn("No circles found");
            }

            // Visualization
            Mat output = inputImage.clone();
            if (circles.size() > 0) {
                Scalar4f[] circleData = circles.get();
                if (circleData.length >= 3) {
                    float x = circleData[0].get();
                    float y = circleData[1].get();
                    float radius = circleData[2].get();
                    circle(output, new Point(Math.round(x), Math.round(y)), Math.round(radius),
                            Scalar.GREEN);
                }
            }
            return output;
//        } catch (Exception ex) {
//            log.error("Error processing image", ex);
        }
//        return null;
    }
}
