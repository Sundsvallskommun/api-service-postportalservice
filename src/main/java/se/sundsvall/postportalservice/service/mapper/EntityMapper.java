package se.sundsvall.postportalservice.service.mapper;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.legalentity.LEAddress;
import generated.se.sundsvall.legalentity.LEPostAddress;
import generated.se.sundsvall.legalentity.LegalEntity2;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.converter.PartyType;

import static java.util.Collections.emptyList;
import static se.sundsvall.postportalservice.Constants.INELIGIBLE_MINOR;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.Constants.UNDELIVERABLE;

@Component
public class EntityMapper {

	private static String joinNonBlank(final String left, final String right) {
		final var hasLeft = left != null && !left.isBlank();
		final var hasRight = right != null && !right.isBlank();
		if (hasLeft && hasRight) {
			return left + " " + right;
		}
		if (hasLeft) {
			return left;
		}
		if (hasRight) {
			return right;
		}
		return null;
	}

	public RecipientEntity toRecipientEntity(final SmsRecipient smsRecipient) {
		return Optional.ofNullable(smsRecipient).map(recipient -> RecipientEntity.create()
			.withMessageType(MessageType.SMS)
			.withStatus(PENDING)
			.withPartyId(recipient.getPartyId())
			.withPhoneNumber(recipient.getPhoneNumber()))
			.orElse(null);
	}

	public RecipientEntity toRecipientEntity(final Recipient recipient) {
		return toRecipientEntity(recipient, PartyType.PRIVATE);
	}

	public RecipientEntity toRecipientEntity(final Recipient recipient, final PartyType partyType) {
		if (recipient == null) {
			return null;
		}
		final var messageType = switch (recipient.getDeliveryMethod()) {
			case DIGITAL_MAIL -> MessageType.DIGITAL_MAIL;
			case SNAIL_MAIL -> MessageType.SNAIL_MAIL;
			default -> null;
		};

		final var recipientEntity = new RecipientEntity();
		final var address = recipient.getAddress();

		if (messageType == MessageType.SNAIL_MAIL && address != null) {
			recipientEntity
				.withStreetAddress(address.getStreet())
				.withApartmentNumber(address.getApartmentNumber())
				.withCareOf(address.getCareOf())
				.withZipCode(address.getZipCode())
				.withCity(address.getCity())
				.withCountry(address.getCountry());
		}

		return recipientEntity
			.withFirstName(Optional.ofNullable(address).map(Address::getFirstName).orElse(null))
			.withLastName(Optional.ofNullable(address).map(Address::getLastName).orElse(null))
			.withMessageType(messageType)
			.withStatus(PENDING)
			.withPartyId(recipient.getPartyId())
			.withPartyType(partyType);
	}

	public RecipientEntity toRecipientEntity(final Address address) {
		return Optional.ofNullable(address).map(address1 -> RecipientEntity.create()
			.withCountry(address1.getCountry())
			.withCity(address1.getCity())
			.withStreetAddress(address1.getStreet())
			.withZipCode(address1.getZipCode())
			.withFirstName(address1.getFirstName())
			.withLastName(address1.getLastName())
			.withApartmentNumber(address1.getApartmentNumber())
			.withCareOf(address1.getCareOf())
			.withMessageType(MessageType.SNAIL_MAIL)
			.withStatus(PENDING))
			.orElse(null);

	}

	public RecipientEntity toDigitalMailRecipientEntity(final String partyId, final CitizenExtended citizenExtended) {
		return Optional.ofNullable(partyId).map(id -> RecipientEntity.create()
			.withFirstName(Optional.ofNullable(citizenExtended).map(CitizenExtended::getGivenname).orElse(null))
			.withLastName(Optional.ofNullable(citizenExtended).map(CitizenExtended::getLastname).orElse(null))
			.withPartyId(id)
			.withPartyType(PartyType.PRIVATE)
			.withMessageType(MessageType.DIGITAL_MAIL)
			.withStatus(PENDING))
			.orElse(null);
	}

	public RecipientEntity toEnterpriseDigitalMailRecipientEntity(final String partyId) {
		return Optional.ofNullable(partyId).map(id -> RecipientEntity.create()
			.withPartyId(id)
			.withPartyType(PartyType.ENTERPRISE)
			.withMessageType(MessageType.DIGITAL_MAIL)
			.withStatus(PENDING))
			.orElse(null);
	}

	public RecipientEntity toEnterpriseUndeliverableRecipientEntity(final String partyId, final String reason) {
		return Optional.ofNullable(partyId).map(id -> RecipientEntity.create()
			.withPartyId(id)
			.withPartyType(PartyType.ENTERPRISE)
			.withMessageType(MessageType.LETTER)
			.withStatus(UNDELIVERABLE)
			.withStatusDetail(reason))
			.orElse(null);
	}

	public RecipientEntity toEnterpriseSnailMailRecipientEntity(final String partyId, final LegalEntity2 legalEntity) {
		if (partyId == null || legalEntity == null) {
			return null;
		}

		final var address = legalEntity.getAddress();
		final var postAddress = legalEntity.getPostAddress();

		final var streetAddress = Optional.ofNullable(address)
			.map(addr -> joinNonBlank(addr.getAddressArea(), addr.getAdressNumber()))
			.orElse(null);
		final var zipCode = Optional.ofNullable(address).map(LEAddress::getPostalCode).orElse(null);
		final var city = Optional.ofNullable(address).map(LEAddress::getCity).orElse(null);

		if ((streetAddress == null || streetAddress.isBlank()) && (zipCode == null || zipCode.isBlank())) {
			return null;
		}

		return RecipientEntity.create()
			.withPartyId(partyId)
			.withPartyType(PartyType.ENTERPRISE)
			.withFirstName(legalEntity.getName())
			.withMessageType(MessageType.SNAIL_MAIL)
			.withStreetAddress(streetAddress)
			.withCareOf(Optional.ofNullable(postAddress).map(LEPostAddress::getCoAdress).orElse(null))
			.withZipCode(zipCode)
			.withCity(city)
			.withCountry(Optional.ofNullable(postAddress).map(LEPostAddress::getCountry).orElse(null))
			.withStatus(PENDING);
	}

	public RecipientEntity toSnailMailRecipientEntity(final CitizenExtended citizenExtended) {
		final var address = Optional.ofNullable(citizenExtended)
			.map(CitizenExtended::getAddresses)
			.orElse(emptyList())
			.stream()
			.filter(citizenAddress -> "POPULATION_REGISTRATION_ADDRESS".equals(citizenAddress.getAddressType()))
			.findFirst()
			.orElse(null);

		if (address == null) {
			// If no valid address is found, we return null. We don't want to create a RecipientEntity without an address.
			// The faulty citizenExtended will be handled by a separate method.
			return null;
		}

		return RecipientEntity.create()
			.withPartyId(Optional.ofNullable(citizenExtended.getPersonId()).map(UUID::toString).orElse(null))
			.withPartyType(PartyType.PRIVATE)
			.withFirstName(citizenExtended.getGivenname())
			.withLastName(citizenExtended.getLastname())
			.withMessageType(MessageType.SNAIL_MAIL)
			.withStreetAddress(address.getAddress())
			.withApartmentNumber(address.getAppartmentNumber())
			.withCareOf(address.getCo())
			.withZipCode(address.getPostalCode())
			.withCity(address.getCity())
			.withCountry(address.getCountry())
			.withStatus(PENDING);
	}

	public RecipientEntity toUndeliverableRecipientEntity(final CitizenExtended citizenExtended) {
		return createBaseRecipientEntity(citizenExtended, UNDELIVERABLE);
	}

	public RecipientEntity toIneligibleMinorRecipientEntity(final CitizenExtended citizenExtended) {
		return createBaseRecipientEntity(citizenExtended, INELIGIBLE_MINOR);
	}

	private RecipientEntity createBaseRecipientEntity(final CitizenExtended citizenExtended, final String status) {
		return Optional.ofNullable(citizenExtended).map(citizen -> RecipientEntity.create()
			.withPartyId(Optional.ofNullable(citizen.getPersonId()).map(UUID::toString).orElse(null))
			.withPartyType(PartyType.PRIVATE)
			.withMessageType(MessageType.LETTER)
			.withStatus(status))
			.orElse(null);
	}
}
