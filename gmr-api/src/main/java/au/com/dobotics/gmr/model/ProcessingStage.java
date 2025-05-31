package au.com.dobotics.gmr.model;

public enum ProcessingStage {
    ORIGINAL,
    GRAYSCALE,
    BLUR,
    CANNY_EDGE,
    DIGIT_DETECTION,
    OCR,
    FINAL;
}
