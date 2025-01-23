package yoann;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAndGroupIterable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Named("sqs-p")
public class SQSParallelLambda implements RequestHandler<SQSEvent, SQSBatchResponse> {

    @Inject
    Logger logger;

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        Instant start = Instant.now();

        logger.infof("Got %s messages", sqsEvent.getRecords().size());
        List<Uni<Optional<SQSBatchResponse.BatchItemFailure>>> unis =
                sqsEvent
                        .getRecords()
                        .stream()
                        .map(this::createUni)
                        .toList();

        UniAndGroupIterable<Object> uniAndGroupIterable = Uni
                .combine()
                .all()
                .unis(unis)
                .usingConcurrencyOf(10)
                .collectFailures();


        Instant end = Instant.now();
        long time = end.toEpochMilli() - start.toEpochMilli();
        logger.infof("It took %s ms for %s messages", time, sqsEvent.getRecords().size());
        return SQSBatchResponse
                .builder()
                .build();
    }

    Uni<Optional<SQSBatchResponse.BatchItemFailure>> createUni(SQSEvent.SQSMessage sqsMessage) {
        return Uni.createFrom()
                .item(sqsMessage)
                .chain((msg) -> {
                    SQSBatchResponse.BatchItemFailure msgFailure = null;
                    logger.infof("UNI before sleep - %s : %s for msg = %s", Thread.currentThread().getId(), Thread.currentThread().getName(), msg.getBody());
                    try {
                        // simulate message processing
                        Thread.sleep(3000L);
                    } catch (Exception e) {
                        logger.errorf("UNI - exception: %s", e);
                    }
                    logger.infof("UNI after sleep - %s : %s for msg = %s", Thread.currentThread().getId(), Thread.currentThread().getName(), msg.getBody());
                    return Uni.createFrom().item(Optional.ofNullable(msgFailure));
                })
                .runSubscriptionOn(executorService);
    }

}
