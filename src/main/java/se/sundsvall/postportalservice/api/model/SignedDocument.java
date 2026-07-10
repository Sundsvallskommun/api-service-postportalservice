package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "A signed document received in a signing event")
public class SignedDocument {

	@Schema(description = "Descriptive name of the document", examples = "Employment contract")
	private String name;

	@Schema(description = "The document file name including extension", examples = "contract.pdf")
	private String fileName;

	@Schema(description = "The document mime type", examples = "application/pdf")
	private String mimeType;

	@Schema(description = "Base64-encoded content of the signed document")
	@NotBlank
	private String content;

	public static SignedDocument create() {
		return new SignedDocument();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SignedDocument withName(String name) {
		this.name = name;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public SignedDocument withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public SignedDocument withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public SignedDocument withContent(String content) {
		this.content = content;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SignedDocument that = (SignedDocument) o;
		return Objects.equals(name, that.name) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType) && Objects.equals(content, that.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, fileName, mimeType, content);
	}

	@Override
	public String toString() {
		return "SignedDocument{" +
			"name='" + name + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", content='" + content + '\'' +
			'}';
	}
}
