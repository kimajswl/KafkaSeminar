package com.example.kafkatest2.kafkaLibraryCustom;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.*;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.CleanupConfig;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StreamsBuilder.class)
@ConditionalOnBean(name = KafkaStreamsDefaultConfigCustom.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
@RequiredArgsConstructor
public class KafkaStreamsAnnotationDrivenConfigCustom {

    private final KafkaProperties properties;

    @ConditionalOnMissingBean
    @Bean("customKafkaStreamsConfig")
    KafkaStreamsConfiguration customKafkaStreamsConfig(Environment environment,
                                                       KafkaConnectionDetails connectionDetails,
                                                       ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> properties = this.properties.buildStreamsProperties(sslBundles.getIfAvailable());
        applyKafkaConnectionDetailsForStreams(properties, connectionDetails);
        if (this.properties.getStreams().getApplicationId() == null) {
            String applicationName = environment.getProperty("spring.application.name");
            if (applicationName == null) {
                throw new InvalidConfigurationPropertyValueException("spring.kafka.streams.application-id", null,
                        "This property is mandatory and fallback 'spring.application.name' is not set either.");
            }
            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
        }
        return new KafkaStreamsConfiguration(properties);
    }

    @Bean("customKafkaStreamsFactoryBeanConfigurer")
    KafkaStreamsAnnotationDrivenConfigCustom.KafkaStreamsFactoryBeanConfigurer customKafkaStreamsFactoryBeanConfigurer(
            @Qualifier(KafkaStreamsDefaultConfigCustom.DEFAULT_STREAMS_BUILDER_BEAN_NAME) StreamsBuilderFactoryBean factoryBean,
            ObjectProvider<StreamsBuilderFactoryBeanCustomizer> customizers) {
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factoryBean));
        return new KafkaStreamsAnnotationDrivenConfigCustom.KafkaStreamsFactoryBeanConfigurer(this.properties, factoryBean);
    }

    private void applyKafkaConnectionDetailsForStreams(Map<String, Object> properties,
                                                       KafkaConnectionDetails connectionDetails) {
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getStreamsBootstrapServers());
        if (!(connectionDetails instanceof PropertiesKafkaConnectionDetailsCustom)) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
        }
    }

    static class KafkaStreamsFactoryBeanConfigurer implements InitializingBean {

        private final KafkaProperties properties;

        private final StreamsBuilderFactoryBean factoryBean;

        KafkaStreamsFactoryBeanConfigurer(KafkaProperties properties, StreamsBuilderFactoryBean factoryBean) {
            this.properties = properties;
            this.factoryBean = factoryBean;
        }

        @Override
        public void afterPropertiesSet() {
            this.factoryBean.setAutoStartup(this.properties.getStreams().isAutoStartup());
            KafkaProperties.Cleanup cleanup = this.properties.getStreams().getCleanup();
            CleanupConfig cleanupConfig = new CleanupConfig(cleanup.isOnStartup(), cleanup.isOnShutdown());
            this.factoryBean.setCleanupConfig(cleanupConfig);
        }
    }
}
