# PostPortalService

_The service acts as an entry point for a number of underlying services, which are used to handle postal mailings and SMS. Postal items are sent either digitally or physically to the respective recipient depending on whether the recipient has a digital mailbox or not._

_The service also handles sending registered letters in digital form, SMS and mass mailing of postal items via CSV file. Finally, it provides resources to perform prerequisite checks for the type of mailing (digital mail or snail mail) that the recipient has the opportunity to receive._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Sundsvallskommun/api-service-postportalservice.git
   cd api-service-postportalservice
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible. See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## Dependencies

This microservice depends on the following services:

- **DigitalRegisteredLetter**
  - **Purpose:** Service providing functionality to send digital registered letters.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-digital-registered-letter)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Employee**
  - **Purpose:** Service providing functionallity to read employee information.
  - **Repository:** External service
- **Messaging**
  - **Purpose:** Service providing functionallity to send different type of messages, such as emails, text messages and physical letters.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **MessagingSettings**
  - **Purpose:** A service providing functionallity to fetch messaging configurations.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-messaging-settings)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

Alternatively, refer to the `openapi.yml` file located in the project's test resource directory for the OpenAPI specification.

## Usage

### API Endpoints

Refer to the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/2281/statistics/departments?year=2025&month=10' \
  -H 'accept: */*'
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in `application.yml`.

### Key Configuration Parameters

- **Server Port:**

```yaml
spring:
  server:
    port: 8080
```

- **Database Settings:**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_db_username
    password: your_db_password
```

- **External Service URLs:**

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          citizen:
            token-uri: http://dependecy_service_token_url
          digitalregisteredletter:
            token-uri: http://dependecy_service_token_url
          messagingsettings:
            token-uri: http://dependecy_service_token_url
          messaging:
            token-uri: http://dependecy_service_token_url
          employee:
            token-uri: http://dependecy_service_token_url
        registration:
          citizen:
            client-id: some-client-id
            client-secret: some-client-secret
          digitalregisteredletter:
            client-id: some-client-id
            client-secret: some-client-secret
          messagingsettings:
            client-id: some-client-id
            client-secret: some-client-secret
          messaging:
            client-id: some-client-id
            client-secret: some-client-secret
          employee:
            client-id: some-client-id
            client-secret: some-client-secret

integration:
 citizen:
   url: http://dependency_service_url
 digitalregisteredletter:
   url: http://dependency_service_url
 messagingsettings:
   url: http://dependency_service_url
 messaging:
   url: http://dependency_service_url
 employee:
   url: http://dependency_service_url

```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-postportalservice&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-postportalservice)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-postportalservice&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-postportalservice)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-postportalservice&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-postportalservice)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-postportalservice&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-postportalservice)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-postportalservice&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-postportalservice)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-postportalservice&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-postportalservice)

---

&copy; 2025 Sundsvalls kommun
