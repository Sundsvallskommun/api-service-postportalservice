package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.postportalservice.integration.db.converter.MessageStatus;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

@Entity
@Table(name = "recipient", indexes = {
	@Index(name = "IDX_RECIPIENT_MESSAGE_ID", columnList = "message_id")
})
public class RecipientEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "party_id", columnDefinition = "VARCHAR(36)")
	private String partyId;

	@Column(name = "email", columnDefinition = "VARCHAR(150)")
	private String email;

	@Column(name = "phone_number", columnDefinition = "VARCHAR(20)")
	private String phoneNumber;

	@Column(name = "first_name", columnDefinition = "VARCHAR(100)")
	private String firstName;

	@Column(name = "last_name", columnDefinition = "VARCHAR(100)")
	private String lastName;

	@Column(name = "address", columnDefinition = "VARCHAR(255)")
	private String streetAddress;

	@Column(name = "apartment_number", columnDefinition = "VARCHAR(20)")
	private String apartmentNumber;

	@Column(name = "care_of", columnDefinition = "VARCHAR(100)")
	private String careOf;

	@Column(name = "zip_code", columnDefinition = "VARCHAR(10)")
	private String zipCode;

	@Column(name = "city", columnDefinition = "VARCHAR(100)")
	private String city;

	@Column(name = "country", columnDefinition = "VARCHAR(100)")
	private String country;

	@Column(name = "status", columnDefinition = "VARCHAR(50)")
	private MessageStatus messageStatus;

	@Column(name = "type", columnDefinition = "VARCHAR(50)")
	private MessageType messageType;

	@Column(name = "status_detail", columnDefinition = "TEXT")
	private String statusDetail;

	@Column(name = "messaging_id", columnDefinition = "VARCHAR(36)")
	private String messagingId;

	@Column(name = "created", columnDefinition = "DATETIME")
	private OffsetDateTime created;

	@PrePersist
	void prePersist() {
		created = OffsetDateTime.now();
	}

	public static RecipientEntity create() {
		return new RecipientEntity();
	}

	public MessageStatus getMessageStatus() {
		return messageStatus;
	}

	public void setMessageStatus(MessageStatus messageStatus) {
		this.messageStatus = messageStatus;
	}

	public RecipientEntity withMessageStatus(MessageStatus messageStatus) {
		this.messageStatus = messageStatus;
		return this;
	}

	public String getMessagingId() {
		return messagingId;
	}

	public void setMessagingId(String messagingId) {
		this.messagingId = messagingId;
	}

	public RecipientEntity withMessagingId(String messagingId) {
		this.messagingId = messagingId;
		return this;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public RecipientEntity withMessageType(MessageType messageType) {
		this.messageType = messageType;
		return this;
	}

	public String getStatusDetail() {
		return statusDetail;
	}

	public void setStatusDetail(String statusDetail) {
		this.statusDetail = statusDetail;
	}

	public RecipientEntity withStatusDetail(String statusDetail) {
		this.statusDetail = statusDetail;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public RecipientEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}

	public RecipientEntity withPartyId(String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public RecipientEntity withEmail(String email) {
		this.email = email;
		return this;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public RecipientEntity withPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public RecipientEntity withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public RecipientEntity withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public RecipientEntity withStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
		return this;
	}

	public String getApartmentNumber() {
		return apartmentNumber;
	}

	public void setApartmentNumber(String apartmentNumber) {
		this.apartmentNumber = apartmentNumber;
	}

	public RecipientEntity withApartmentNumber(String apartmentNumber) {
		this.apartmentNumber = apartmentNumber;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(String careOf) {
		this.careOf = careOf;
	}

	public RecipientEntity withCareOf(String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public RecipientEntity withZipCode(String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public RecipientEntity withCity(String city) {
		this.city = city;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public RecipientEntity withCountry(String country) {
		this.country = country;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public RecipientEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public String toString() {
		return "RecipientEntity{" +
			"id='" + id + '\'' +
			", partyId='" + partyId + '\'' +
			", email='" + email + '\'' +
			", phoneNumber='" + phoneNumber + '\'' +
			", firstName='" + firstName + '\'' +
			", lastName='" + lastName + '\'' +
			", streetAddress='" + streetAddress + '\'' +
			", apartmentNumber='" + apartmentNumber + '\'' +
			", careOf='" + careOf + '\'' +
			", zipCode='" + zipCode + '\'' +
			", city='" + city + '\'' +
			", country='" + country + '\'' +
			", messageStatus=" + messageStatus +
			", messageType=" + messageType +
			", statusDetail='" + statusDetail + '\'' +
			", messagingId='" + messagingId + '\'' +
			", created=" + created +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		RecipientEntity that = (RecipientEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(partyId, that.partyId) && Objects.equals(email, that.email) && Objects.equals(phoneNumber, that.phoneNumber) && Objects.equals(firstName,
			that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(streetAddress, that.streetAddress) && Objects.equals(apartmentNumber, that.apartmentNumber) && Objects.equals(careOf, that.careOf)
			&& Objects.equals(zipCode, that.zipCode) && Objects.equals(city, that.city) && Objects.equals(country, that.country) && messageStatus == that.messageStatus && messageType == that.messageType
			&& Objects.equals(statusDetail, that.statusDetail) && Objects.equals(messagingId, that.messagingId) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, partyId, email, phoneNumber, firstName, lastName, streetAddress, apartmentNumber, careOf, zipCode, city, country, messageStatus, messageType, statusDetail, messagingId, created);
	}
}
