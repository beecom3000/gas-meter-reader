package au.com.dobotics.gmr.service;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.findContours;


@Component
public class ImageProcessor {

    private static final int CV_RETR_EXTERNAL = 0;
    private static final int CV_CHAIN_APPROX_NONE = 1;

    public void process(Mat frame) {
        // convert color into grey

        // Detect edges using Canny algorithm.

        // find digits
    }

    private void findCounterDigits() {
//        Mat edges = detectEdges();
//        Mat imgRet = edges.clone();
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        findContours(edges, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);
    }

    private Mat detectEdges(Mat greyImage, int threshold1, int threshold2) {
        Mat edges = new Mat();
        Canny(greyImage, edges, threshold1, threshold2);
        return edges;
    }
}
