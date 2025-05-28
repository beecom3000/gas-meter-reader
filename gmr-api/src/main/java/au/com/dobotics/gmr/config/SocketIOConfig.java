package au.com.dobotics.gmr.config;

import au.com.dobotics.gmr.socketio.ExtendedSpringAnnotationScanner;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;

@Configuration
@Slf4j
@CrossOrigin
public class SocketIOConfig {

    @Value("${socketio.port}")
    private int port;

    @Value("${socketio.hostname}")
    private String hostname;

    @Value("${socketio.max.frame.payload.length:65335}")
    private int maxFramePayloadLength;

    @Value("${socketio.max.http.content.length:65335}")
    private int maxHttpContentLength;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(this.hostname);
        config.setPort(this.port);
        config.setOrigin("*");

//        // Enable multiple namespace support
//        config.setAuthorizationListener(data -> true); // Add your auth logic here

        // Enable binary support
//        config.setUseLinuxNativeEpoll(true); // For Linux performance
        config.setMaxFramePayloadLength(maxFramePayloadLength); // 100MB
        config.setMaxHttpContentLength(maxHttpContentLength); // 100MB
        SocketIOServer server = new SocketIOServer(config);

//        server.addConnectListener(client -> log.info("Client connected: {}", client.getSessionId()));
//        server.addDisconnectListener(client -> log.info("Client disconnected: {}", client.getSessionId()));
        return server;
    }


//    @Bean
    public SocketIONamespace namespace(SocketIOServer server) {
        SocketIONamespace namespace = server.addNamespace("/api/v1/gasfeed");
        namespace.addConnectListener(client -> {
            log.info("Client connected: {}", client.getSessionId());
        });
        return namespace;
    }

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new ExtendedSpringAnnotationScanner(socketServer);
    }

}
