package au.com.dobotics.gmr.socketio.handler;

import au.com.dobotics.gmr.model.FrameData;
import au.com.dobotics.gmr.model.ProcessingStage;
import au.com.dobotics.gmr.processor.GrayscaleImageProcessorImpl;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_imgproc.Vec4iVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

@Slf4j
@Component
public class GasMeterFeedEventHandler {

    public static final String NAMESPACE = "/api/v1/gas";

    // Parameters
    int MOTION_THRESHOLD = 500;   // Adjust for sensitivity
    int MIN_LINE_LENGTH = 50;     // Min needle length (pixels)
    int MAX_LINE_GAP = 10;        // Max gap in line segments

    private final GrayscaleImageProcessorImpl grayscaleImageProcessorImpl;

    private final Map<String, ProcessingStage> clientStages;
    private final Map<String, Boolean> processingFlags;

    @Autowired
    public GasMeterFeedEventHandler(GrayscaleImageProcessorImpl grayscaleImageProcessorImpl) {
        this.grayscaleImageProcessorImpl = grayscaleImageProcessorImpl;
        this.clientStages = new ConcurrentHashMap<>();
        this.processingFlags = new ConcurrentHashMap<>();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        clientStages.put(sessionId, ProcessingStage.ORIGINAL);
        processingFlags.put(sessionId, false);
        log.info("Client connected to {}: {}", NAMESPACE, sessionId);
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        clientStages.remove(sessionId);
        processingFlags.remove(sessionId);
        log.info("Client disconnected from {}: {}", NAMESPACE, sessionId);
    }

    static class Result {
        public int width;
        public int height;
        public String status;
        public byte[] data;
    }

    @OnEvent("stage-change")
    public void onStageChange(SocketIOClient client, String stage, AckRequest ackRequest) {
        ProcessingStage processingStage = ProcessingStage.valueOf(stage.toUpperCase());
        String sessionId = client.getSessionId().toString();
        if (isValidStage(stage)) {
            clientStages.put(sessionId, processingStage);
        }
    }

    private boolean isValidStage(String stage) {
        return stage != null && (ProcessingStage.valueOf(stage.toUpperCase()) != null);
    }

    @OnEvent("processing-complete")
    public void onProcessingComplete(SocketIOClient client, Void data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        processingFlags.put(sessionId, false);
        client.sendEvent("processing-complete");
    }

    @OnEvent("processing-cancelled")
    public void onProcessingCancelled(SocketIOClient client, Void data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        processingFlags.put(sessionId, false);
    }

    @OnEvent("processing-start")
    public void onProcessingStart(SocketIOClient client, Void data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        processingFlags.put(sessionId, true);
    }

    @OnEvent("feed")
    public void onFeed(SocketIOClient client, FrameData data, AckRequest ackRequest) {
        if (data == null || data.getFrame() == null) {
            client.sendEvent("error", "Data is empty or null");
            return;
        }

        String sessionId = client.getSessionId().toString();
        ProcessingStage stage = clientStages.getOrDefault(sessionId, ProcessingStage.FINAL);
        // Check if processing was cancelled
        if (Boolean.FALSE.equals(processingFlags.get(sessionId))) {
            return;
        }

        byte[] frame = data.getFrame();
        int width = data.getWidth();
        int height = data.getHeight();

        byte[] processedFrame = process(stage, frame, width, height);

        // validate jpeg
//        boolean isJpeg = jpegValidator.isJpeg(processedFrame);
//        assert isJpeg : "processed frame is not jpeg";

        // Send with explicit binary attachment
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", width);
        metadata.put("height", height);
        metadata.put("timestamp", System.currentTimeMillis());

        // Send back the processed frame
        client.sendEvent("processed-frame", metadata, processedFrame);
    }

    private byte[] process(ProcessingStage stage, byte[] rawData, int width, int height) {
//
//        try {
//            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(rawData));
//            Frame frame = Java2DFrameUtils
//
//            // Convert Frame to OpenCV Mat
//            try (OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat()) {
//                return converter.convert(frame);
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        Mat mat = opencv_imgcodecs.imdecode(bytePointer.asBuffer(), opencv_imgcodecs.IMREAD_UNCHANGED);

//        Here's how to convert a JPEG byte array to an OpenCV Mat image using JavaCPP:

//        BytePointer data = new BytePointer(rawData);
//        try (Mat frame = new Mat(height, width, CvType.CV_8UC1, data)) {
//        try (Mat frame = new Mat(rawData)) {
        try (Mat frame = opencv_imgcodecs.imdecode(new Mat(rawData), IMREAD_UNCHANGED)) {

            if (frame.empty()) {
                log.error("Failed to decode image due to it is empty");
                return rawData; // Return original if processing fails
            }

            // Process the frame (e.g. blur, motion detection, etc.)
            try (Mat processedFrame = processFrame(stage, frame, width, height)) {
//                printImageInfo(processedFrame);
                // Convert back to byte array (JPEG)
                Result result = new Result();
//            result.data = toArray(processedFrame);
                result.height = processedFrame.rows();
                result.width = processedFrame.cols();
                result.status = "OK";
                return toArray(processedFrame);
            }
        }
    }

    // Helper method to verify dimensions
    public static void printImageInfo(Mat image) {
        System.out.println("Image dimensions: " + image.rows() + "x" + image.cols());
        System.out.println("Channels: " + image.channels());
        System.out.println("Type: " + image.type());
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

    private Mat processFrame(ProcessingStage stage, Mat frame, int width, int height) {

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
            gray = grayscaleImageProcessorImpl.process(frame);

            if (ProcessingStage.GRAYSCALE == stage) {
                return gray;
            }

            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

            if (ProcessingStage.BLUR == stage) {
                return blurred;
            }

            // 2. Motion detection (compared with the previous frame)
            if (!prevFrame.empty()) {
                opencv_core.absdiff(prevFrame, blurred, frameDiff);
                opencv_imgproc.threshold(frameDiff, frameDiff, 25, 255, opencv_imgproc.THRESH_BINARY);
                double motionArea = opencv_core.sumElems(motionMask).get(0) / 255;
//                    double motionArea = Core.sumElems(motionMask).val[0] / 255;
                trackingActive = motionArea > MOTION_THRESHOLD;
            }
            blurred.copyTo(prevFrame);

            // 3. Skip processing if no motion (reuse last angle)
            double angle = prevAngle;
            if (trackingActive) {
                // 4. Detect edges and lines (Hough Transform)
                opencv_imgproc.Canny(blurred, edges, 50, 150);
//                    Mat lines = new Mat();
                Vec4iVector lines = new Vec4iVector();
                opencv_imgproc.HoughLinesP(
                        edges,
                        lines,
                        1,
                        Math.PI / 180,
                        50,
                        MIN_LINE_LENGTH,
                        MAX_LINE_GAP
                );

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
