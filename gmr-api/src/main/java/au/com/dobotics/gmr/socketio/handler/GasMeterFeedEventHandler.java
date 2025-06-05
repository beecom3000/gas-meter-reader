package au.com.dobotics.gmr.socketio.handler;

import au.com.dobotics.gmr.model.FrameData;
import au.com.dobotics.gmr.model.ProcessingStage;
import au.com.dobotics.gmr.pipeline.Context;
import au.com.dobotics.gmr.pipeline.ImageProcessingPipeline;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GasMeterFeedEventHandler {

    public static final String NAMESPACE = "/api/v1/gas";

    // Parameters
    int MOTION_THRESHOLD = 500;   // Adjust for sensitivity
    int MIN_LINE_LENGTH = 50;     // Min needle length (pixels)
    int MAX_LINE_GAP = 10;        // Max gap in line segments

    private final Map<String, ProcessingStage> clientStages;
    private final Map<String, Boolean> processingFlags;

    private final SocketIOServer server;
    private final ImageProcessingPipeline pipeline;

    private SocketIONamespace namespace;

    @Autowired
    public GasMeterFeedEventHandler(SocketIOServer server, ImageProcessingPipeline pipeline) {
        this.server = server;
        this.pipeline = pipeline;
        this.clientStages = new ConcurrentHashMap<>();
        this.processingFlags = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        log.info("Socket.io adding namespace: {}", NAMESPACE);
        this.namespace = server.addNamespace(NAMESPACE);
        log.info("Socket.io namespace added.");
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        clientStages.put(sessionId, ProcessingStage.ORIGINAL);
        processingFlags.put(sessionId, false);
        log.info("Client connected to {}: {}", NAMESPACE, sessionId);
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        clientStages.remove(sessionId);
        processingFlags.remove(sessionId);
        log.info("Client disconnected from {}: {}", NAMESPACE, sessionId);
    }

    @OnEvent("stage-change")
    public void onStageChange(SocketIOClient client, String stage, AckRequest ackRequest) {
        ProcessingStage newStage = ProcessingStage.valueOf(stage.toUpperCase());
        String sessionId = client.getSessionId().toString();
        ProcessingStage currentStage = clientStages.get(sessionId);
        if (isValidStage(stage) && !currentStage.is(newStage)) {
            log.debug("Client {} changed processing stage from {} to {}", sessionId, currentStage, newStage);
            clientStages.put(sessionId, newStage);
            client.sendEvent("stage-change-completed", stage);
        }
    }

    private boolean isValidStage(String stage) {
        return stage != null && (ProcessingStage.valueOf(stage.toUpperCase()) != null);
    }

    @OnEvent("processing-complete")
    public void onProcessingComplete(SocketIOClient client, Void data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        processingFlags.put(sessionId, false);
        client.sendEvent("processing-complete");
    }

    @OnEvent("processing-cancelled")
    public void onProcessingCancelled(SocketIOClient client, Void data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        processingFlags.put(sessionId, false);
    }

    @OnEvent("processing-start")
    public void onProcessingStart(SocketIOClient client, Void data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        processingFlags.put(sessionId, true);
    }

    @OnEvent("feed")
    public void onFeed(SocketIOClient client, FrameData data, AckRequest ackRequest) {
        if (data == null || data.getFrame() == null) {
            client.sendEvent("error", "Data is empty or null");
            return;
        }

        String sessionId = client.getSessionId().toString();
        ProcessingStage stage = clientStages.getOrDefault(sessionId, ProcessingStage.FINAL);
        // Check if processing was cancelled
        if (Boolean.FALSE.equals(processingFlags.get(sessionId))) {
            return;
        }

        byte[] frame = data.getFrame();
        int width = data.getWidth();
        int height = data.getHeight();

        this.pipeline.addContext(Context.Key.PROCESSING_STAGE, stage);
        this.pipeline.addContext(Context.Key.FRAME_WIDTH, width);
        this.pipeline.addContext(Context.Key.FRAME_HEIGHT, height);

        byte[] processedFrame = this.pipeline.execute(frame);

        // validate jpeg
//        boolean isJpeg = jpegValidator.isJpeg(processedFrame);
//        assert isJpeg : "processed frame is not jpeg";

        // Send with explicit binary attachment
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", width);
        metadata.put("height", height);
        metadata.put("timestamp", System.currentTimeMillis());

        // Send back the processed frame
        client.sendEvent("processed-frame", metadata, processedFrame);
    }


}
