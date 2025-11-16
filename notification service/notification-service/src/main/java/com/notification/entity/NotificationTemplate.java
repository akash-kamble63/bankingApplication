package com.notification.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_templates", indexes = {
		@Index(name = "idx_template_code", columnList = "template_code", unique = true),
		@Index(name = "idx_type_channel", columnList = "type, channel") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "template_code", unique = true, nullable = false, length = 100)
	private String templateCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationChannel channel;

	@Column(nullable = false, length = 200)
	private String subject;

	@Column(name = "content_template", columnDefinition = "TEXT", nullable = false)
	private String contentTemplate; // Template with placeholders: {{name}}, {{amount}}

	@Column(name = "html_template", columnDefinition = "TEXT")
	private String htmlTemplate; // For email

	@Column(columnDefinition = "jsonb")
	private String variables; // JSON array of required variables

	@Column(nullable = false)
	private Boolean active = true;

	@Column(length = 500)
	private String description;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
