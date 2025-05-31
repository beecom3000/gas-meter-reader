package au.com.dobotics.gmr.processor;

import org.bytedeco.opencv.opencv_core.Mat;

public interface ImageProcessor {
    Mat process(Mat frame);
}
