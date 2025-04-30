package au.com.dobotics.gmr.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenCvConfig {

    @PostConstruct
    public void init() {
        OpenCV.loadLocally();
    }

    @Bean
    public CascadeClassifier cascadeClassifier() {
        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        cascadeClassifier.load("src/main/resources/models/haarcascade_frontalface_default.xml");
        if (cascadeClassifier.empty()) {
            log.error("Failed to load cascade classifier");
            throw new RuntimeException("Failed to load cascade classifier");
        }
        return cascadeClassifier;
    }
}
