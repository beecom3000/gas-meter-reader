package au.com.dobotics.gmr.config;

import au.com.dobotics.gmr.model.Config;
import au.com.dobotics.gmr.pipeline.CircleDetectionConfig;
import au.com.dobotics.gmr.pipeline.CircleDetectionStep;
import au.com.dobotics.gmr.pipeline.ImageProcessingPipeline;
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
    public ImageProcessingPipeline imageProcessingPipeline() {
        CircleDetectionConfig config = CircleDetectionConfig
                .builder()
                .dp(1.0)
                .minDistFactor(0.125)
                .cannyEdgeThreshold1(100)
                .cannyEdgeThreshold2(30)
                .minRadiusFactor(0.3)
                .maxRadiusFactor(0.5)
                .blurKernelSize(9)
                .thresholdValue(150)
                .build();
        CircleDetectionStep circleDetectionStep = new CircleDetectionStep(config);
        ImageProcessingPipeline pipeline = new ImageProcessingPipeline();
        pipeline.addStep(circleDetectionStep);
        return pipeline;
    }

}
