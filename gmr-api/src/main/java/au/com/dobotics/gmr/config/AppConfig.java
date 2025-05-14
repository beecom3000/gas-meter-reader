package au.com.dobotics.gmr.config;

import au.com.dobotics.gmr.model.Config;
import au.com.dobotics.gmr.validator.JpegValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PropertySourceFactory;

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

}
