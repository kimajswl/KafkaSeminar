package com.example.kafkatest2.kafkaLibraryCustom;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.KafkaListenerConfigUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;

import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableKafka.class)
@RequiredArgsConstructor
public class KafkaAnnotationDrivenConfigCustom {
    @Autowired
    private KafkaProperties properties;

    private RecordMessageConverter recordMessageConverter;

    private RecordFilterStrategy<Object, Object> recordFilterStrategy;

    private BatchMessageConverter batchMessageConverter;

    private KafkaTemplate<Object, Object> kafkaTemplate;

    private KafkaAwareTransactionManager<Object, Object> transactionManager;

    private ConsumerAwareRebalanceListener rebalanceListener;

    private CommonErrorHandler commonErrorHandler;

    private AfterRollbackProcessor<Object, Object> afterRollbackProcessor;

    private RecordInterceptor<Object, Object> recordInterceptor;

    private BatchInterceptor<Object, Object> batchInterceptor;

    private Function<MessageListenerContainer, String> threadNameSupplier;

    void KafkaAnnotationDrivenConfiguration(KafkaProperties properties,
                                            ObjectProvider<RecordMessageConverter> recordMessageConverter,
                                            ObjectProvider<RecordFilterStrategy<Object, Object>> recordFilterStrategy,
                                            ObjectProvider<BatchMessageConverter> batchMessageConverter,
                                            ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplate,
                                            ObjectProvider<KafkaAwareTransactionManager<Object, Object>> kafkaTransactionManager,
                                            ObjectProvider<ConsumerAwareRebalanceListener> rebalanceListener,
                                            ObjectProvider<CommonErrorHandler> commonErrorHandler,
                                            ObjectProvider<AfterRollbackProcessor<Object, Object>> afterRollbackProcessor,
                                            ObjectProvider<RecordInterceptor<Object, Object>> recordInterceptor,
                                            ObjectProvider<BatchInterceptor<Object, Object>> batchInterceptor,
                                            ObjectProvider<Function<MessageListenerContainer, String>> threadNameSupplier) {
        this.properties = properties;
        this.recordMessageConverter = recordMessageConverter.getIfUnique();
        this.recordFilterStrategy = recordFilterStrategy.getIfUnique();
        this.batchMessageConverter = batchMessageConverter
                .getIfUnique(() -> new BatchMessagingMessageConverter(this.recordMessageConverter));
        this.kafkaTemplate = kafkaTemplate.getIfUnique();
        this.transactionManager = kafkaTransactionManager.getIfUnique();
        this.rebalanceListener = rebalanceListener.getIfUnique();
        this.commonErrorHandler = commonErrorHandler.getIfUnique();
        this.afterRollbackProcessor = afterRollbackProcessor.getIfUnique();
        this.recordInterceptor = recordInterceptor.getIfUnique();
        this.batchInterceptor = batchInterceptor.getIfUnique();
        this.threadNameSupplier = threadNameSupplier.getIfUnique();
    }

    @Bean(name = "customKafkaListenerContainerFactoryConfigCustom")
    @ConditionalOnMissingBean
    @ConditionalOnThreading(Threading.PLATFORM)
    ConcurrentKafkaListenerContainerFactoryConfigCustom kafkaListenerContainerFactoryConfigurer() {
        return configurer();
    }

    @Bean(name = "kafkaListenerContainerFactoryConfigCustom")
    @ConditionalOnMissingBean
    @ConditionalOnThreading(Threading.VIRTUAL)
    ConcurrentKafkaListenerContainerFactoryConfigCustom kafkaListenerContainerFactoryConfigurerVirtualThreads() {
        ConcurrentKafkaListenerContainerFactoryConfigCustom configurer = configurer();
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-");
        executor.setVirtualThreads(true);
        configurer.setListenerTaskExecutor(executor);
        return configurer;
    }

    private ConcurrentKafkaListenerContainerFactoryConfigCustom configurer() {
        ConcurrentKafkaListenerContainerFactoryConfigCustom configurer = new ConcurrentKafkaListenerContainerFactoryConfigCustom();
        configurer.setKafkaProperties(this.properties);
        configurer.setBatchMessageConverter(this.batchMessageConverter);
        configurer.setRecordMessageConverter(this.recordMessageConverter);
        configurer.setRecordFilterStrategy(this.recordFilterStrategy);
        configurer.setReplyTemplate(this.kafkaTemplate);
        configurer.setTransactionManager(this.transactionManager);
        configurer.setRebalanceListener(this.rebalanceListener);
        configurer.setCommonErrorHandler(this.commonErrorHandler);
        configurer.setAfterRollbackProcessor(this.afterRollbackProcessor);
        configurer.setRecordInterceptor(this.recordInterceptor);
        configurer.setBatchInterceptor(this.batchInterceptor);
        configurer.setThreadNameSupplier(this.threadNameSupplier);
        return configurer;
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactoryCustom")
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigCustom configurer,
            ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory,
            ObjectProvider<ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>> kafkaContainerCustomizer,
            ObjectProvider<SslBundles> sslBundles) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory.getIfAvailable(() -> new DefaultKafkaConsumerFactory<>(
                this.properties.buildConsumerProperties(sslBundles.getIfAvailable()))));
        kafkaContainerCustomizer.ifAvailable(factory::setContainerCustomizer);
        return factory;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableKafka
    @ConditionalOnMissingBean(name = KafkaListenerConfigUtils.KAFKA_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
    static class EnableKafkaConfiguration {

    }
}
