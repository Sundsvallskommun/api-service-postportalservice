CREATE TABLE signing
(
    id               VARCHAR(36)  NOT NULL,
    message_id       VARCHAR(36)  NOT NULL,
    provider_case_id VARCHAR(255) NULL,
    provider         VARCHAR(50)  NULL,
    status           VARCHAR(50)  NULL,
    attachment_id    VARCHAR(36)  NULL,
    created          datetime     NULL,
    CONSTRAINT pk_signing PRIMARY KEY (id),
    CONSTRAINT UK_SIGNING_MESSAGE UNIQUE (message_id),
    CONSTRAINT UK_SIGNING_ATTACHMENT UNIQUE (attachment_id)
);

ALTER TABLE signing
    ADD CONSTRAINT FK_SIGNING_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE;

ALTER TABLE signing
    ADD CONSTRAINT FK_SIGNING_ON_ATTACHMENT FOREIGN KEY (attachment_id) REFERENCES attachment (id);
