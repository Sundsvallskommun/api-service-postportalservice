package se.sundsvall.postportalservice.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.validation.ValidRecipient;

@ExtendWith(MockitoExtension.class)
class ValidRecipientConstraintValidatorTest {

	private final String validPartyId = "61ce033b-6f0f-49eb-9e51-3efa964bd0d7";
	private final Address validAddress = Address.create()
		.withFirstName("John")
		.withLastName("Doe")
		.withCity("Sundsvall")
		.withCountry("Sweden")
		.withStreet("Storgatan 1")
		.withZipCode("85750")
		.withCareOf("c/o Jane Doe")
		.withApartmentNumber("1101");

	@Mock
	private ValidRecipient mockAnnotation;

	@Mock
	private ConstraintValidatorContext context;

	@Mock
	private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

	@Mock
	private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeContext;

	@InjectMocks
	private ValidRecipientConstraintValidator validator;

	@BeforeEach
	void beforeEach() {
		validator.initialize(mockAnnotation);
	}

	@Test
	void validateDigitalMailRecipient() {
		var recipient = Recipient.create()
			.withAddress(null)
			.withPartyId(validPartyId)
			.withDeliveryMethod(Recipient.DeliveryMethod.DIGITAL_MAIL);

		var valid = validator.isValid(recipient, context);

		assertThat(valid).isTrue();
	}

	@Test
	void validateNoContactRecipient() {
		var recipient = Recipient.create()
			.withAddress(null)
			.withPartyId(validPartyId)
			.withDeliveryMethod(Recipient.DeliveryMethod.DELIVERY_NOT_POSSIBLE);

		var valid = validator.isValid(recipient, context);

		assertThat(valid).isTrue();
	}

	@Test
	void validateSnailMailRecipient_validAddress() {
		var recipient = Recipient.create()
			.withAddress(validAddress)
			.withPartyId(validPartyId)
			.withDeliveryMethod(Recipient.DeliveryMethod.SNAIL_MAIL);

		var valid = validator.isValid(recipient, context);

		assertThat(valid).isTrue();
	}

	@Test
	void validateSnailMailRecipient_invalidAddress() {
		when(context.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);
		when(violationBuilder.addPropertyNode(any())).thenReturn(nodeContext);

		var recipient = Recipient.create()
			.withAddress(null)
			.withPartyId(validPartyId)
			.withDeliveryMethod(Recipient.DeliveryMethod.SNAIL_MAIL);

		var valid = validator.isValid(recipient, context);

		assertThat(valid).isFalse();

		verify(context).buildConstraintViolationWithTemplate("must not be null");
		verify(violationBuilder).addPropertyNode("address");
		verify(nodeContext).addConstraintViolation();
	}

}
