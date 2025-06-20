package au.com.dobotics.gmr.pipeline;

import au.com.dobotics.gmr.pipeline.config.DialDetectionConfig;
import au.com.dobotics.gmr.pipeline.config.HoughCircleConfig;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ConfigManager {

    private final Map<String, Object> configs;

    public ConfigManager(DialDetectionConfig dialDetectionConfig, HoughCircleConfig houghCircleConfig) {
        this.configs = new ConcurrentHashMap<>();
        this.configs.put(DialDetectionConfig.class.getSimpleName(), dialDetectionConfig);
        this.configs.put(HoughCircleConfig.class.getSimpleName(), houghCircleConfig);
    }

    public void save(Object config) {
        this.configs.put(config.getClass().getSimpleName(), config);
    }

    public <T> T get(Class<T> key) {
        return (T) configs.get(key.getSimpleName());
    }

}
