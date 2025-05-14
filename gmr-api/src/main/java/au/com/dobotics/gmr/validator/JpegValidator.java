package au.com.dobotics.gmr.validator;

public class JpegValidator {
    public boolean isJpeg(byte[] data) {
        if (data.length < 2) {
            return false;
        }
        return (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8);
    }
}

