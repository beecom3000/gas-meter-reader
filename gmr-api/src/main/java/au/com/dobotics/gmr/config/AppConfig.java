package au.com.dobotics.gmr.config;

import au.com.dobotics.gmr.model.Config;
import au.com.dobotics.gmr.pipeline.*;
import au.com.dobotics.gmr.pipeline.config.DialDetectionConfig;
import au.com.dobotics.gmr.pipeline.config.HoughCircleConfig;
import au.com.dobotics.gmr.validator.JpegValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:config.yml", factory = YamlPropertySourceFactory.class)
public class AppConfig {

    @Bean
    public JpegValidator jpegValidator() {
        return new JpegValidator();
    }

    @Bean
    public Config config() {
        return new Config();
    }

    @Bean
    public HoughCircleConfig houghCircleConfig() {
        return HoughCircleConfig.builder()
                .dp(1.0)
                .minDist(100)
                .param1(100)
                .param2(25)
                .minRadius(150)
                .maxRadius(400)
                .build();

        /*
                        .dp(1.0)
                .minDist(100)
                .param1(50)
                .param2(30)
                .minRadius(100)
                .maxRadius(250)
                .build();
         */
    }

    @Bean
    public DialDetectionConfig dialDetectionConfig() {
        return DialDetectionConfig
                .builder()
                .method(DetectionMethodType.HOUGH)
                .blurKernelSize(5)
                .thresholdValue(150)
                .build();
    }

    @Bean
    public ConfigManager configManager(DialDetectionConfig dialDetectionConfig, HoughCircleConfig houghCircleConfig) {
        return new ConfigManager(dialDetectionConfig, houghCircleConfig);
    }

    @Bean
    public ImageProcessingPipeline imageProcessingPipeline(ConfigManager configManager) {
        DialDetectionStep dialDetectionStep = new DialDetectionStep(configManager);
        ImageProcessingPipeline pipeline = new ImageProcessingPipeline();
        pipeline.addStep(dialDetectionStep);
        return pipeline;
    }

}
