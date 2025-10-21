package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "user", indexes = {
	@Index(name = "IDX_USER_USERNAME", columnList = "username")
})
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "username", columnDefinition = "VARCHAR(100)")
	private String username;

	public static UserEntity create() {
		return new UserEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UserEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public UserEntity withUsername(String username) {
		this.username = username;
		return this;
	}

	@Override
	public String toString() {
		return "UserEntity{" +
			"id='" + id + '\'' +
			", username='" + username + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final var that = (UserEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(username, that.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, username);
	}
}
