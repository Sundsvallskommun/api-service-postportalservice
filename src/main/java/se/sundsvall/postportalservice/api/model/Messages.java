package se.sundsvall.postportalservice.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Messages model")
public class Messages {

	@JsonProperty("_meta")
	@Schema(implementation = PagingMetaData.class, accessMode = READ_ONLY)
	private PagingMetaData metaData;

	@ArraySchema(schema = @Schema(implementation = Message.class, accessMode = READ_ONLY))
	private List<Message> messages;

	public static Messages create() {
		return new Messages();
	}

	public PagingMetaData getMetaData() {
		return metaData;
	}

	public Messages withMetaData(PagingMetaData metaData) {
		this.metaData = metaData;
		return this;
	}

	public void setMetaData(PagingMetaData metaData) {
		this.metaData = metaData;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public Messages withMessages(List<Message> messages) {
		this.messages = messages;
		return this;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public String toString() {
		return "Messages{" +
			"metaData=" + metaData +
			", messages=" + messages +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Messages messages1 = (Messages) o;
		return Objects.equals(metaData, messages1.metaData) && Objects.equals(messages, messages1.messages);
	}

	@Override
	public int hashCode() {
		return Objects.hash(metaData, messages);
	}
}
