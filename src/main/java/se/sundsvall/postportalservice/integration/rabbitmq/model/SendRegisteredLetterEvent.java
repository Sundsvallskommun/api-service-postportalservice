package se.sundsvall.postportalservice.integration.rabbitmq.model;

public record SendRegisteredLetterEvent(String municipalityId, String recipientId) {
}
