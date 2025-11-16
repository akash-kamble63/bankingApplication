package com.transaction_service.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {
	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;

	/**
	 * Producer Factory with idempotency and reliability settings
	 */
	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

		// Idempotency and reliability
		config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		config.put(ProducerConfig.ACKS_CONFIG, "all");
		config.put(ProducerConfig.RETRIES_CONFIG, 3);
		config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

		// Performance optimization
		config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
		config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
		config.put(ProducerConfig.LINGER_MS_CONFIG, 10);

		// Timeout settings
		config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
		config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	/**
	 * Consumer Factory with error handling and manual commit
	 */
	@Bean
	public ConsumerFactory<String, Object> consumerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

		// Use ErrorHandlingDeserializer to wrap the actual deserializers
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

		// Delegate to the actual deserializers
		config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
		config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

		// JsonDeserializer specific configs
		config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.transaction_service.*,com.fraud_service.*");
		config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.transaction_service.DTOs.FraudResultEvent");
		config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit

		// Session and poll timeouts
		config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
		config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
		config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

		return new DefaultKafkaConsumerFactory<>(config);
	}

	/**
	 * Kafka Listener Container Factory with manual acknowledgment
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.setConcurrency(3); // 3 concurrent consumers
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

		// Error handling
		factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
				new org.springframework.util.backoff.FixedBackOff(1000L, 3L) // Retry 3 times with 1 second delay
		));

		return factory;
	}


	@Bean
	public NewTopic transactionInitiatedTopic() {
		return TopicBuilder.name("banking.transaction.initiated").partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic transactionCompletedTopic() {
		return TopicBuilder.name("banking.transaction.completed").partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic transactionFailedTopic() {
		return TopicBuilder.name("banking.transaction.failed").partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic fraudResultTopic() {
		return TopicBuilder.name("banking.fraud.result").partitions(3).replicas(1).build();
	}
}
