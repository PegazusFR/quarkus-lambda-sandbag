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
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * https://stackoverflow.com/questions/67495287/uni-combine-all-unis-v-s-multi-onitem-transformtomultiandconcatenate
 * https://smallrye.io/smallrye-mutiny/2.8.0/guides/combining-items/#combining-unis
 * https://www.naiyerasif.com/post/2024/02/11/using-localstack-for-aws-lambda-with-sqs-trigger/
 */
@Named("sqs-p2")
public class SQSParallelLambda2 implements RequestHandler<SQSEvent, SQSBatchResponse> {

    @Inject
    Logger logger;

    ExecutorService executorService = Executors.newFixedThreadPool(50);

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        Instant start = Instant.now();

        logger.infof("Got %s messages", sqsEvent.getRecords().size());
        List<Uni<SQSBatchResponse.BatchItemFailure>> unis =
                sqsEvent
                        .getRecords()
                        .stream()
                        .map(this::createUni)
                        .toList();

        Uni<ArrayList<SQSBatchResponse.BatchItemFailure>> uniAndGroupIterable = Uni
                .combine()
                .all()
                .unis(unis)
                .with(SQSBatchResponse.BatchItemFailure.class, ArrayList::new);

        ArrayList<SQSBatchResponse.BatchItemFailure> items =
                uniAndGroupIterable
                        .await()
                        .atMost(Duration.ofSeconds(20));

        logger.infof("The result is %s", items);


        Instant end = Instant.now();
        long time = end.toEpochMilli() - start.toEpochMilli();
        logger.infof("It took %s ms for %s messages", time, sqsEvent.getRecords().size());
        return SQSBatchResponse
                .builder()
                .build();
    }

    Uni<SQSBatchResponse.BatchItemFailure> createUni(SQSEvent.SQSMessage sqsMessage) {
        return Uni.createFrom()
                .item(sqsMessage)
                .chain((msg) -> {
                    SQSBatchResponse.BatchItemFailure.BatchItemFailureBuilder msgFailure = SQSBatchResponse.BatchItemFailure.builder();
                    try {
                        // simulate message processing
                        Thread.sleep(1000L);
                    } catch (Exception e) {
                        logger.errorf("UNI - exception: %s", e);
                        msgFailure.withItemIdentifier(sqsMessage.getMessageId());
                    }
                    logger.infof("UNI after sleep - %s : %s for msg = %s", Thread.currentThread().getId(), Thread.currentThread().getName(), msg.getBody());
                    return Uni.createFrom().item(msgFailure.build());
                })
                .runSubscriptionOn(executorService);
    }

}
