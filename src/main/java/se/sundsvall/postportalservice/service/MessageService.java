package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SMS;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SNAIL_MAIL;
import static se.sundsvall.postportalservice.service.util.SemaphoreUtil.withPermit;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageResult;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.api.model.Attachments;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.employee.EmployeeIntegration;
import se.sundsvall.postportalservice.integration.employee.EmployeeUtil;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.AttachmentMapper;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;

@Service
public class MessageService {

	private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

	private final ExecutorService executor = Executors.newFixedThreadPool(64);
	private final Semaphore permits = new Semaphore(32);

	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;
	private final MessagingIntegration messagingIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final EmployeeIntegration employeeIntegration;

	private final AttachmentMapper attachmentMapper;
	private final EntityMapper entityMapper;

	private final DepartmentRepository departmentRepository;
	private final UserRepository userRepository;
	private final MessageRepository messageRepository;

	public MessageService(
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final MessagingIntegration messagingIntegration,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final EmployeeIntegration employeeIntegration,
		final AttachmentMapper attachmentMapper,
		final EntityMapper entityMapper,
		final DepartmentRepository departmentRepository,
		final UserRepository userRepository,
		final MessageRepository messageRepository) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
		this.messagingIntegration = messagingIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.employeeIntegration = employeeIntegration;
		this.attachmentMapper = attachmentMapper;
		this.entityMapper = entityMapper;
		this.departmentRepository = departmentRepository;
		this.userRepository = userRepository;
		this.messageRepository = messageRepository;
	}

	public String processDigitalRegisteredLetterRequest(final String municipalityId, final DigitalRegisteredLetterRequest request, final Attachments attachments) {
		var sentBy = getSentBy(municipalityId);

		var senderInfo = messagingSettingsIntegration.getSenderInfo(municipalityId, sentBy.organizationId);

		var user = getOrCreateUser(sentBy.userName);
		var department = getOrCreateDepartment(sentBy)
			.withOrganizationNumber(senderInfo.getOrganizationNumber())
			.withSupportText(senderInfo.getSupportText())
			.withContactInformationUrl(senderInfo.getContactInformationUrl())
			.withContactInformationEmail(senderInfo.getContactInformationEmail())
			.withContactInformationPhoneNumber(senderInfo.getContactInformationPhoneNumber());

		var recipient = new RecipientEntity()
			.withPartyId(request.getPartyId())
			.withStatus(PENDING)
			.withMessageType(DIGITAL_REGISTERED_LETTER);

		var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);

		var message = MessageEntity.create()
			.withMunicipalityId(municipalityId)
			.withUser(user)
			.withDepartment(department)
			.withRecipients(List.of(recipient))
			.withAttachments(attachmentEntities)
			.withBody(request.getBody())
			.withContentType(request.getContentType())
			.withSubject(request.getSubject())
			.withMessageType(DIGITAL_REGISTERED_LETTER);

		digitalRegisteredLetterIntegration.sendLetter(message, recipient);

		messageRepository.save(message);

		return message.getId();
	}

	public String processLetterRequest(final String municipalityId, final LetterRequest letterRequest, final Attachments attachments) {
		var sentBy = getSentBy(municipalityId);

		var senderInfo = messagingSettingsIntegration.getSenderInfo(municipalityId, sentBy.organizationId);

		var user = getOrCreateUser(sentBy.userName);
		var department = getOrCreateDepartment(sentBy)
			.withOrganizationNumber(senderInfo.getOrganizationNumber())
			.withSupportText(senderInfo.getSupportText())
			.withContactInformationUrl(senderInfo.getContactInformationUrl())
			.withContactInformationEmail(senderInfo.getContactInformationEmail())
			.withContactInformationPhoneNumber(senderInfo.getContactInformationPhoneNumber());

		var recipientEntities = Optional.ofNullable(letterRequest.getRecipients()).orElse(emptyList()).stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull);

		var addressRecipients = Optional.ofNullable(letterRequest.getAddresses()).orElse(emptyList()).stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull);

		var recipients = Stream.concat(recipientEntities, addressRecipients).toList();

		var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);

		var message = MessageEntity.create()
			.withMunicipalityId(municipalityId)
			.withUser(user)
			.withDepartment(department)
			.withRecipients(recipients)
			.withAttachments(attachmentEntities)
			.withBody(letterRequest.getBody())
			.withContentType(letterRequest.getContentType())
			.withSubject(letterRequest.getSubject())
			.withMessageType(LETTER);

		messageRepository.save(message);

		processRecipients(message);
		return message.getId();
	}

	/**
	 * Maps an incoming SmsRequest to a MessageEntity. Persists the MessageEntity and its associated entities to the
	 * database. Sends a message to each recipient asynchronously. Returns the MessageEntity ID that can be used to read the
	 * message.
	 */
	public String processSmsRequest(final String municipalityId, final SmsRequest smsRequest) {
		var sentBy = getSentBy(municipalityId);

		var senderInfo = messagingSettingsIntegration.getSenderInfo(municipalityId, sentBy.organizationId());

		var user = getOrCreateUser(sentBy.userName);
		var department = getOrCreateDepartment(sentBy)
			.withOrganizationNumber(senderInfo.getOrganizationNumber())
			.withSupportText(senderInfo.getSupportText())
			.withContactInformationUrl(senderInfo.getContactInformationUrl())
			.withContactInformationEmail(senderInfo.getContactInformationEmail())
			.withContactInformationPhoneNumber(senderInfo.getContactInformationPhoneNumber());

		var recipients = smsRequest.getRecipients().stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull)
			.toList();

		var message = MessageEntity.create()
			.withDisplayName(senderInfo.getSmsSender())
			.withMunicipalityId(municipalityId)
			.withUser(user)
			.withDepartment(department)
			.withRecipients(recipients)
			.withBody(smsRequest.getMessage())
			.withMessageType(SMS);

		messageRepository.save(message);

		processRecipients(message);
		return message.getId();
	}

	CompletableFuture<Void> processRecipients(final MessageEntity messageEntity) {
		var futures = Optional.ofNullable(messageEntity.getRecipients()).orElse(emptyList()).stream()
			.map(recipientEntity -> withPermit(() -> sendMessageToRecipient(messageEntity, recipientEntity), permits, executor))
			.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures)
			.handle((v, throwable) -> {
				var triggerSnailmailBatch = messageEntity.getRecipients().stream()
					.anyMatch(recipientEntity -> recipientEntity.getMessageType() == SNAIL_MAIL);
				if (triggerSnailmailBatch) {
					messagingIntegration.triggerSnailMailBatchProcessing(messageEntity.getMunicipalityId(), messageEntity.getId());
				}
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
			case DIGITAL_MAIL -> sendDigitalMailToRecipient(messageEntity, recipientEntity);
			case SNAIL_MAIL -> sendSnailMailToRecipient(messageEntity, recipientEntity);
			default -> {
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail("Unsupported message type: " + recipientEntity.getMessageType());
				yield CompletableFuture.completedFuture(null);
			}
		};
	}

	CompletableFuture<Void> sendSmsToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return supplyAsync(() -> messagingIntegration.sendSms(messageEntity, recipientEntity))
			.thenAccept(messageResult -> updateRecipient(messageResult, recipientEntity))
			.exceptionally(throwable -> {
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail(throwable.getMessage());
				return null;
			});
	}

	CompletableFuture<Void> sendDigitalMailToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return supplyAsync(() -> messagingIntegration.sendDigitalMail(messageEntity, recipientEntity))
			.thenAccept(messageBatchResult -> {
				var messageResult = messageBatchResult.getMessages().getFirst();
				updateRecipient(messageResult, recipientEntity);
			})
			.exceptionally(throwable -> {
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail(throwable.getMessage());
				return null;
			});
	}

	CompletableFuture<Void> sendSnailMailToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return supplyAsync(() -> messagingIntegration.sendSnailMail(messageEntity, recipientEntity))
			.thenAccept(messageResult -> updateRecipient(messageResult, recipientEntity))
			.exceptionally(throwable -> {
				recipientEntity.setStatus(FAILED);
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

		recipientEntity.setStatus(status.toString());
		recipientEntity.setExternalId(String.valueOf(messageId));
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
