package MikiMock.com.MikiMock.Config;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final ConsumerFactory<String, String> consumerFactory;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(){

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) ->
                                new TopicPartition(
                                        record.topic() + "-dlt",
                                        record.partition()
                                )
                );

        ExponentialBackOff backOff =
                new ExponentialBackOff();

        backOff.setInitialInterval(2000);

        backOff.setMultiplier(2);

        backOff.setMaxInterval(8000);

        DefaultErrorHandler handler =
                new DefaultErrorHandler(
                        recoverer,
                        backOff
                );

        handler.addNotRetryableExceptions(

                IllegalArgumentException.class,

                BusinessException.class
        );

        return handler;
    }

//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
//            DefaultErrorHandler kafkaErrorHandler
//    ) {
//
//        ConcurrentKafkaListenerContainerFactory<String, String> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//
//        factory.setConsumerFactory(consumerFactory);
//
//        factory.setCommonErrorHandler(kafkaErrorHandler);
//
//        return factory;
//    }

}