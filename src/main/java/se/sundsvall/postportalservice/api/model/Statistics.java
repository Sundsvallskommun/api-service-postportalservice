package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Statistics model")
public class Statistics {

	@Schema(description = "Entity id", examples = "f40e6975-a82a-4167-8622-4b0e71ab8d92")
	private String id;

	@Schema(description = "Entity name", examples = "Test Department")
	private String name;

	@Schema(description = "Number of snail mail sent", examples = "50")
	private Long snailMail;

	@Schema(description = "Number of digital mail sent", examples = "30")
	private Long digitalMail;

	@Schema(description = "Number of text messages", examples = "20")
	private Long sms;

	@Schema(description = "Number of registered letters sent", examples = "5")
	private Long digitalRegisteredLetter;

	public static Statistics create() {
		return new Statistics();
	}

	public String getId() {
		return id;
	}

	public Statistics withId(String id) {
		this.id = id;
		return this;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public Statistics withName(String name) {
		this.name = name;
		return this;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getSnailMail() {
		return snailMail;
	}

	public Statistics withSnailMail(Long snailMail) {
		this.snailMail = snailMail;
		return this;
	}

	public void setSnailMail(Long snailMail) {
		this.snailMail = snailMail;
	}

	public Long getDigitalMail() {
		return digitalMail;
	}

	public Statistics withDigitalMail(Long digitalMail) {
		this.digitalMail = digitalMail;
		return this;
	}

	public void setDigitalMail(Long digitalMail) {
		this.digitalMail = digitalMail;
	}

	public Long getSms() {
		return sms;
	}

	public Statistics withSms(Long sms) {
		this.sms = sms;
		return this;
	}

	public void setSms(Long sms) {
		this.sms = sms;
	}

	public Long getDigitalRegisteredLetter() {
		return digitalRegisteredLetter;
	}

	public Statistics withDigitalRegisteredLetter(Long digitalRegisteredLetter) {
		this.digitalRegisteredLetter = digitalRegisteredLetter;
		return this;
	}

	public void setDigitalRegisteredLetter(Long digitalRegisteredLetter) {
		this.digitalRegisteredLetter = digitalRegisteredLetter;
	}

	@Override
	public String toString() {
		return "Statistics{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", snailMail=" + snailMail +
			", digitalMail=" + digitalMail +
			", sms=" + sms +
			", digitalRegisteredLetter=" + digitalRegisteredLetter +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Statistics that = (Statistics) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(snailMail, that.snailMail) && Objects.equals(digitalMail, that.digitalMail) && Objects.equals(sms, that.sms)
			&& Objects.equals(digitalRegisteredLetter, that.digitalRegisteredLetter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, snailMail, digitalMail, sms, digitalRegisteredLetter);
	}
}
