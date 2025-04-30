package au.com.dobotics.gmr.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

@Slf4j
@Configuration
public class OpenCvConfig {

    @PostConstruct
    public void init() {
        OpenCV.loadLocally();
    }

    @Bean
    public CascadeClassifier cascadeClassifier() {
        URL resource = getClass().getResource("/models/haarcascade_frontalface_default.xml");
        assert resource != null : "Failed to find cascade classifier file";
        String filename = resource.getFile();
        log.info("Loading cascade classifier file from {}", filename);
        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        cascadeClassifier.load(filename);
        if (cascadeClassifier.empty()) {
            log.error("Failed to load cascade classifier");
            throw new RuntimeException("Failed to load cascade classifier");
        }
        return cascadeClassifier;
    }
}
