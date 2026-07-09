package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.esigning.StartSigningResponse;
import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.ESigningRequest;
import se.sundsvall.postportalservice.api.model.LetterCsvRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsCsvRequest;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.SigningEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.converter.PartyType;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.esigning.EsigningIntegration;
import se.sundsvall.postportalservice.integration.esigning.EsigningMapper;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.integration.party.PartyIntegration;
import se.sundsvall.postportalservice.service.mapper.AttachmentMapper;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.configuration.DeliveryExecutorConfiguration.DELIVERY_EXECUTOR;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.E_SIGNING;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SMS;
import static se.sundsvall.postportalservice.service.util.CsvUtil.parseLetterCsv;
import static se.sundsvall.postportalservice.service.util.CsvUtil.validateSmsCsv;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.FOLDER_NAME;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SMS_SENDER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_METHOD;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_METHOD_VALUE;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SUPPORT_TEXT;

@Service
public class MessageService {

	private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

	private final ThreadPoolTaskExecutor deliveryExecutor;

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
	private final PartyIntegration partyIntegration;
	private final EsigningIntegration esigningIntegration;
	private final EsigningMapper esigningMapper;
	private final SigningRepository signingRepository;

	public MessageService(
		@Qualifier(DELIVERY_EXECUTOR) final ThreadPoolTaskExecutor deliveryExecutor,
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final MessagingIntegration messagingIntegration,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final PrecheckService precheckService,
		final AttachmentMapper attachmentMapper,
		final EntityMapper entityMapper,
		final DepartmentRepository departmentRepository,
		final UserRepository userRepository,
		final MessageRepository messageRepository,
		final RecipientRepository recipientRepository,
		final CitizenIntegration citizenIntegration,
		final PartyIntegration partyIntegration,
		final EsigningIntegration esigningIntegration,
		final EsigningMapper esigningMapper,
		final SigningRepository signingRepository) {
		this.deliveryExecutor = deliveryExecutor;
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
		this.partyIntegration = partyIntegration;
		this.esigningIntegration = esigningIntegration;
		this.esigningMapper = esigningMapper;
		this.signingRepository = signingRepository;
	}

	public String processDigitalRegisteredLetterRequest(final String municipalityId, final DigitalRegisteredLetterRequest request, final List<MultipartFile> attachments) {
		final var partyId = request.getPartyId();
		final var partyType = partyIntegration.getPartyTypes(municipalityId, List.of(partyId)).get(partyId);
		if (partyType == PartyType.ENTERPRISE) {
			throw Problem.valueOf(BAD_REQUEST, "Digital registered letters are not supported for enterprise recipients");
		}

		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var message = createMessage(municipalityId, settingsMap, DIGITAL_REGISTERED_LETTER, request.getSubject(), request.getBody(), request.getContentType());

		final var citizens = citizenIntegration.getCitizens(municipalityId, List.of(partyId)).getFirst();

		final var recipient = RecipientEntity.create()
			.withFirstName(citizens.getGivenname())
			.withLastName(citizens.getLastname())
			.withPartyId(partyId)
			.withStatus(PENDING)
			.withMessageType(DIGITAL_REGISTERED_LETTER);
		message.setRecipients(List.of(recipient));

		final var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);
		message.setAttachments(attachmentEntities);

		digitalRegisteredLetterIntegration.sendLetter(message, recipient);

		messageRepository.save(message);
		return message.getId();
	}

	/**
	 * Starts a Comfact e-signing case. Persists the message with one recipient per signatory (type {@code E_SIGNING},
	 * status {@code PENDING}) and the uploaded document, then calls api-service-e-signing with the message id as
	 * {@code customerReference} so every event callback can be correlated back to this message. The provider case is
	 * recorded in a {@link SigningEntity}. The whole method is transactional - if the provider call fails nothing is
	 * persisted, so no dangling case is left behind.
	 */
	@Transactional
	public String processESigningRequest(final String municipalityId, final ESigningRequest request, final MultipartFile document, final List<MultipartFile> attachments) {
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var message = createMessage(municipalityId, settingsMap, E_SIGNING, request.getSubject(), request.getBody(), TEXT_PLAIN_VALUE);

		final var recipients = request.getSignatories().stream()
			.map(signatory -> RecipientEntity.create()
				.withPartyId(signatory.getPartyId())
				.withEmail(signatory.getEmail())
				.withFirstName(signatory.getName())
				.withStatus(PENDING)
				.withMessageType(E_SIGNING))
			.toList();
		message.setRecipients(recipients);

		// Store the primary document first, followed by any attachments, all as message attachments.
		final var files = new ArrayList<MultipartFile>();
		files.add(document);
		files.addAll(ofNullable(attachments).orElseGet(List::of));
		final var attachmentEntities = attachmentMapper.toAttachmentEntities(files);
		message.setAttachments(attachmentEntities);

		messageRepository.save(message);

		final var documentEntity = attachmentEntities.getFirst();
		final var attachmentEntitiesToSign = attachmentEntities.subList(1, attachmentEntities.size());
		final var startSigningRequest = esigningMapper.toStartSigningRequest(message, request, documentEntity, attachmentEntitiesToSign);
		final var response = esigningIntegration.createSigning(municipalityId, startSigningRequest);

		// The signing's attachment is left null; it is set to the signed (merged) document when the completion event arrives.
		final var signing = SigningEntity.create()
			.withMessage(message)
			.withProviderCaseId(response.getProviderCaseId())
			.withProvider(response.getProvider())
			.withStatus(ofNullable(response.getStatus()).map(StartSigningResponse.StatusEnum::getValue).orElse(null));
		signingRepository.save(signing);

		return message.getId();
	}

	public String processCsvLetterRequest(final String municipalityId, final LetterCsvRequest request, final MultipartFile csvFile, final List<MultipartFile> attachments) {
		final var parsed = parseLetterCsv(csvFile);

		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var message = createMessage(municipalityId, settingsMap, LETTER, request.getSubject(), request.getBody(), request.getContentType());

		final var privateIds = new ArrayList<>(parsed.privateIds().keySet());
		final var enterpriseIds = new ArrayList<>(parsed.enterpriseIds().keySet());
		final var recipientEntities = precheckService.precheckLegalIds(municipalityId, privateIds, enterpriseIds);
		message.setRecipients(recipientEntities);
		final var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);
		message.setAttachments(attachmentEntities);

		messageRepository.save(message);

		processRecipients(message, settingsMap);
		return message.getId();
	}

	public String processCsvSmsRequest(final String municipalityId, final SmsCsvRequest request, final MultipartFile csvFile) {
		final var validationResult = validateSmsCsv(csvFile);
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var message = createMessage(municipalityId, settingsMap, SMS, null, request.getMessage(), null);

		final var recipientEntities = validationResult.validEntries().keySet().stream()
			.map(phoneNumber -> RecipientEntity.create()
				.withPhoneNumber(phoneNumber)
				.withMessageType(SMS)
				.withStatus(PENDING))
			.toList();
		message.setRecipients(recipientEntities);

		messageRepository.save(message);

		processRecipients(message, settingsMap);
		return message.getId();
	}

	public String processLetterRequest(final String municipalityId, final LetterRequest letterRequest, final List<MultipartFile> attachments) {
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var message = createMessage(municipalityId, settingsMap, LETTER, letterRequest.getSubject(), letterRequest.getBody(), letterRequest.getContentType());

		final var letterRecipients = ofNullable(letterRequest.getRecipients()).orElse(emptyList());
		final var partyIds = letterRecipients.stream()
			.map(Recipient::getPartyId)
			.filter(Objects::nonNull)
			.toList();
		final var partyTypes = partyIntegration.getPartyTypes(municipalityId, partyIds);

		final var recipientEntities = letterRecipients.stream()
			.map(recipient -> entityMapper.toRecipientEntity(recipient, partyTypes.getOrDefault(recipient.getPartyId(), PartyType.PRIVATE)))
			.filter(Objects::nonNull);

		final var addressRecipients = ofNullable(letterRequest.getAddresses()).orElse(emptyList()).stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull);

		final var recipients = Stream.concat(recipientEntities, addressRecipients).toList();
		message.setRecipients(recipients);

		final var attachmentEntities = attachmentMapper.toAttachmentEntities(attachments);
		message.setAttachments(attachmentEntities);

		messageRepository.save(message);

		processRecipients(message, settingsMap);
		return message.getId();
	}

	/**
	 * Maps an incoming SmsRequest to a MessageEntity. Persists the MessageEntity and its associated entities to the
	 * database. Sends a message to each recipient asynchronously. Returns the MessageEntity ID that can be used to read the
	 * message.
	 */
	public String processSmsRequest(final String municipalityId, final SmsRequest smsRequest) {
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var message = createMessage(municipalityId, settingsMap, SMS, null, smsRequest.getMessage(), null);

		final var recipients = smsRequest.getRecipients().stream()
			.map(entityMapper::toRecipientEntity)
			.filter(Objects::nonNull)
			.toList();
		message.setRecipients(recipients);

		messageRepository.save(message);

		processRecipients(message, settingsMap);
		return message.getId();
	}

	void processRecipients(final MessageEntity messageEntity, final Map<String, String> settingsMap) {
		LOG.info("Starting to process recipients for message with id {}", messageEntity.getId());
		ofNullable(messageEntity.getRecipients()).orElse(emptyList()).stream()
			.filter(recipientEntity -> !"UNDELIVERABLE".equalsIgnoreCase(recipientEntity.getStatus()))
			.forEach(recipientEntity -> deliveryExecutor.execute(() -> deliver(messageEntity, recipientEntity, settingsMap)));
	}

	/**
	 * Delivers a message to a single recipient. Runs on a {@link #deliveryExecutor} thread - the messaging call blocks
	 * that thread, so the pool size is the concurrency limit. On success the recipient status is updated from the
	 * messaging result; on failure the recipient is marked as FAILED.
	 */
	void deliver(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		try {
			final var messageResult = switch (recipientEntity.getMessageType()) {
				case SMS -> messagingIntegration.sendSms(messageEntity, recipientEntity);
				case DIGITAL_MAIL -> messagingIntegration.sendDigitalMail(messageEntity, recipientEntity).getMessages().getFirst();
				case SNAIL_MAIL -> deliverSnailMailOrCallback(messageEntity, recipientEntity, settingsMap);
				default -> {
					LOG.error("Unsupported message type: {}, for recipient with id: {}", recipientEntity.getMessageType(), recipientEntity.getId());
					recipientEntity.setStatus(FAILED);
					recipientEntity.setStatusDetail("Unsupported message type: " + recipientEntity.getMessageType());
					recipientRepository.save(recipientEntity);
					yield null;
				}
			};
			updateRecipient(messageResult, recipientEntity);
		} catch (final Exception e) {
			LOG.error("Failed to deliver to recipient with id {}", recipientEntity.getId(), e);
			recipientEntity.setStatus(FAILED);
			recipientEntity.setStatusDetail(e.getMessage());
			recipientRepository.save(recipientEntity);
		}
	}

	MessageResult deliverSnailMailOrCallback(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		LOG.info("Sending snail mail to recipient with id {}", recipientEntity.getId());

		// If callback email is configured, send as email instead of snail mail.
		if (SNAILMAIL_METHOD_VALUE.equals(settingsMap.get(SNAILMAIL_METHOD))) {
			LOG.info("Snail mail method is set to {}, sending callback email instead.", SNAILMAIL_METHOD_VALUE);
			return messagingIntegration.sendCallbackEmail(messageEntity, recipientEntity, settingsMap);
		}

		return messagingIntegration.sendSnailMail(messageEntity, recipientEntity);
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
			.orElse(MessageStatus.FAILED);

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

	/**
	 * Creates a message with the user, department, municipality and display name resolved from the messaging settings,
	 * and the content fields populated. For body-only types such as SMS, pass {@code null} for subject and content type.
	 */
	MessageEntity createMessage(final String municipalityId, final Map<String, String> settingsMap, final MessageType messageType, final String subject, final String body, final String contentType) {
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
			.withDepartment(department)
			.withMessageType(messageType)
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType);
	}
}
