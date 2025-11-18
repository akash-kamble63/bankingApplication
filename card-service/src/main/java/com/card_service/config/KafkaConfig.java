package com.card_service.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {
	@Value("${spring.kafka.bootstrap-servers:localhost:9092}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.group-id:card-service-group}")
	private String groupId;

	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				org.apache.kafka.common.serialization.StringSerializer.class);
		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				org.springframework.kafka.support.serializer.JsonSerializer.class);

		// Idempotency and reliability
		configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		configProps.put(ProducerConfig.ACKS_CONFIG, "all");
		configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
		configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

		// Performance optimization
		configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
		configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
		configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
		configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

		// Timeout settings
		configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
		configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

		return new DefaultKafkaProducerFactory<>(configProps);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	@Bean
	public ConsumerFactory<String, Object> consumerFactory() {
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				org.apache.kafka.common.serialization.StringDeserializer.class);
		configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

		configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.card_service.*");
		configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.card_service.dto.BaseEvent");
		configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

		configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
		configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

		return new DefaultKafkaConsumerFactory<>(configProps);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.setConcurrency(3);
		factory.getContainerProperties().setPollTimeout(3000);

		// Manual acknowledgment
		factory.getContainerProperties()
				.setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

		return factory;
	}

	// Card-specific Kafka Topics
	@Bean
	public NewTopic cardIssuedTopic() {
		return TopicBuilder.name("banking.card.issued").partitions(3).replicas(1).compact().build();
	}

	@Bean
	public NewTopic cardActivatedTopic() {
		return TopicBuilder.name("banking.card.activated").partitions(3).replicas(1).compact().build();
	}

	@Bean
	public NewTopic cardBlockedTopic() {
		return TopicBuilder.name("banking.card.blocked").partitions(3).replicas(1).compact().build();
	}

	@Bean
	public NewTopic cardTransactionTopic() {
		return TopicBuilder.name("banking.card.transaction").partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic cardEventsTopic() {
		return TopicBuilder.name("banking.card.events").partitions(3).replicas(1).build();
	}
}
