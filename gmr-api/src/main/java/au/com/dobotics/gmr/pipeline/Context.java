package au.com.dobotics.gmr.pipeline;

/**
 * Allows sharing data between steps in the pipeline.
 */
public class Context {

    public static class Key {
        public static final String FRAME_WIDTH = "frameWidth";
        public static final String FRAME_HEIGHT = "frameHeight";
        public static final String PROCESSING_STAGE = "processingStage";
        public static final String CIRCLE = "circle";
    }

    private final java.util.Map<String, Object> store = new java.util.HashMap<>();

    public void put(String key, Object value) {
        store.put(key, value);
    }

    public Object get(String key) {
        return store.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        return (T) store.get(key);
    }
}
