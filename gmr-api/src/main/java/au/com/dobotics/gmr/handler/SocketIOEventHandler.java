package au.com.dobotics.gmr.handler;

import au.com.dobotics.gmr.model.FrameData;
import au.com.dobotics.gmr.model.ResponseData;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.listener.DataListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class SocketIOEventHandler {

    public static final String JPEG_EXTENSION = ".jpg";
    public static final String FRAME_EVENT = "frame";
    public static final String PROCESSED_FRAME_EVENT = "processed-frame";

    private final SocketIOServer server;
    private final CascadeClassifier faceDetector;

    @Autowired
    public SocketIOEventHandler(SocketIOServer server, CascadeClassifier cascadeClassifier) {
        this.server = server;
        this.faceDetector = cascadeClassifier;
    }

    public DataListener<FrameData> onFrameReceived() {
        return (SocketIOClient client, FrameData data, AckRequest ackRequest) -> {
//            log.debug("Data: {}", data);

            if (data == null || data.getFrame() == null) {
                client.sendEvent("error", "Invalid frame data");
                return;
            }

            String frame = data.getFrame();
            int width = data.getWidth();
            int height = data.getHeight();

            // process frame
            byte[] processedFrame = processFrame(frame, width, height);

            // Send back the processed frame
            ObjectMapper mapper = new ObjectMapper();
            ResponseData responseData = new ResponseData();
            responseData.setFrame(Base64.getEncoder().encodeToString(processedFrame));
            client.sendEvent("processed-frame", mapper.writeValueAsString(responseData));


//            SocketIONamespace namespace = client.getNamespace();
//            String name = namespace.getName();
//            client.sendEvent(PROCESSED_FRAME_EVENT, mobArray);
//            ackRequest.sendAckData("processed");
        };
    }

    private byte[] processFrame(String frameData, int width, int height) {
        // Remove data URL prefix
        String base64Image = frameData.split(",")[1];
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // Convert to OpenCV Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        if (image.empty()) {
            System.err.println("Failed to decode image");
            return imageBytes; // Return original if processing fails
        }

        // Detect faces
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);

        // Draw rectangles around detected faces
        for (Rect rect : faceDetections.toArray()) {
            Imgproc.rectangle(
                    image,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0),
                    3
            );
        }

        // Convert back to byte array (JPEG)
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte);

//        Mat image = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_COLOR);
//        if (image.empty()) return;
//        Mat result = detectFaces(image);
//        MatOfByte mob = new MatOfByte();
//        boolean success = Imgcodecs.imencode(JPEG_EXTENSION, result, mob);
//        if (!success) {
//            log.warn("Failed to encode processed frame into JPEG");
//            return;
//        }
//        byte[] mobArray = mob.toArray();
//        log.debug("Sending processed frame to client: {}, bytes: {} ", client.getSessionId(), mobArray.length);

        return matOfByte.toArray();
    }

    @PostConstruct
    public void init() {
        log.info("Socket.io server is starting");
        this.server.addEventListener(FRAME_EVENT, FrameData.class, onFrameReceived());
        log.info("Socket.io server started");
    }

    private Mat detectFaces(Mat frame) {
        MatOfRect facesDetection = new MatOfRect();
        faceDetector.detectMultiScale(frame, facesDetection);
//        , 1.1, 3, 0, new Size(30, 30), new Size()
        for (Rect rect : facesDetection.toList()) {
            Imgproc.rectangle(frame, rect, new Scalar(0, 255, 0), 2);
        }
        return frame;
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Client connected: " + client.getSessionId());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        log.info("Client disconnected: " + client.getSessionId());
    }

    @OnEvent("frame")
    public void onFrameReceived(SocketIOClient client, byte[] data, AckRequest ackRequest) {
        try {
            // Process the received binary data (e.g., save to file)
            // Example:
            // FileUtils.writeByteArrayToFile(new File("received_file.dat"), data);
            log.debug("Received blob data from client: {}, size: {} bytes", client.getSessionId() , data.length);
            Mat image = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_COLOR);
            if (image.empty()) return;
            Mat result = detectFaces(image);
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", result, mob);
            client.sendEvent("processed-frame", mob.toArray());
            ackRequest.sendAckData("success");
        } catch (Exception e) {
            log.error("Error processing frame", e);
            ackRequest.sendAckData("failure");
        }
//        System.out.println("Received message: " + message + " from " + client.getSessionId());
//        this.server.getClient(client.getSessionId()).sendEvent("message", message);
    }

}
