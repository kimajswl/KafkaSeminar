package com.example.kafkatest2.kafkaLibraryCustom;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import java.util.List;

@RequiredArgsConstructor
public class PropertiesKafkaConnectionDetailsCustom implements KafkaConnectionDetails {
    private KafkaProperties properties;

    public PropertiesKafkaConnectionDetailsCustom(KafkaProperties properties) {
        this.properties = properties;
    }


    public void PropertiesKafkaConnectionDetailsCustom(KafkaProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> getBootstrapServers() {
        return this.properties.getBootstrapServers();
    }

    @Override
    public List<String> getConsumerBootstrapServers() {
        return getServers(this.properties.getConsumer().getBootstrapServers());
    }

    @Override
    public List<String> getProducerBootstrapServers() {
        return getServers(this.properties.getProducer().getBootstrapServers());
    }

    @Override
    public List<String> getStreamsBootstrapServers() {
        return getServers(this.properties.getStreams().getBootstrapServers());
    }

    private List<String> getServers(List<String> servers) {
        return (servers != null) ? servers : getBootstrapServers();
    }
}
