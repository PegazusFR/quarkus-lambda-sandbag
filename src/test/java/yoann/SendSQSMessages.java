package yoann;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SendSQSMessages {

    public static void main(String[] args) {
        SqsClient sqsClient = SqsClient.builder().build();

        List<List<SendMessageBatchRequestEntry>> batch = getLists();

        batch.forEach(batchItems -> {
            SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest
                    .builder()
                    .entries(batchItems)
                    .queueUrl("https://sqs.eu-central-1.amazonaws.com/372345383787/terraform-example-queue")
                    .build();
            SendMessageBatchResponse sendMessageBatchResponse = sqsClient.sendMessageBatch(sendMessageBatchRequest);
            System.out.println(sendMessageBatchResponse.toString());
        });


    }

    private static List<List<SendMessageBatchRequestEntry>> getLists() {
        List<Integer> listInt = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<List<SendMessageBatchRequestEntry>> batch = new ArrayList<>();


        listInt.forEach(value -> {
                    batch.add(List.of(
                                    "a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
                            .stream()
                            .map(msg -> SendMessageBatchRequestEntry
                                    .builder()
                                    .id(UUID.randomUUID().toString())
                                    .messageBody(msg + " - " + value)
                                    .build())
                            .toList());
                }
        );
        return batch;
    }
}
