package au.com.dobotics.gmr.socketio.handler;

import au.com.dobotics.gmr.model.FrameData;
import au.com.dobotics.gmr.validator.JpegValidator;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

@Slf4j
@Component
public class FaceDetectionEventHandler {

    public static final String NAMESPACE = "/api/v1/face";

    public static final String JPEG_EXTENSION = ".jpg";
    public static final String FRAME_EVENT = "frame";
    public static final String PROCESSED_FRAME_EVENT = "processed-frame";

    private final SocketIOServer server;
    private SocketIONamespace namespace;
    private final CascadeClassifier faceDetector;
    private final JpegValidator jpegValidator;

    @Autowired
    public FaceDetectionEventHandler(SocketIOServer server, CascadeClassifier cascadeClassifier, JpegValidator jpegValidator) {
        this.server = server;
        this.faceDetector = cascadeClassifier;
        this.jpegValidator = jpegValidator;
    }

    @PostConstruct
    public void init() {
        log.info("Socket.io adding namespace: {}", NAMESPACE);
        this.namespace = server.addNamespace(NAMESPACE);
//        this.namespace.addConnectListener(client -> log.info("Client connected: {}", client.getSessionId()));
//        this.namespace.addDisconnectListener(client -> log.info("Client disconnected: {}", client.getSessionId()));
//        this.namespace.addEventListener(FRAME_EVENT, FrameData.class, onFrameReceived());
//        this.namespace.addEventListener("test-load", String.class, onTestLoaded());
//        this.namespace.addEventListener(FRAME_EVENT, FrameData.class, onFrameReceived());
        log.info("Socket.io server started");
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Client connected to {}: {}", NAMESPACE, client.getSessionId());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        log.info("Client disconnected from {}: {}", NAMESPACE, client.getSessionId());
    }

    // Helper method to print byte headers
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, length); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    public DataListener<String> onTestLoaded() {
        return (SocketIOClient client, String data, AckRequest ackRequest) -> {
            log.info("Client sent test-load event");
            try (InputStream is = getClass().getResourceAsStream("/moutain-800-450.jpg")) {
                assert is != null : "Failed to find test image";
                byte[] imageBytes = is.readAllBytes();
                boolean isJpeg  = jpegValidator.isJpeg(imageBytes);
                log.debug("Test image loaded, size: {} byte(s), JPEG: {}", imageBytes.length, isJpeg);
                Map<String, Object> payload = new HashMap<>();
                payload.put("width", 800);
                payload.put("height", 450);
                payload.put("timestamp", System.currentTimeMillis());
//                client.sendEvent("test-frame", payload, imageBytes);
                Packet packet = new Packet(PacketType.MESSAGE, client.getEngineIOVersion());
                packet.setSubType(PacketType.EVENT);
                packet.setName("test-frame");
                packet.setData(Arrays.asList(payload, imageBytes));
                client.send(packet);
            }
        };
    };

    @OnEvent(FRAME_EVENT)
    public void onFrameReceived(SocketIOClient client, FrameData data, AckRequest ackRequest) {
        detectFaces(client, data);
    }

    public DataListener<FrameData> onFrameReceived() {
        return (SocketIOClient client, FrameData data, AckRequest ackRequest) -> {
//            log.debug("Data: {}", data);
            detectFaces(client, data);
        };
    }

    private void detectFaces(SocketIOClient client, FrameData data) {
        if (data == null || data.getFrame() == null) {
            client.sendEvent("error", "Invalid frame data");
            return;
        }

        byte[] frame = data.getFrame();
        int width = data.getWidth();
        int height = data.getHeight();

        // process frame
        byte[] processedFrame = processFrame(frame, width, height);

        boolean isJpeg = jpegValidator.isJpeg(processedFrame);
        assert isJpeg : "processed frame is not jpeg";

        // Verify before sending
//        log.debug("Server sending JPEG - Size: {} byte(s)", processedFrame.length);
//        log.debug("Header: " + bytesToHex(processedFrame, 4));

        // Send with explicit binary attachment
        Map<String, Object> payload = new HashMap<>();
        payload.put("width", width);
        payload.put("height", height);
        payload.put("timestamp", System.currentTimeMillis());
//            payload.put("message", "Hello World");

        // Send back the processed frame
        int numOfBytes = processedFrame.length;
        client.sendEvent("processed-frame", payload, processedFrame);
    }

    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(obj);
        return byteStream.toByteArray();
    }

    private byte[] processFrame(byte[] frameData, int width, int height) {
        // Convert to OpenCV Mat
        Mat image = imdecode(new Mat(frameData), IMREAD_COLOR);
//        org.opencv.core.Mat image = Imgcodecs.imdecode(new MatOfByte(frameData), Imgcodecs.IMREAD_COLOR);

        if (image.empty()) {
            log.error("Failed to decode image due to it's empty");
            return frameData; // Return original if processing fails
        }

        // Detect faces
        RectVector faces = new RectVector();
//        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(image, faces);

        // Draw rectangles around detected faces
        for (Rect rect : faces.get()) {
            rectangle(
                    image,
                    rect,
                    Scalar.GREEN
            );
        }

        // Convert back to byte array (JPEG)
        BytePointer bytePointer = new BytePointer();
        IntPointer intPointer = new IntPointer(IMWRITE_JPEG_QUALITY , 80 );
        boolean isSuccess = imencode(".jpg", image, bytePointer, intPointer);
        if (!isSuccess) {
            log.warn("Failed to encode image to JPEG");
        }

        ByteBuffer encodedImage = bytePointer.asByteBuffer();
//        ByteBuffer encodedImage = image.asByteBuffer();
//        IntBuffer intBuffer = IntBuffer.wrap(new int[]{ IMWRITE_JPEG_QUALITY , 80 }); // 80% quality
//        boolean isSuccess = imencode(".jpg", image, bytePointer, intBuffer);
//        MatOfByte encodedImage = new MatOfByte();
//        MatOfInt params = new MatOfInt(IMWRITE_JPEG_QUALITY, 80);
//        Imgcodecs.imencode(".jpg", image, encodedImage, params);

//        return encodedImage.array();
        byte[] arr = new byte[encodedImage.remaining()];
        encodedImage.get(arr);
        return arr;
    }

//    // In Java backend:
//    byte[] testImage = Files.readAllBytes(Paths.get("known-good.jpg"));
//client.sendEvent("test-frame", testImage);

//    @OnConnect
//    public void onConnect(SocketIOClient client) {
//        log.info("Client connected: " + client.getSessionId());
//    }
//
//    @OnDisconnect
//    public void onDisconnect(SocketIOClient client) {
//        log.info("Client disconnected: " + client.getSessionId());
//    }
//
//    @OnEvent("frame")
//    public void onFrameReceived(SocketIOClient client, byte[] data, AckRequest ackRequest) {
//        try {
//            // Process the received binary data (e.g., save to file)
//            // Example:
//            // FileUtils.writeByteArrayToFile(new File("received_file.dat"), data);
//            log.debug("Received blob data from client: {}, size: {} bytes", client.getSessionId() , data.length);
//            Mat image = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_COLOR);
//            if (image.empty()) return;
//            Mat result = detectFaces(image);
//            MatOfByte mob = new MatOfByte();
//            Imgcodecs.imencode(".jpg", result, mob);
//            client.sendEvent("processed-frame", mob.toArray());
//            ackRequest.sendAckData("success");
//        } catch (Exception e) {
//            log.error("Error processing frame", e);
//            ackRequest.sendAckData("failure");
//        }
//    }

}
