package au.com.dobotics.gmr.pipeline;

import au.com.dobotics.gmr.model.ProcessingStage;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.Vec4iVector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

@Slf4j
public class ImageProcessingPipeline {

    private final List<ImageProcessingStep> steps = new ArrayList<>();
    private final Context context = new Context();

    public void addStep(ImageProcessingStep step) {
        steps.add(step);
    }

    public void addContext(String key, Object value) {
        context.put(key, value);
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public byte[] execute(byte[] rawData) {
        try (Mat frame = opencv_imgcodecs.imdecode(new Mat(rawData), IMREAD_UNCHANGED)) {

            if (frame.empty()) {
                log.error("Failed to decode image due to it is empty");
                return rawData; // Return original if processing fails
            }

            ProcessingStage stage = this.context.get(Context.Key.PROCESSING_STAGE, ProcessingStage.class);
            if (stage.is(ProcessingStage.ORIGINAL)) {
                return toArray(frame);
            }

            try (Mat current = frame.clone()) {
                for (ImageProcessingStep step : steps) {
                    try (Mat result = step.process(current, context)) {
                        result.copyTo(current);
                    }
                }
                return toArray(current);
            }


//                printImageInfo(processedFrame);
                // Convert back to byte array (JPEG)
//                Result result = new Result();
//            result.data = toArray(processedFrame);
//                result.height = processedFrame.rows();
//                result.width = processedFrame.cols();
//                result.status = "OK";

        }
    }

    private byte[] toArray(Mat mat) {
        ByteBuffer out = encodeImage(mat);
        byte[] arr = new byte[out.remaining()];
        out.get(arr);
        return arr;
    }

    private ByteBuffer encodeImage(Mat mat) {
        BytePointer outPointer = new BytePointer();
        IntPointer params = new IntPointer(IMWRITE_JPEG_QUALITY , 80 );
        boolean isSuccess = imencode(".jpg", mat, outPointer, params);
        if (!isSuccess) {
            log.warn("Failed to encode image to JPEG");
            throw new RuntimeException("Failed to encode image to JPEG");
        }
        return outPointer.asByteBuffer();
    }

    // Helper method to verify dimensions
    public static void printImageInfo(Mat image) {
        System.out.println("Image dimensions: " + image.rows() + "x" + image.cols());
        System.out.println("Channels: " + image.channels());
        System.out.println("Type: " + image.type());
    }

    private Mat processFrame(Mat frame, Context context) {

        ProcessingStage stage = (ProcessingStage) context.get(Context.Key.PROCESSING_STAGE);
        boolean sendFinal = stage == ProcessingStage.FINAL;

        if (ProcessingStage.ORIGINAL == stage) {
            return frame;
        }

        Mat blurred = new Mat();
        Mat gray = null;
        // Convert byte array to OpenCV Mat
        try (Mat prevFrame = new Mat();
             Mat edges = new Mat();
             Mat motionMask = new Mat();
             Mat frameDiff = new Mat()) {

            double prevAngle = -1;      // Tracking the angle from the previous frame
            boolean trackingActive = true;

            // 1. Preprocess (grayscale + blur)
            gray = null; //grayscaleImageProcessorImpl.process(frame);

            if (ProcessingStage.GRAYSCALE == stage) {
                return gray;
            }

            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

            if (ProcessingStage.BLURRED == stage) {
                return blurred;
            }

            // 2. Motion detection (compared with the previous frame)
            if (!prevFrame.empty()) {
                opencv_core.absdiff(prevFrame, blurred, frameDiff);
                opencv_imgproc.threshold(frameDiff, frameDiff, 25, 255, opencv_imgproc.THRESH_BINARY);
                double motionArea = opencv_core.sumElems(motionMask).get(0) / 255;
//                    double motionArea = Core.sumElems(motionMask).val[0] / 255;
//                trackingActive = motionArea > MOTION_THRESHOLD;
            }
            blurred.copyTo(prevFrame);

            // 3. Skip processing if no motion (reuse last angle)
            double angle = prevAngle;
            if (trackingActive) {
                // 4. Detect edges and lines (Hough Transform)
                opencv_imgproc.Canny(blurred, edges, 50, 150);
//                    Mat lines = new Mat();
                Vec4iVector lines = new Vec4iVector();
//                opencv_imgproc.HoughLinesP(
//                        edges,
//                        lines,
//                        1,
//                        Math.PI / 180,
//                        50,
//                        MIN_LINE_LENGTH,
//                        MAX_LINE_GAP
//                );

                // 5. Find the longest line near the center (needle)
                Point center = new Point(frame.cols() / 2, frame.rows() / 2);
                double maxLength = 0;
                Point[] needleLine = null;

                // Probabilistic Line Transform
//                    vector<Vec4i> linesP; // will hold the results of the detection
//                    HoughLinesP(dst, linesP, 1, CV_PI/180, 50, 50, 10 ); // runs the actual detection
//                    // Draw the lines
//                    for( size_t i = 0; i < linesP.size(); i++ )
//                    {
//                        Vec4i l = linesP[i];
//                        line( cdstP, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(0,0,255), 3, LINE_AA);
//                    }

                long totalVals = lines.size(); // Expect to see 4 here
                Scalar4i[] scalarArray = lines.get();
                Point pt1 = new Point(scalarArray[0].get(), scalarArray[1].get());
                Point pt2 = new Point(scalarArray[2].get(), scalarArray[3].get());

//                    for (int i = 0; i < lines.size(); i++) {
//                        Scalar4i line = lines.get(i);
//                        double[] line = lines.get(i);
//                        Point pt1 = new Point(line[0], line[1]);
//                        Point pt2 = new Point(line[2], line[3]);
                double length = Math.sqrt(Math.pow(pt2.x() - pt1.x(), 2) + Math.pow(pt2.y() - pt1.y(), 2));

                // Check if line passes near the center
                double dist1 = Math.sqrt(Math.pow(pt1.x() - center.x(), 2) + Math.pow(pt1.y() - center.y(), 2));
                double dist2 = Math.sqrt(Math.pow(pt2.x() - center.x(), 2) + Math.pow(pt2.y() - center.y(), 2));

                if ((dist1 < 50 || dist2 < 50) && length > maxLength) {
                    maxLength = length;
                    needleLine = new Point[]{pt1, pt2};
                }
//                    }

                // 6. Compute angle if needle detected
                if (needleLine != null) {
                    double dx = needleLine[1].x() - needleLine[0].x();
                    double dy = needleLine[1].y() - needleLine[0].y();
                    angle = Math.toDegrees(Math.atan2(dy, dx)) % 360;
                    prevAngle = angle;

                    // Draw needle (green) and center (red)
                    opencv_imgproc.line(frame, needleLine[0], needleLine[1], new Scalar(0, 255, 0, 1));
                }
            }

            // 7. Display results
            opencv_imgproc.circle(frame, new Point(frame.cols() / 2, frame.rows() / 2), 5, new Scalar(0, 0, 255, 1));
            String status = trackingActive ? "TRACKING" : "IDLE (No motion)";
            opencv_imgproc.putText(
                    frame, "Status: " + status, new Point(10, 60),
                    opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, trackingActive ? 255 : 0, 0, 1)
            );
            if (angle != -1) {
                opencv_imgproc.putText(
                        frame,
                        String.format("Angle: %.1f°", angle),
                        new Point(10, 30),
                        opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        new Scalar(0, 255, 0, 1)
                );
            }

            return frame;
        }
    }
}
