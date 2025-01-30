package com.example.kafkatest2;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamsTopology {
    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("topic1");
        stream.foreach((key, value) -> {
            System.out.println("Key: " + key + ", Value: " + value);
        });
        return stream;
    }
}
