package com.example.kafkatest2.producer;

import com.example.kafkatest2.kafkaLibraryCustom.PropertiesKafkaConnectionDetailsCustom;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Profile("producer")
public class KafkaProducerConfig {
    private final KafkaProperties properties;
    private final ObjectProvider<KafkaConnectionDetails> connectionDetailsProvider;
    private final ObjectProvider<SslBundles> sslBundlesProvider;
    private final ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers;

    @Bean
    @ConditionalOnMissingBean(ProducerListener.class) // 이런거 다 커스텀으로 빼야할듯?
    public LoggingProducerListener<Object, Object> kafkaProducerListener() {
        return new LoggingProducerListener<>();
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {

        SslBundles sslBundles = this.sslBundlesProvider.getIfAvailable();

        Map<String, Object> properties = this.properties.buildProducerProperties(sslBundles);

        KafkaConnectionDetails connectionDetails = this.connectionDetailsProvider.getIfAvailable();

        applyKafkaConnectionDetailsForProducer(properties, connectionDetails);
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(properties);
        String transactionIdPrefix = this.properties.getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public DefaultKafkaProducerFactory<?, ?> kafkaProducerFactory(KafkaConnectionDetails connectionDetails,
                                                                  ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers, ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> properties = this.properties.buildProducerProperties(sslBundles.getIfAvailable());
        applyKafkaConnectionDetailsForProducer(properties, connectionDetails);
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(properties);
        String transactionIdPrefix = this.properties.getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private void applyKafkaConnectionDetailsForProducer(Map<String, Object> properties,
                                                        KafkaConnectionDetails connectionDetails) {
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducerBootstrapServers());
        if (!(connectionDetails instanceof PropertiesKafkaConnectionDetailsCustom)) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
        }
    }
}
