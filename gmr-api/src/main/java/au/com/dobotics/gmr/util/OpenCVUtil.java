package au.com.dobotics.gmr.util;

import org.bytedeco.opencv.opencv_core.Mat;

public final class OpenCVUtil {
    private OpenCVUtil() { }

    public static byte[] mat2ByteArray(Mat mat) {
        byte[] b = new byte[mat.channels() * mat.cols() * mat.rows()];
        mat.data().get(b);
        return b;
    }

}
