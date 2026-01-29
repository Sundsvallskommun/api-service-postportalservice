package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SMS;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SNAIL_MAIL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.FOLDER_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SMS_SENDER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SUPPORT_TEXT;
import static se.sundsvall.postportalservice.service.util.CsvUtil.parseCsvToLegalIds;
import static se.sundsvall.postportalservice.service.util.SemaphoreUtil.withPermit;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterCsvRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.AttachmentMapper;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.util.RecipientId;

@Service
public class MessageService {

	private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

	private final ExecutorService executor = Executors.newFixedThreadPool(64);
	private final Semaphore permits = new Semaphore(32);

	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;
	private final MessagingIntegration messagingIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final PrecheckService precheckService;

	private final AttachmentMapper attachmentMapper;
	private final EntityMapper entityMapper;

	private final DepartmentRepository departmentRepository;
	private final UserRepository userRepository;
	private final MessageRepository messageRepository;
	private final RecipientRepository recipientRepository;
	private final CitizenIntegration citizenIntegration;

	public MessageService(
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final MessagingIntegration messagingIntegration,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final PrecheckService precheckService,
		final AttachmentMapper attachmentMapper,
		final EntityMapper entityMapper,
		final DepartmentRepository departmentRepository,
		final UserRepository userRepository,
		final MessageRepository messageRepository,
		final RecipientRepository recipientRepository, final CitizenIntegration citizenIntegration) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
		this.messagingIntegration = messagingIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.precheckService = precheckService;
		this.attachmentMapper = attachmentMapper;
		this.entityMapper = entityMapper;
		this.departmentRepository = departmentRepository;
		this.userRepository = userRepository;
		this.messageRepository = messageRepository;
		this.recipientRepository = recipientRepository;
		this.citizenIntegration = citizenIntegration;
	}

	public String processDigitalRegisteredLetterRequest(final String municipalityId, final DigitalRegisteredLetterRequest request, final List<MultipartFile> attachments) {
		final var message = createMessageEntity(municipalityId);
		message.setBody(request.getBody());
		message.setContentType(request.getContentType());
		message.setSubject(request.getSubject());
		message.setMessageType(DIGITAL_REGISTERED_LETTER);

		final var citizens = citizenIntegration.getCitizens(municipalityId, List.of(request.getPartyId())).getFirst();

		final var recipient = RecipientEntity.create()
			.withFirstName(citizens.getGivenname())
			.withLastName(citizens.getLastname())
			.withPartyId(request.getPartyId())
			.withStatus(PENDING)
			.withMessageType(DIGITAL_REGISTERED_LETTER);
		message.setRecipients(List.of(recipient));

		final var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);
		message.setAttachments(attachmentEntities);

		digitalRegisteredLetterIntegration.sendLetter(message, recipient);

		messageRepository.save(message);
		return message.getId();
	}

	public String processCsvLetterRequest(final String municipalityId, final LetterCsvRequest request, final MultipartFile csvFile, final List<MultipartFile> attachments) {
		final var legalIds = parseCsvToLegalIds(csvFile);
		final var message = createMessageEntity(municipalityId);
		message.setSubject(request.getSubject());
		message.setContentType(request.getContentType());
		message.setBody(request.getBody());
		message.setMessageType(LETTER);

		final var recipientEntities = precheckService.precheckLegalIds(municipalityId, new ArrayList<>(legalIds));
		message.setRecipients(recipientEntities);
		final var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);
		message.setAttachments(attachmentEntities);

		messageRepository.save(message);

		processRecipients(message);
		return message.getId();
	}

	public String processLetterRequest(final String municipalityId, final LetterRequest letterRequest, final List<MultipartFile> attachments) {
		final var message = createMessageEntity(municipalityId);
		message.setBody(letterRequest.getBody());
		message.setContentType(letterRequest.getContentType());
		message.setSubject(letterRequest.getSubject());
		message.setMessageType(LETTER);

		final var recipientEntities = ofNullable(letterRequest.getRecipients()).orElse(emptyList()).stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull);

		final var addressRecipients = ofNullable(letterRequest.getAddresses()).orElse(emptyList()).stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull);

		final var recipients = Stream.concat(recipientEntities, addressRecipients).toList();
		message.setRecipients(recipients);

		final var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);
		message.setAttachments(attachmentEntities);

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
		final var message = createMessageEntity(municipalityId);
		message.setBody(smsRequest.getMessage());
		message.setMessageType(SMS);

		final var recipients = smsRequest.getRecipients().stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull)
			.toList();
		message.setRecipients(recipients);

		messageRepository.save(message);

		processRecipients(message);
		return message.getId();
	}

	CompletableFuture<Void> processRecipients(final MessageEntity messageEntity) {
		LOG.info("Starting to process recipients for message with id {}", messageEntity.getId());
		final var futures = ofNullable(messageEntity.getRecipients()).orElse(emptyList()).stream()
			.filter(recipientEntity -> !"UNDELIVERABLE".equalsIgnoreCase(recipientEntity.getStatus()))
			.map(recipientEntity -> withPermit(() -> sendMessageToRecipient(messageEntity, recipientEntity), permits, executor))
			.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures)
			.handle((_, throwable) -> {
				final var triggerSnailmailBatch = messageEntity.getRecipients().stream()
					.anyMatch(recipientEntity -> recipientEntity.getMessageType() == SNAIL_MAIL);
				if (triggerSnailmailBatch) {
					LOG.info("Triggering snail mail batch processing for message with id {}", messageEntity.getId());
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
				LOG.error("Unsupported message type: {}, for recipient with id: {}", recipientEntity.getMessageType(), recipientEntity.getId());
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail("Unsupported message type: " + recipientEntity.getMessageType());
				yield CompletableFuture.completedFuture(null);
			}
		};
	}

	CompletableFuture<Void> sendSmsToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		LOG.info("Sending SMS to recipient with id {}", recipientEntity.getId());
		final var contextMap = MDC.getCopyOfContextMap();
		return supplyAsync(() -> {
			MDC.setContextMap(contextMap);
			return messagingIntegration.sendSms(messageEntity, recipientEntity);
		})
			.thenAccept(messageResult -> {
				MDC.setContextMap(contextMap);
				LOG.info("SMS sent to recipient with id {}", recipientEntity.getId());
				updateRecipient(messageResult, recipientEntity);
				RecipientId.reset();
			})
			.exceptionally(throwable -> {
				LOG.error("Failed to send SMS to recipient with id {}", recipientEntity.getId(), throwable);
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail(throwable.getMessage());
				recipientRepository.save(recipientEntity);
				RecipientId.reset();
				return null;
			});
	}

	CompletableFuture<Void> sendDigitalMailToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		LOG.info("Sending digital mail to recipient with id {}", recipientEntity.getId());
		final var contextMap = MDC.getCopyOfContextMap();
		return supplyAsync(() -> {
			MDC.setContextMap(contextMap);
			return messagingIntegration.sendDigitalMail(messageEntity, recipientEntity);
		})
			.thenAccept(messageBatchResult -> {
				MDC.setContextMap(contextMap);
				LOG.info("Digital mail sent to recipient with id {}", recipientEntity.getId());
				final var messageResult = messageBatchResult.getMessages().getFirst();
				updateRecipient(messageResult, recipientEntity);
				RecipientId.reset();
			})
			.exceptionally(throwable -> {
				LOG.error("Failed to send digital mail to recipient with id {}", recipientEntity.getId(), throwable);
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail(throwable.getMessage());
				recipientRepository.save(recipientEntity);
				RecipientId.reset();
				return null;
			});
	}

	CompletableFuture<Void> sendSnailMailToRecipient(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		LOG.info("Sending snail mail to recipient with id {}", recipientEntity.getId());
		final var contextMap = MDC.getCopyOfContextMap();
		return supplyAsync(() -> {
			MDC.setContextMap(contextMap);
			return messagingIntegration.sendSnailMail(messageEntity, recipientEntity);
		})
			.thenAccept(messageResult -> {
				MDC.setContextMap(contextMap);
				LOG.info("Snail mail sent to recipient with id {}", recipientEntity.getId());
				updateRecipient(messageResult, recipientEntity);
				RecipientId.reset();
			})
			.exceptionally(throwable -> {
				LOG.error("Failed to send snail mail to recipient with id {}", recipientEntity.getId(), throwable);
				recipientEntity.setStatus(FAILED);
				recipientEntity.setStatusDetail(throwable.getMessage());
				recipientRepository.save(recipientEntity);
				RecipientId.reset();
				return null;
			});
	}

	void updateRecipient(final MessageResult messageResult, final RecipientEntity recipientEntity) {
		if (messageResult == null) {
			return;
		}
		final var messageId = messageResult.getMessageId();
		final var deliveryResult = ofNullable(messageResult.getDeliveries()).stream()
			.flatMap(Collection::stream)
			.findFirst()
			.orElse(null);
		final var status = ofNullable(deliveryResult)
			.map(DeliveryResult::getStatus)
			.orElse(generated.se.sundsvall.messaging.MessageStatus.FAILED);

		LOG.info("Updating recipient with id {}, Status: {}, ExternalId: {}", recipientEntity.getId(), status, messageId);
		recipientEntity.setStatus(status.toString());
		recipientEntity.setExternalId(String.valueOf(messageId));
		recipientRepository.save(recipientEntity);
	}

	UserEntity getOrCreateUser(final String userName) {
		return userRepository.findByUsernameIgnoreCase(userName)
			.orElseGet(() -> UserEntity.create()
				.withUsername(userName));
	}

	DepartmentEntity getOrCreateDepartment(final Map<String, String> settingsMap) {
		return departmentRepository.findByOrganizationId(settingsMap.get(DEPARTMENT_ID))
			.orElseGet(() -> DepartmentEntity.create()
				.withOrganizationId(settingsMap.get(DEPARTMENT_ID))
				.withName(settingsMap.get(DEPARTMENT_NAME)));
	}

	private MessageEntity createMessageEntity(final String municipalityId) {
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);

		final var user = getOrCreateUser(Identifier.get().getValue());
		final var department = getOrCreateDepartment(settingsMap)
			.withFolderName(settingsMap.get(FOLDER_NAME))
			.withOrganizationNumber(settingsMap.get(ORGANIZATION_NUMBER))
			.withSupportText(settingsMap.get(SUPPORT_TEXT))
			.withContactInformationUrl(settingsMap.get(CONTACT_INFORMATION_URL))
			.withContactInformationEmail(settingsMap.get(CONTACT_INFORMATION_EMAIL))
			.withContactInformationPhoneNumber(settingsMap.get(CONTACT_INFORMATION_PHONE_NUMBER));

		return MessageEntity.create()
			.withMunicipalityId(municipalityId)
			.withDisplayName(settingsMap.get(SMS_SENDER))
			.withUser(user)
			.withDepartment(department);
	}
}
