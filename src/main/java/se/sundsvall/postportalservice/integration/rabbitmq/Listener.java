package se.sundsvall.postportalservice.integration.rabbitmq;

public interface Listener {

	void handleEvent(String id);

}
