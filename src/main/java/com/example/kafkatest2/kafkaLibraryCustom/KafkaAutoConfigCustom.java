package com.example.kafkatest2.kafkaLibraryCustom;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.kafka.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.*;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.retry.backoff.SleepingBackOffPolicy;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
@RequiredArgsConstructor
@Import({ KafkaAnnotationDrivenConfigCustom.class, KafkaStreamsAnnotationDrivenConfigCustom.class })
@Profile({"consumer1", "consumer2"})
public class KafkaAutoConfigCustom {
    private KafkaProperties properties;

    void KafkaAutoConfigurationCustom(KafkaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(KafkaConnectionDetails.class)
    PropertiesKafkaConnectionDetailsCustom kafkaConnectionDetails(KafkaProperties properties) {
        return new PropertiesKafkaConnectionDetailsCustom(properties);
    }

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<Object, Object> kafkaProducerFactory,
                                             ProducerListener<Object, Object> kafkaProducerListener,
                                             ObjectProvider<RecordMessageConverter> messageConverter) {
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
        messageConverter.ifUnique(kafkaTemplate::setMessageConverter);
        map.from(kafkaProducerListener).to(kafkaTemplate::setProducerListener);
        map.from(this.properties.getTemplate().getDefaultTopic()).to(kafkaTemplate::setDefaultTopic);
        map.from(this.properties.getTemplate().getTransactionIdPrefix()).to(kafkaTemplate::setTransactionIdPrefix);
        map.from(this.properties.getTemplate().isObservationEnabled()).to(kafkaTemplate::setObservationEnabled);
        return kafkaTemplate;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.producer.transaction-id-prefix")
    @ConditionalOnMissingBean
    public KafkaTransactionManager<?, ?> kafkaTransactionManager(ProducerFactory<?, ?> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.jaas.enabled")
    @ConditionalOnMissingBean
    public KafkaJaasLoginModuleInitializer kafkaJaasInitializer() throws IOException {
        KafkaJaasLoginModuleInitializer jaas = new KafkaJaasLoginModuleInitializer();
        KafkaProperties.Jaas jaasProperties = this.properties.getJaas();
        if (jaasProperties.getControlFlag() != null) {
            jaas.setControlFlag(jaasProperties.getControlFlag());
        }
        if (jaasProperties.getLoginModule() != null) {
            jaas.setLoginModule(jaasProperties.getLoginModule());
        }
        jaas.setOptions(jaasProperties.getOptions());
        return jaas;
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaAdmin kafkaAdmin(KafkaConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> properties = this.properties.buildAdminProperties(sslBundles.getIfAvailable());
        applyKafkaConnectionDetailsForAdmin(properties, connectionDetails);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(properties);
        KafkaProperties.Admin admin = this.properties.getAdmin();
        if (admin.getCloseTimeout() != null) {
            kafkaAdmin.setCloseTimeout((int) admin.getCloseTimeout().getSeconds());
        }
        if (admin.getOperationTimeout() != null) {
            kafkaAdmin.setOperationTimeout((int) admin.getOperationTimeout().getSeconds());
        }
        kafkaAdmin.setFatalIfBrokerNotAvailable(admin.isFailFast());
        kafkaAdmin.setModifyTopicConfigs(admin.isModifyTopicConfigs());
        kafkaAdmin.setAutoCreate(admin.isAutoCreate());
        return kafkaAdmin;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.retry.topic.enabled")
    @ConditionalOnSingleCandidate(KafkaTemplate.class)
    public RetryTopicConfiguration kafkaRetryTopicConfiguration(KafkaTemplate<?, ?> kafkaTemplate) {
        KafkaProperties.Retry.Topic retryTopic = this.properties.getRetry().getTopic();
        RetryTopicConfigurationBuilder builder = RetryTopicConfigurationBuilder.newInstance()
                .maxAttempts(retryTopic.getAttempts())
                .useSingleTopicForSameIntervals()
                .suffixTopicsWithIndexValues()
                .doNotAutoCreateRetryTopics();
        setBackOffPolicy(builder, retryTopic.getBackoff());
        return builder.create(kafkaTemplate);
    }

    private void applyKafkaConnectionDetailsForAdmin(Map<String, Object> properties,
                                                     KafkaConnectionDetails connectionDetails) {
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getAdminBootstrapServers());
        if (!(connectionDetails instanceof PropertiesKafkaConnectionDetailsCustom)) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
        }
    }

    private static void setBackOffPolicy(RetryTopicConfigurationBuilder builder, KafkaProperties.Retry.Topic.Backoff retryTopicBackoff) {
        long delay = (retryTopicBackoff.getDelay() != null) ? retryTopicBackoff.getDelay().toMillis() : 0;
        if (delay > 0) {
            PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
            BackOffPolicyBuilder backOffPolicy = BackOffPolicyBuilder.newBuilder();
            map.from(delay).to(backOffPolicy::delay);
            map.from(retryTopicBackoff.getMaxDelay()).as(Duration::toMillis).to(backOffPolicy::maxDelay);
            map.from(retryTopicBackoff.getMultiplier()).to(backOffPolicy::multiplier);
            map.from(retryTopicBackoff.isRandom()).to(backOffPolicy::random);
            builder.customBackoff((SleepingBackOffPolicy<?>) backOffPolicy.build());
        }
        else {
            builder.noBackoff();
        }
    }
}
