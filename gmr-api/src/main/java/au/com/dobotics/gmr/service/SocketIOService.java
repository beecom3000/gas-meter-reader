package au.com.dobotics.gmr.service;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
public class SocketIOService implements ApplicationListener<ContextRefreshedEvent> {

    private final SocketIOServer server;

    public SocketIOService(SocketIOServer server) {
        this.server = server;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        server.start();
    }

    @PreDestroy
    public void stop() {
        server.stop();
    }
}
