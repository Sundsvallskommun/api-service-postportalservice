package se.sundsvall.postportalservice.service;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SMS;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageResult;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageStatus;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.employee.EmployeeIntegration;
import se.sundsvall.postportalservice.integration.employee.EmployeeUtil;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;

@Service
public class MessageService {

	private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

	private final MessagingIntegration messagingIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final EmployeeIntegration employeeIntegration;

	private final DepartmentRepository departmentRepository;
	private final UserRepository userRepository;
	private final MessageRepository messageRepository;

	public MessageService(
		final MessagingIntegration messagingIntegration,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final EmployeeIntegration employeeIntegration,
		final DepartmentRepository departmentRepository,
		final UserRepository userRepository,
		final MessageRepository messageRepository) {
		this.messagingIntegration = messagingIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.employeeIntegration = employeeIntegration;
		this.departmentRepository = departmentRepository;
		this.userRepository = userRepository;
		this.messageRepository = messageRepository;
	}

	/**
	 * Maps an incoming SmsRequest to a MessageEntity. Persists the MessageEntity and its associated entities to the
	 * database. Sends a message to each recipient asynchronously. Returns the MessageEntity ID that can be used to read the
	 * message.
	 */
	public String processRequest(final String municipalityId, final SmsRequest smsRequest) {
		var sentBy = getSentBy(municipalityId);

		var user = getOrCreateUser(sentBy.userName);
		var department = getOrCreateDepartment(sentBy);

		var senderInfo = messagingSettingsIntegration.getSenderInfo(municipalityId, department.getOrganizationId());

		var recipients = smsRequest.getRecipients().stream()
			.map(EntityMapper::toRecipientEntity)
			.toList();

		var message = MessageEntity.create()
			.withDisplayName(senderInfo.getSmsSender())
			.withMunicipalityId(municipalityId)
			.withUser(user)
			.withDepartment(department)
			.withRecipients(recipients)
			.withText(smsRequest.getMessage())
			.withMessageType(SMS);

		message = messageRepository.save(message);

		processRecipients(message);
		return message.getId();
	}

	CompletableFuture<Void> processRecipients(final MessageEntity messageEntity) {
		var futures = messageEntity.getRecipients().stream()
			.map(recipientEntity -> sendMessageToRecipient(messageEntity, recipientEntity))
			.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures)
			.handle((v, throwable) -> {
				messageRepository.save(messageEntity);
				if (throwable != null) {
					LOG.error(throwable.getMessage(), throwable);
				}
				return null;
			});
	}

	CompletableFuture<Void> sendMessageToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return switch (recipientEntity.getMessageType()) {
			case SMS -> sendSmsToRecipient(messageEntity, recipientEntity);
			// case DIGITAL_MAIL -> sendDigitalMailToRecipient(messageEntity, recipientEntity);
			// case SNAIL_MAIL -> sendSnailMailToRecipient(messageEntity, recipientEntity);
			default -> {
				recipientEntity.setMessageStatus(MessageStatus.FAILED);
				recipientEntity.setStatusDetail("Unsupported message type: " + recipientEntity.getMessageType());
				yield CompletableFuture.completedFuture(null);
			}
		};

	}

	CompletableFuture<Void> sendSmsToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return supplyAsync(() -> messagingIntegration.sendSms(messageEntity, recipientEntity))
			.thenAccept(messageResult -> updateRecipient(messageResult, recipientEntity))
			.exceptionally(throwable -> {
				recipientEntity.setMessageStatus(MessageStatus.FAILED);
				recipientEntity.setStatusDetail(throwable.getMessage());
				return null;
			});
	}

	void updateRecipient(final MessageResult messageResult, final RecipientEntity recipientEntity) {
		if (messageResult == null) {
			return;
		}
		var messageId = messageResult.getMessageId();
		var deliveryResult = Optional.ofNullable(messageResult.getDeliveries()).stream()
			.flatMap(Collection::stream)
			.findFirst()
			.orElse(null);
		var status = Optional.ofNullable(deliveryResult)
			.map(DeliveryResult::getStatus)
			.orElse(generated.se.sundsvall.messaging.MessageStatus.FAILED);

		recipientEntity.setMessageStatus(MessageStatus.fromValue(status.toString()));
		recipientEntity.setMessagingId(String.valueOf(messageId));
	}

	UserEntity getOrCreateUser(final String userName) {
		return userRepository.findByName(userName)
			.orElseGet(() -> UserEntity.create()
				.withName(userName));
	}

	DepartmentEntity getOrCreateDepartment(final SentBy sentBy) {
		return departmentRepository.findByOrganizationId(sentBy.organizationId)
			.orElseGet(() -> DepartmentEntity.create()
				.withOrganizationId(sentBy.organizationId)
				.withName(sentBy.departmentName));
	}

	SentBy getSentBy(final String municipalityId) {
		var username = Identifier.get().getValue();
		var personData = employeeIntegration.getPortalPersonData(municipalityId, username)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Failed to retrieve employee data for user [%s]".formatted(username)));
		var department = EmployeeUtil.parseOrganizationString(personData.getOrgTree())
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to parse organization from employee data"));

		return new SentBy(username, department.identifier(), department.name());
	}

	record SentBy(String userName, String organizationId, String departmentName) {
	}

}
