package au.com.dobotics.gmr.pipeline;

import org.bytedeco.opencv.opencv_core.Mat;

public interface ImageProcessingStep {

    Mat process(Mat inputImage, Context context);

}
