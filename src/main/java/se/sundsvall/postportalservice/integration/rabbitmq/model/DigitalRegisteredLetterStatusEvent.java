package se.sundsvall.postportalservice.integration.rabbitmq.model;

public record DigitalRegisteredLetterStatusEvent(
	String recipientId,
	String externalId,
	String status,
	String statusDetail) {}
