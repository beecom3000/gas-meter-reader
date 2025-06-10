package au.com.dobotics.gmr.endpoint;

import au.com.dobotics.gmr.pipeline.CircleDetectionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequestMapping("/api/v1/gas")
public class GasMeterReadingEndpoint {

    @PostMapping(value = "/stream")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> livestream(@PathVariable("id") Long tipperId) {
        return null;
    }

    @PostMapping(value = "/ping")
    @ResponseBody
    public ResponseEntity<String> livestream(@RequestBody byte[] data) {
        return ResponseEntity.ok("pong");
    }

    @GetMapping(value = "/config")
    @ResponseBody
    public ResponseEntity<CircleDetectionConfig> config() {
        return null;
    }
}
