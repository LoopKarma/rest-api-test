package app.processor;

import app.dto.ProcessingEventCtx;

class EventsProducer {
    void produceProcessingResultEvent(TransferExecutor.ProcessingResult result, ProcessingEventCtx context) {
        /*
            send event to some queue to later consuming in clients and backend applications
         */
    }
}
