package au.com.dobotics.gmr.model;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Config {
    @Value("${rotationDegrees}")
    private String rotationDegrees;

    @Value("${cannyThreshold1}")
    private String cannyThreshold1;
}


//rotationDegrees: 0
//cannyThreshold1: 100
//cannyThreshold2: 200
//digitMinHeight: 20
//digitMaxHeight: 90
//digitYAlignment: 10
//ocrMaxDist: 600000.
//trainingDataFilename: "training.yml"
