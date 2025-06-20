package au.com.dobotics.gmr.model;

public enum ProcessingStage {
    ORIGINAL,
    GRAYSCALE,
    BLURRED,
    CANNY_EDGE,
    THRESHOLD,
    FINAL;

    public boolean is(ProcessingStage stage) {
        return this == stage;
    }
}
