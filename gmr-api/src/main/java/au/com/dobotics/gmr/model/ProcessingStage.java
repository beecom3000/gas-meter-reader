package au.com.dobotics.gmr.model;

public enum ProcessingStage {
    GRAYSCALE,
    BLURRED,
    CANNY_EDGE,
    THRESHOLD,
    FINAL;

    public boolean is(ProcessingStage stage) {
        return this == stage;
    }
}
