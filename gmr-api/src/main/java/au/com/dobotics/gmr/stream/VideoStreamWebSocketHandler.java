package au.com.dobotics.gmr.stream;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.opencv.core.MatOfByte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

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

                    Mat img = imdecode(new Mat(bytes), IMREAD_COLOR);
                    if (img.empty()) {
                        return Mono.empty();
                    }

                    Mat processed = detectObjects(img);

                    ByteBuffer outputBuffer = ByteBuffer.allocate(1);
                    imencode(".jpg", processed, outputBuffer);

                    return Mono.just(session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(outputBuffer)));
                })
                .flatMap(message -> session.send(Mono.just(message))).then();
    }

    private Mat detectObjects(Mat frame) {
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(frame, faces);

        for (Rect rect : faces.get()) {
            rectangle(frame, rect, Scalar.GREEN);
        }

        return frame;
    }
}
