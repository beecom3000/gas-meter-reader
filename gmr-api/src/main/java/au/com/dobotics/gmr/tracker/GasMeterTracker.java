package au.com.dobotics.gmr.tracker;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.Vec4iVector;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

public class GasMeterTracker {
    public static void main(String[] args) {
        // Load OpenCV native library
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Initialize video capture (0 for webcam, or file path)
        VideoCapture cap = null;
        try {
            cap = new VideoCapture(0);
            if (!cap.isOpened()) {
                System.out.println("Error: Could not open video source.");
                return;
            }

            // Parameters
            int MOTION_THRESHOLD = 500;   // Adjust for sensitivity
            int MIN_LINE_LENGTH = 50;     // Min needle length (pixels)
            int MAX_LINE_GAP = 10;        // Max gap in line segments

            Mat prevFrame = new Mat();
            Mat frame = new Mat();
            Mat gray = new Mat();
            Mat blurred = new Mat();
            Mat edges = new Mat();
            Mat motionMask = new Mat();
            Mat frameDiff = new Mat();

            double prevAngle = -1;
            boolean trackingActive = false;

            while (true) {
                cap.read(frame);
                if (frame.empty()) break;

                // 1. Preprocess (grayscale + blur)
                opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
                opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

                // 2. Motion detection (compare with previous frame)
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
                            edges, lines, 1, Math.PI / 180, 50,
                            MIN_LINE_LENGTH, MAX_LINE_GAP
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
                        opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, trackingActive ? 255 : 0, 0, 1)
                );
                if (angle != -1) {
                    opencv_imgproc.putText(
                            frame, String.format("Angle: %.1f°", angle), new Point(10, 30),
                            opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0, 1)
                    );
                }

                opencv_highgui.imshow("Needle Tracking (Java)", frame);
                if (opencv_highgui.waitKey(1) == 'q') break;
            }
        } finally {
            if (cap != null) {
                cap.release();
                opencv_highgui.destroyAllWindows();
            }

        }
    }
}
