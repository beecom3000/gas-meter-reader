package au.com.dobotics.gmr.model;

import lombok.Data;

@Data
public class FrameData {
    private byte[] frame;
    private int width;
    private int height;
    private long timestamp;
}
