package au.com.dobotics.gmr.processor;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImageProcessor {


    public Mat applyGrayscale(Mat frame) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
        if (gray.empty()) {
            throw new RuntimeException("Failed to convert to grayscale");
        }
        return gray;
    }

    public void applyBlur() {

    }

}
