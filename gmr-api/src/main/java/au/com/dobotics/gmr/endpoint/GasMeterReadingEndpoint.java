package au.com.dobotics.gmr.endpoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequestMapping("/gas/api/v1")
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

}
