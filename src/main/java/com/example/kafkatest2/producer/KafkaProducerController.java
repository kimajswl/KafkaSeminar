package com.example.kafkatest2.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("producer")
public class KafkaProducerController {
    private final KafkaProducer kafkaProducer;

    @PostMapping("/kafka/send")
    public String sendMessage(@RequestParam String message) throws Exception {
        try {
            kafkaProducer.sendMessage(message);

            return "success";
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
}
