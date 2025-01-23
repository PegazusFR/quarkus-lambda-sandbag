package yoann;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Named("test")
public class TestLambda implements RequestHandler<InputObject, OutputObject> {

    @Inject
    Logger logger;

    @Inject
    ProcessingService service;

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public OutputObject handleRequest(InputObject input, Context context) {
        logger.infof("Handler - " + Thread.currentThread().getId() + ":" + Thread.currentThread().getName());
        Uni<String> uni1 = createUni("Titi", 1000L);
        Uni<String> uni2 = createUni("Toto", 2000L);
        Uni<String> uni3 = createUni("Toto1", 3000L);
        Uni<String> uni4 = createUni("Toto2", 4000L);

        Uni.combine()
                .all()
                .unis(uni1, uni2, uni3, uni4)
                .asTuple()
                .await()
                .atMost(Duration.ofMinutes(1))
                .forEach(e -> logger.infof("result : %s", e));
        logger.infof("multiSubscribe");
        return service.process(input).setRequestId(context.getAwsRequestId());
    }

    Uni<String> createUni(String value, Long sleep) {
        return Uni.createFrom()
                .item(value)
                .onItem()
                .call((v) -> {
                    logger.infof("UNI before sleep - " + Thread.currentThread().getId() + ":" + Thread.currentThread().getName());
                    try {
                        Thread.sleep(sleep);
                    } catch (Exception e) {
                        logger.errorf("UNI - exception: %s", e);
                    }
                    logger.infof("UNI after sleep - " + Thread.currentThread().getId() + ":" + Thread.currentThread().getName());
                    return Uni.createFrom().item(v);
                })
                // important the run concurrently
                .runSubscriptionOn(executorService)
                .log("Before chain")
                .chain(v -> Uni.createFrom().item(v.toUpperCase()))
                .log("After chain");
    }
}
