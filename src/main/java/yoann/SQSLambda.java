package yoann;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


@Named("sqs")
public class SQSLambda implements RequestHandler<SQSEvent, SQSBatchResponse> {

    @Inject
    Logger logger;

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        logger.infof("There are %s messages", sqsEvent.getRecords().size());
        List<String> bodies = sqsEvent.getRecords()
                .stream()
                .parallel()
                .map(msg -> {
                    logger.infof(msg.getBody());
                    return msg.getBody();
                })
                .toList();

        List<SQSBatchResponse.BatchItemFailure> failures = sqsEvent.getRecords().stream().map(msg -> SQSBatchResponse
                .BatchItemFailure
                .builder()
                .withItemIdentifier(msg.getMessageId())
                .build())
                .toList();

        logger.infof("bodies=%s", bodies);
        logger.errorf("failures=%s", failures);
        return SQSBatchResponse
                .builder()
                .withBatchItemFailures(failures)
                .build();
    }
}
