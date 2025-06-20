package au.com.dobotics.gmr.endpoint;

import au.com.dobotics.gmr.model.Config;
import au.com.dobotics.gmr.pipeline.ConfigManager;
import au.com.dobotics.gmr.pipeline.DetectionMethodType;
import au.com.dobotics.gmr.pipeline.config.DialDetectionConfig;
import au.com.dobotics.gmr.pipeline.config.HoughCircleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/gas/api/v1/config/dial")
public class DialDetectionConfigEndpoint {

    private final ConfigManager configManager;
    private final Config config;

    @Autowired
    public DialDetectionConfigEndpoint(ConfigManager configManager, Config config) {
        this.configManager = configManager;
        this.config = config;
    }

    @PutMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<HttpStatus> save(@RequestBody DialDetectionConfig config) {
        this.configManager.save(config);
        return ResponseEntity.ok(HttpStatus.OK);
    }
    @PutMapping(value = "/hough", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<HttpStatus> save(@RequestBody HoughCircleConfig config) {
        this.configManager.save(config);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping(value = "", produces = { MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> readConfig() {
        DialDetectionConfig cfg = DialDetectionConfig.builder().build();
        HoughCircleConfig houghCircleConfig = configManager.get(HoughCircleConfig.class);
        Map<String, Object> values = new HashMap<>();
        values.put("config", cfg);
        values.put("hough", houghCircleConfig);
        return ResponseEntity.ok(values);
    }
}
