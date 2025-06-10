package au.com.dobotics.gmr.pipeline;

import au.com.dobotics.gmr.model.Circle;
import au.com.dobotics.gmr.model.ProcessingStage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.Vec4fVector;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Getter
@Slf4j
public class CircleDetectionStep implements ImageProcessingStep {

    private final CircleDetectionConfig config;

    public CircleDetectionStep(CircleDetectionConfig config) {
        this.config = config;
    }

    @Override
    public Mat process(Mat inputImage, Context context) {
        ProcessingStage stage = context.get(Context.Key.PROCESSING_STAGE, ProcessingStage.class);
        try (Mat gray = new Mat();
             Mat blurred = new Mat();
             Mat binary = new Mat();
             Vec4fVector circles = new Vec4fVector()) {

            // Convert to grayscale
            cvtColor(inputImage, gray, opencv_imgproc.COLOR_BGR2GRAY);

            if (stage.is(ProcessingStage.GRAYSCALE)){
                return gray.clone();
            }

            // Apply Gaussian blur
            GaussianBlur(gray, blurred,
                    new Size(config.blurKernelSize(), config.blurKernelSize()), 2);
            if (stage.is(ProcessingStage.BLUR)) {
                return blurred.clone();
            }

            // Apply threshold
            threshold(blurred, binary, config.thresholdValue(), 255,
                    THRESH_BINARY);

            if (stage.is(ProcessingStage.THRESHOLD)) {
                return binary.clone();
            }

            // Calculate absolute values from factors
            double minDist = gray.rows() * config.minDistFactor();
            int minRadius = (int) (gray.rows() * config.minRadiusFactor());
            int maxRadius = (int) (gray.rows() * config.maxRadiusFactor());

            // Detect circles
            HoughCircles(
                    binary,
                    circles,
                    HOUGH_GRADIENT,
                    config.dp(),
                    minDist,
                    config.cannyEdgeThreshold1(),
                    config.cannyEdgeThreshold2(),
                    minRadius,
                    maxRadius
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
        }
    }
}
