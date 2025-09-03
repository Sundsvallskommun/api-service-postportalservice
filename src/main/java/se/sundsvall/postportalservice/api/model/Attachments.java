package se.sundsvall.postportalservice.api.model;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.validation.NoDuplicateFileNames;

public class Attachments {

	@NotEmpty
	@NoDuplicateFileNames
	private List<MultipartFile> files;

	public static Attachments create() {
		return new Attachments();
	}

	public List<MultipartFile> getFiles() {
		return files;
	}

	public void setFiles(List<MultipartFile> files) {
		this.files = files;
	}

	public Attachments withFiles(List<MultipartFile> files) {
		this.files = files;
		return this;
	}

	@Override
	public String toString() {
		return "Attachments{" +
			"files=" + files +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Attachments that = (Attachments) o;
		return Objects.equals(files, that.files);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(files);
	}
}
