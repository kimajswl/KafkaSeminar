package com.example.kafkatest2.kafkaLibraryCustom;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

@Configuration(proxyBeanMethods = false)
public class KafkaStreamsDefaultConfigCustom {
    public static final String DEFAULT_STREAMS_CONFIG_BEAN_NAME = "customKafkaStreamsConfig";

    /**
     * The bean name for auto-configured default {@link StreamsBuilderFactoryBean}.
     */
    public static final String DEFAULT_STREAMS_BUILDER_BEAN_NAME = "customKafkaStreamsBuilder";

    /**
     * Bean for the default {@link StreamsBuilderFactoryBean}.
     * @param streamsConfigProvider the streams config.
     * @param configurerProvider the configurer.
     *
     * @return the factory bean.
     */
    @Bean(name = DEFAULT_STREAMS_BUILDER_BEAN_NAME)
    public StreamsBuilderFactoryBean defaultKafkaStreamsBuilder(
            @Qualifier(DEFAULT_STREAMS_CONFIG_BEAN_NAME)
            ObjectProvider<KafkaStreamsConfiguration> streamsConfigProvider,
            ObjectProvider<StreamsBuilderFactoryBeanConfigurer> configurerProvider) {

        KafkaStreamsConfiguration streamsConfig = streamsConfigProvider.getIfAvailable();
        if (streamsConfig != null) {
            StreamsBuilderFactoryBean fb = new StreamsBuilderFactoryBean(streamsConfig);
            configurerProvider.orderedStream().forEach(configurer -> configurer.configure(fb));
            return fb;
        }
        else {
            throw new UnsatisfiedDependencyException(KafkaStreamsDefaultConfigCustom.class.getName(),
                    DEFAULT_STREAMS_BUILDER_BEAN_NAME, "streamsConfig", "There is no '" +
                    DEFAULT_STREAMS_CONFIG_BEAN_NAME + "' " + KafkaStreamsConfiguration.class.getName() +
                    " bean in the application context.\n" +
                    "Consider declaring one or don't use @EnableKafkaStreams.");
        }
    }
}
