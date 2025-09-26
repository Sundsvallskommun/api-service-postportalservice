package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "department")
public class DepartmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "name", columnDefinition = "VARCHAR(100)")
	private String name;

	// Organization number refers to the official organization number.
	@Column(name = "organization_number", columnDefinition = "VARCHAR(12)")
	private String organizationNumber;

	// Organization ID refers to the ID from the Employee API
	@Column(name = "organization_id", columnDefinition = "VARCHAR(12)")
	private String organizationId;

	@Column(name = "support_text", columnDefinition = "VARCHAR(255)")
	private String supportText;

	@Column(name = "contact_information_url", columnDefinition = "VARCHAR(255)")
	private String contactInformationUrl;

	@Column(name = "contact_information_phone_number", columnDefinition = "VARCHAR(30)")
	private String contactInformationPhoneNumber;

	@Column(name = "contact_information_email", columnDefinition = "VARCHAR(100)")
	private String contactInformationEmail;

	public static DepartmentEntity create() {
		return new DepartmentEntity();
	}

	public String getOrganizationNumber() {
		return organizationNumber;
	}

	public DepartmentEntity withOrganizationNumber(String organizationNumber) {
		this.organizationNumber = organizationNumber;
		return this;
	}

	public void setOrganizationNumber(String organizationNumber) {
		this.organizationNumber = organizationNumber;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DepartmentEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DepartmentEntity withName(String name) {
		this.name = name;
		return this;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String orgId) {
		this.organizationId = orgId;
	}

	public DepartmentEntity withOrganizationId(String organizationId) {
		this.organizationId = organizationId;
		return this;
	}

	public String getSupportText() {
		return supportText;
	}

	public void setSupportText(String supportText) {
		this.supportText = supportText;
	}

	public DepartmentEntity withSupportText(String supportText) {
		this.supportText = supportText;
		return this;
	}

	public String getContactInformationUrl() {
		return contactInformationUrl;
	}

	public void setContactInformationUrl(String contactInformationUrl) {
		this.contactInformationUrl = contactInformationUrl;
	}

	public DepartmentEntity withContactInformationUrl(String contactInformationUrl) {
		this.contactInformationUrl = contactInformationUrl;
		return this;
	}

	public String getContactInformationPhoneNumber() {
		return contactInformationPhoneNumber;
	}

	public void setContactInformationPhoneNumber(String contactInformationPhoneNumber) {
		this.contactInformationPhoneNumber = contactInformationPhoneNumber;
	}

	public DepartmentEntity withContactInformationPhoneNumber(String contactInformationPhoneNumber) {
		this.contactInformationPhoneNumber = contactInformationPhoneNumber;
		return this;
	}

	public String getContactInformationEmail() {
		return contactInformationEmail;
	}

	public void setContactInformationEmail(String contactInformationEmail) {
		this.contactInformationEmail = contactInformationEmail;
	}

	public DepartmentEntity withContactInformationEmail(String contactInformationEmail) {
		this.contactInformationEmail = contactInformationEmail;
		return this;
	}

	@Override
	public String toString() {
		return "DepartmentEntity{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", organizationNumber='" + organizationNumber + '\'' +
			", organizationId='" + organizationId + '\'' +
			", supportText='" + supportText + '\'' +
			", contactInformationUrl='" + contactInformationUrl + '\'' +
			", contactInformationPhoneNumber='" + contactInformationPhoneNumber + '\'' +
			", contactInformationEmail='" + contactInformationEmail + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		DepartmentEntity that = (DepartmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(organizationNumber, that.organizationNumber) && Objects.equals(organizationId, that.organizationId)
			&& Objects.equals(supportText, that.supportText) && Objects.equals(contactInformationUrl, that.contactInformationUrl) && Objects.equals(contactInformationPhoneNumber, that.contactInformationPhoneNumber)
			&& Objects.equals(contactInformationEmail, that.contactInformationEmail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, organizationNumber, organizationId, supportText, contactInformationUrl, contactInformationPhoneNumber, contactInformationEmail);
	}
}
