package com.example.kafkatest2.consumer;

import com.example.kafkatest2.kafkaLibraryCustom.PropertiesKafkaConnectionDetailsCustom;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Profile({"consumer1", "consumer2"})
public class KafkaConsumerConfig {

    private final KafkaProperties properties;
    private final ObjectProvider<KafkaConnectionDetails> connectionDetailsProvider;
    private final ObjectProvider<SslBundles> sslBundlesProvider;
    private final ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers;

    @Bean
    @ConditionalOnMissingBean(ConsumerFactory.class)
    public ConsumerFactory<?, ?> consumerFactory() {
        SslBundles sslBundles = this.sslBundlesProvider.getIfAvailable();

        Map<String, Object> properties = this.properties.buildConsumerProperties(sslBundles);

        KafkaConnectionDetails connectionDetails = this.connectionDetailsProvider.getIfAvailable();

        applyKafkaConnectionDetailsForConsumer(properties, connectionDetails);

        DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(properties);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
        return factory;
    }

    private void applyKafkaConnectionDetailsForConsumer(Map<String, Object> properties,
                                                        KafkaConnectionDetails connectionDetails) {
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumerBootstrapServers());
        if (!(connectionDetails instanceof PropertiesKafkaConnectionDetailsCustom)) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
        }
    }
}
