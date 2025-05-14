package au.com.dobotics.gmr.model;

import lombok.Data;

@Data
public class ResponseData implements java.io.Serializable {
    private byte[] frame;
    private long timestamp;
}
