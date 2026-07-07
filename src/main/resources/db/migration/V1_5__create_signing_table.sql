CREATE TABLE signing
(
    id               VARCHAR(36)  NOT NULL,
    message_id       VARCHAR(36)  NULL,
    provider_case_id VARCHAR(255) NULL,
    provider         VARCHAR(50)  NULL,
    status           VARCHAR(50)  NULL,
    attachment_id    VARCHAR(36)  NULL,
    created          datetime     NULL,
    CONSTRAINT pk_signing PRIMARY KEY (id)
);

ALTER TABLE signing
    ADD CONSTRAINT FK_SIGNING_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES message (id);

ALTER TABLE signing
    ADD CONSTRAINT FK_SIGNING_ON_ATTACHMENT FOREIGN KEY (attachment_id) REFERENCES attachment (id);

CREATE INDEX IDX_SIGNING_MESSAGE_ID ON signing (message_id);

CREATE INDEX IDX_SIGNING_PROVIDER_CASE_ID ON signing (provider_case_id);
