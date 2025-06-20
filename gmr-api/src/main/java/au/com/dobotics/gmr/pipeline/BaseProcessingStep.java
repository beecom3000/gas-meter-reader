package au.com.dobotics.gmr.pipeline;

import au.com.dobotics.gmr.model.ProcessingStage;

public abstract class BaseProcessingStep {

    protected ProcessingStage getProcessingStage(Context context) {
        return context.get(Context.Key.PROCESSING_STAGE, ProcessingStage.class);
    }

    protected boolean is(Context context, ProcessingStage processingStage) {
        ProcessingStage stage = getProcessingStage(context);
        return stage.is(processingStage);
    }

}
