
    create table attachment (
        created DATETIME,
        content_type VARCHAR(50),
        file_name VARCHAR(150),
        id VARCHAR(36) not null,
        message_id VARCHAR(36),
        content LONGBLOB,
        primary key (id)
    ) engine=InnoDB;

    create table department (
        contact_information_email VARCHAR(100),
        contact_information_phone_number VARCHAR(30),
        contact_information_url VARCHAR(255),
        id VARCHAR(36) not null,
        name VARCHAR(100),
        organization_id VARCHAR(12),
        organization_number VARCHAR(12),
        support_text VARCHAR(255),
        primary key (id)
    ) engine=InnoDB;

    create table message (
        created DATETIME,
        body TEXT,
        content_type VARCHAR(100),
        department_id VARCHAR(36),
        display_name VARCHAR(100),
        id VARCHAR(36) not null,
        message_type VARCHAR(50),
        municipality_id VARCHAR(6),
        subject VARCHAR(255),
        user_id VARCHAR(36),
        primary key (id)
    ) engine=InnoDB;

    create table recipient (
        created DATETIME,
        address VARCHAR(255),
        apartment_number VARCHAR(20),
        care_of VARCHAR(100),
        city VARCHAR(100),
        country VARCHAR(100),
        email VARCHAR(150),
        external_id VARCHAR(36),
        first_name VARCHAR(100),
        id VARCHAR(36) not null,
        last_name VARCHAR(100),
        message_id VARCHAR(36),
        party_id VARCHAR(36),
        phone_number VARCHAR(20),
        status VARCHAR(80),
        status_detail TEXT,
        type VARCHAR(50),
        zip_code VARCHAR(10),
        primary key (id)
    ) engine=InnoDB;

    create table user (
        id VARCHAR(36) not null,
        name VARCHAR(100),
        primary key (id)
    ) engine=InnoDB;

    create index IDX_ATTACHMENT_MESSAGE_ID 
       on attachment (message_id);

    create index IDX_MESSAGE_DEPARTMENT_ID 
       on message (department_id);

    create index IDX_MESSAGE_USER_ID 
       on message (user_id);

    create index IDX_RECIPIENT_MESSAGE_ID 
       on recipient (message_id);

    alter table if exists attachment 
       add constraint FK_ATTACHMENT_MESSAGE 
       foreign key (message_id) 
       references message (id);

    alter table if exists message 
       add constraint FK_MESSAGE_DEPARTMENT 
       foreign key (department_id) 
       references department (id);

    alter table if exists message 
       add constraint FK_MESSAGE_USER 
       foreign key (user_id) 
       references user (id);

    alter table if exists recipient 
       add constraint FK_RECIPIENT_MESSAGE 
       foreign key (message_id) 
       references message (id);
