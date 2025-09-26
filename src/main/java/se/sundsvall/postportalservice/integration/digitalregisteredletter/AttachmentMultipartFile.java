package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static java.nio.file.Files.write;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import org.springframework.web.multipart.MultipartFile;

public class AttachmentMultipartFile implements MultipartFile {

	private final String name;
	private final String contentType;
	private final byte[] content;

	public AttachmentMultipartFile(String name, String contentType, Blob blob) throws Exception {
		this.name = name;
		this.contentType = contentType;
		this.content = blob.getBytes(1, (int) blob.length());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOriginalFilename() {
		return name;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public boolean isEmpty() {
		return content.length == 0;
	}

	@Override
	public long getSize() {
		return content.length;
	}

	@Override
	public byte[] getBytes() {
		return content;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(content);
	}

	@Override
	public void transferTo(File dest) throws IOException {
		write(dest.toPath(), content);
	}
}
