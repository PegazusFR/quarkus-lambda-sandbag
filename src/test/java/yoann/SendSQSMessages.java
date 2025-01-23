package yoann;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.List;
import java.util.UUID;

public class SendSQSMessages {

    public static void main(String[] args){
        SqsClient sqsClient = SqsClient.builder().build();


        List<SendMessageBatchRequestEntry> list = List.of("a","b","c","d","e")
                .stream()
                .map(msg -> SendMessageBatchRequestEntry
                        .builder()
                        .id(UUID.randomUUID().toString())
                        .messageBody(msg)
                        .build())
                .toList();

        SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest
                .builder()
                .entries(list)
                .queueUrl("https://sqs.eu-central-1.amazonaws.com/372345383787/terraform-example-queue")
                .build();

        SendMessageBatchResponse sendMessageBatchResponse = sqsClient.sendMessageBatch(sendMessageBatchRequest);

        System.out.println(sendMessageBatchRequest.toString());
    }
}
