create table if not exists shareit_users
(
    user_id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    email   VARCHAR(255)                            NOT NULL,
    name    VARCHAR(255)                            NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (user_id),
    CONSTRAINT UQ_USER_EMAIL UNIQUE (email)
);

create table if not exists item_requests
(
    request_id   bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    description  VARCHAR(255)                            NOT NULL,
    created      TIMESTAMP WITHOUT TIME ZONE,
    requester_id BIGINT                                  NOT NULL,
    CONSTRAINT pr_requests PRIMARY KEY (request_id),
    CONSTRAINT fk_requester FOREIGN KEY (requester_id) REFERENCES shareit_users (user_id) ON delete CASCADE
);

create table if not exists items
(
    item_id     bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name        VARCHAR(255)                            NOT NULL,
    description VARCHAR(1000)                           NOT NULL,
    available   BOOLEAN                                 NOT NULL,
    owner_id    BIGINT                                  NOT NULL,
    request_id  BIGINT,
    CONSTRAINT pk_item PRIMARY KEY (item_id),
    CONSTRAINT fk_owner FOREIGN KEY (owner_id) REFERENCES shareit_users (user_id) ON delete CASCADE,
    CONSTRAINT fk_request_item FOREIGN KEY (request_id) REFERENCES item_requests (request_id) ON delete cascade
);

create table if not exists bookings
(
    booking_id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    item_id    BIGINT                                  NOT NULL,
    booker_id  BIGINT                                  NOT NULL,
    start_date TIMESTAMP WITHOUT TIME ZONE,
    end_date   TIMESTAMP WITHOUT TIME ZONE,
    status     VARCHAR(50),
    CONSTRAINT pk_booking PRIMARY KEY (booking_id),
    CONSTRAINT fk_booking_items FOREIGN KEY (item_id) REFERENCES items (item_id) ON delete CASCADE,
    CONSTRAINT fk_booker FOREIGN KEY (booker_id) REFERENCES shareit_users (user_id) ON delete CASCADE
);

create table if not exists comments
(
    comment_id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    text       VARCHAR(1000)                           NOT NULL,
    item_id    BIGINT                                  NOT NULL,
    author_id  BIGINT                                  NOT NULL,
    created    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_comments PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_items FOREIGN KEY (item_id) REFERENCES items (item_id) ON delete CASCADE,
    CONSTRAINT fk_user FOREIGN KEY (author_id) REFERENCES shareit_users (user_id) ON delete CASCADE
);
