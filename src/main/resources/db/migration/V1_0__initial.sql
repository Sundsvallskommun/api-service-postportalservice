CREATE TABLE attachment
(
    id           VARCHAR(36)  NOT NULL,
    file_name    VARCHAR(150) NULL,
    content_type VARCHAR(50)  NULL,
    content      LONGBLOB     NULL,
    created      datetime     NULL,
    message_id   VARCHAR(36)  NULL,
    CONSTRAINT pk_attachment PRIMARY KEY (id)
);

CREATE TABLE department
(
    id                               VARCHAR(36)  NOT NULL,
    name                             VARCHAR(100) NULL,
    organization_number              VARCHAR(12)  NULL,
    organization_id                  VARCHAR(12)  NULL,
    support_text                     VARCHAR(255) NULL,
    contact_information_url          VARCHAR(255) NULL,
    contact_information_phone_number VARCHAR(30)  NULL,
    contact_information_email        VARCHAR(100) NULL,
    CONSTRAINT pk_department PRIMARY KEY (id)
);

CREATE TABLE message
(
    id              VARCHAR(36)  NOT NULL,
    municipality_id VARCHAR(6)   NULL,
    display_name    VARCHAR(100) NULL,
    message_type    VARCHAR(50)  NULL,
    body            TEXT         NULL,
    subject         VARCHAR(255) NULL,
    content_type    VARCHAR(100) NULL,
    created         datetime     NULL,
    user_id         VARCHAR(36)  NULL,
    department_id   VARCHAR(36)  NULL,
    CONSTRAINT pk_message PRIMARY KEY (id)
);

CREATE TABLE recipient
(
    id               VARCHAR(36)  NOT NULL,
    party_id         VARCHAR(36)  NULL,
    email            VARCHAR(150) NULL,
    phone_number     VARCHAR(20)  NULL,
    first_name       VARCHAR(100) NULL,
    last_name        VARCHAR(100) NULL,
    address          VARCHAR(255) NULL,
    apartment_number VARCHAR(20)  NULL,
    care_of          VARCHAR(100) NULL,
    zip_code         VARCHAR(10)  NULL,
    city             VARCHAR(100) NULL,
    country          VARCHAR(100) NULL,
    status           VARCHAR(50)  NULL,
    type             VARCHAR(50)  NULL,
    status_detail    TEXT         NULL,
    external_id      VARCHAR(36)  NULL,
    created          datetime     NULL,
    message_id       VARCHAR(36)  NULL,
    CONSTRAINT pk_recipient PRIMARY KEY (id)
);

CREATE TABLE user
(
    id   VARCHAR(36)  NOT NULL,
    name VARCHAR(100) NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);

ALTER TABLE attachment
    ADD CONSTRAINT FK_ATTACHMENT_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES message (id);

CREATE INDEX IDX_ATTACHMENT_MESSAGE_ID ON attachment (message_id);

ALTER TABLE message
    ADD CONSTRAINT FK_MESSAGE_DEPARTMENT FOREIGN KEY (department_id) REFERENCES department (id);

CREATE INDEX IDX_MESSAGE_DEPARTMENT_ID ON message (department_id);

ALTER TABLE message
    ADD CONSTRAINT FK_MESSAGE_USER FOREIGN KEY (user_id) REFERENCES user (id);

CREATE INDEX IDX_MESSAGE_USER_ID ON message (user_id);

ALTER TABLE recipient
    ADD CONSTRAINT FK_RECIPIENT_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES message (id);

CREATE INDEX IDX_RECIPIENT_MESSAGE_ID ON recipient (message_id);
