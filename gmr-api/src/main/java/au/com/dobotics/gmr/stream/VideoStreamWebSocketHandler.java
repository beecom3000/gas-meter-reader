package au.com.dobotics.gmr.stream;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

@Component
public class VideoStreamWebSocketHandler implements WebSocketHandler {

    private CascadeClassifier faceDetector;

    @Autowired
    public VideoStreamWebSocketHandler(CascadeClassifier faceDetector) {
        this.faceDetector = faceDetector;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive()
                .map(WebSocketMessage::getPayload)
                // The `flatMapSequential` is important for larger buffers to preserve order
                .flatMapSequential(dataBuffer -> Flux.fromIterable(dataBuffer::readableByteBuffers))
                .flatMap((ByteBuffer buffer) -> {
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);

                    Mat img = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
                    if (img.empty()) {
                        return Mono.empty();
                    }

                    Mat processed = detectObjects(img);

                    MatOfByte mob = new MatOfByte();
                    Imgcodecs.imencode(".jpg", processed, mob);
                    ByteBuffer outputBuffer = ByteBuffer.wrap(mob.toArray());

                    return Mono.just(session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(outputBuffer)));
                })
                .flatMap(message -> session.send(Mono.just(message))).then();
    }

    private Mat detectObjects(Mat frame) {
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(frame, faces);

        for (Rect rect : faces.toArray()) {
            Imgproc.rectangle(frame, rect, new Scalar(0, 255, 0), 2);
        }

        return frame;
    }
}
