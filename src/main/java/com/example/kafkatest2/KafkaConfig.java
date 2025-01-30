package com.example.kafkatest2;

import com.example.kafkatest2.kafkaLibraryCustom.KafkaStreamsDefaultConfigCustom;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean(name = KafkaStreamsDefaultConfigCustom.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration defaultKafkaStreamsConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-app"); // 애플리케이션 ID
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // Kafka 서버 주소
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");

        return new KafkaStreamsConfiguration(config);
    }

    @Bean
    public KafkaProperties kafkaProperties() {
        return new KafkaProperties();
    }

    @Bean
    public KafkaConnectionDetails kafkaConnectionDetails(KafkaProperties kafkaProperties) {
        return new KafkaConnectionDetails() {
            @Override
            public List<String> getBootstrapServers() {
                return null;
            }

            @Override
            public List<String> getConsumerBootstrapServers() {
                return Collections.singletonList(String.join(",", kafkaProperties.getBootstrapServers()));
            }

            @Override
            public List<String> getProducerBootstrapServers() {
                return Collections.singletonList(String.join(",", kafkaProperties.getBootstrapServers()));
            }

            @Override
            public List<String> getAdminBootstrapServers() {
                return Collections.singletonList(String.join(",", kafkaProperties.getBootstrapServers()));
            }
        };
    }
}
