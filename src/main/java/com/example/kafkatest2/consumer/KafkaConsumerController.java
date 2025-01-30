package com.example.kafkatest2.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile({"consumer1", "consumer2"})
public class KafkaConsumerController {
    private final KafkaConsumer kafkaConsumer;

    @GetMapping("/kafka/get")
    public List<String> getMessage() {
        return kafkaConsumer.getAllMessage();
    }
}
