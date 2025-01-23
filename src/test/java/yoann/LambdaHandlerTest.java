package yoann;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAndGroupIterable;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class LambdaHandlerTest {

    @Inject
    Logger logger;

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    List<MessageCustom> messageCustoms = new ArrayList(10);

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        // you test your lambdas by invoking on http://localhost:8081
        // this works in dev mode too

        InputObject in = new InputObject();
        in.setName("Stu");
        in.setGreeting("Hello");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hello Stu"));
    }

    public record MessageCustom(String id, String msg, Long time, boolean success) {
    }

    @Test
    public void uniAll() throws Exception {

        // Given
        Uni<MessageCustom> uni1 = createUni(new MessageCustom("1", "Titi", 1000L, true));
        Uni<MessageCustom> uni2 = createUni(new MessageCustom("2", "Toto", 200L, true));
        Uni<MessageCustom> uni3 = createUni(new MessageCustom("3", "Tata", 3000L, true));
        Uni<MessageCustom> uni4 = createUni(new MessageCustom("4", "Tutu", 2222L, true));
        List<Uni<MessageCustom>> unis = List.of(uni1, uni2, uni3, uni4);
        // run

        UniAndGroupIterable<Object> uniAndGroupIterable = Uni
                .combine()
                .all()
                .unis(unis)
                .usingConcurrencyOf(10)
                .collectFailures();

        // Assertions
        List<MessageCustom> ll = uniAndGroupIterable.collectFailures()
                .with(MessageCustom.class, (list) -> {
                    logger.infof("Function %s", list);
                    return list;
                })
                .await()
                .atMost(Duration.ofMinutes(1));

        Assertions.assertEquals(4, ll.size());

    }

    Uni<MessageCustom> createUni(MessageCustom messageCustom) {
        return Uni.createFrom()
                .item(messageCustom)
                .chain((msg) -> {
                    SQSBatchResponse.BatchItemFailure msgFailure = null;
                    Instant start = Instant.now();
                    logger.infof("UNI msg = %s", msg);
                    try {
                        // simulate message processing
                        Thread.sleep(messageCustom.time());
                    } catch (Exception e) {
                        logger.errorf("UNI - exception: %s", e);
                    }
                    if (msg.msg.equals("Toto")) {
                        return Uni.createFrom().item(new MessageCustom(msg.id, msg.msg, msg.time, false));
                        //return Uni.createFrom().failure(new Throwable("Fail processing " + msg.id));
                    }
                    Instant stop = Instant.now();
                    logger.infof("UNI timeRun= %s, msg = %s", (stop.toEpochMilli() - start.toEpochMilli()), msg);
                    return Uni.createFrom().item(msg);
                })
                .onFailure()
                    .retry()
                        .atMost(1)
                .runSubscriptionOn(executorService);
    }

}
