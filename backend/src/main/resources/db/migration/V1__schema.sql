-- NEXORA schema. Written in portable SQL that runs on MySQL 8 and on H2 (MySQL mode, tests only).
-- All timestamps are stored as UTC in DATETIME(6) columns (hibernate.jdbc.time_zone=UTC).

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    title         VARCHAR(80),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_role ON users (role);

-- ---------------------------------------------------------------- audit
CREATE TABLE audit_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id       BIGINT,
    actor_username VARCHAR(50)   NOT NULL,
    action         VARCHAR(60)   NOT NULL,
    entity_type    VARCHAR(60)   NOT NULL,
    entity_id      VARCHAR(60),
    outcome        VARCHAR(10)   NOT NULL,
    details        VARCHAR(1000),
    created_at     DATETIME(6)   NOT NULL,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);

-- ---------------------------------------------------------------- inventory
CREATE TABLE suppliers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    contact_email VARCHAR(120),
    phone         VARCHAR(40),
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_suppliers_name UNIQUE (name)
);

CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku                    VARCHAR(40)    NOT NULL,
    name                   VARCHAR(120)   NOT NULL,
    description            VARCHAR(1000),
    category_id            BIGINT         NOT NULL,
    supplier_id            BIGINT,
    purchase_price         DECIMAL(12, 2) NOT NULL,
    retail_price           DECIMAL(12, 2) NOT NULL,
    quantity               INT            NOT NULL,
    min_stock              INT            NOT NULL,
    unit                   VARCHAR(20)    NOT NULL,
    replenishment_strategy VARCHAR(30)    NOT NULL,
    active                 BOOLEAN        NOT NULL DEFAULT TRUE,
    version                BIGINT         NOT NULL DEFAULT 0,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT ck_products_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_products_min CHECK (min_stock >= 0),
    CONSTRAINT ck_products_prices CHECK (purchase_price >= 0 AND retail_price >= 0)
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_name ON products (name);

CREATE TABLE inventory_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT       NOT NULL,
    movement_type   VARCHAR(20)  NOT NULL,
    quantity_change INT          NOT NULL,
    quantity_before INT          NOT NULL,
    quantity_after  INT          NOT NULL,
    reason          VARCHAR(255),
    performed_by    BIGINT,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_invtx_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_invtx_user FOREIGN KEY (performed_by) REFERENCES users (id)
);
CREATE INDEX idx_invtx_product_created ON inventory_transactions (product_id, created_at);
CREATE INDEX idx_invtx_created ON inventory_transactions (created_at);

CREATE TABLE stock_alerts (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id         BIGINT      NOT NULL,
    quantity_at_alert  INT         NOT NULL,
    threshold          INT         NOT NULL,
    suggested_quantity INT         NOT NULL,
    strategy           VARCHAR(30) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    document_id        BIGINT,
    created_at         DATETIME(6) NOT NULL,
    resolved_at        DATETIME(6),
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id) REFERENCES products (id)
);
CREATE INDEX idx_alert_status ON stock_alerts (status, product_id);

-- ---------------------------------------------------------------- clinic
CREATE TABLE specialties (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_specialties_name UNIQUE (name)
);

CREATE TABLE doctors (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT,
    full_name    VARCHAR(120) NOT NULL,
    specialty_id BIGINT       NOT NULL,
    email        VARCHAR(120),
    phone        VARCHAR(40),
    room         VARCHAR(20),
    bio          VARCHAR(500),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT uk_doctors_user UNIQUE (user_id),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_doctors_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id)
);

CREATE TABLE doctor_schedules (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id    BIGINT NOT NULL,
    day_of_week  INT    NOT NULL,
    start_time   TIME   NOT NULL,
    end_time     TIME   NOT NULL,
    CONSTRAINT uk_schedule_day UNIQUE (doctor_id, day_of_week),
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT ck_schedule_day CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE TABLE patients (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    date_of_birth DATE         NOT NULL,
    gender        VARCHAR(10)  NOT NULL,
    phone         VARCHAR(40),
    email         VARCHAR(120),
    notes         VARCHAR(1000),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_patients_user UNIQUE (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_patients_name ON patients (last_name, first_name);

CREATE TABLE appointments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id    BIGINT       NOT NULL,
    doctor_id     BIGINT       NOT NULL,
    start_time    DATETIME(6)  NOT NULL,
    end_time      DATETIME(6)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    reason        VARCHAR(255) NOT NULL,
    notes         VARCHAR(1000),
    cancel_reason VARCHAR(255),
    created_by    BIGINT,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_appt_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_appt_interval CHECK (end_time > start_time)
);
CREATE INDEX idx_appt_doctor_start ON appointments (doctor_id, start_time);
CREATE INDEX idx_appt_patient ON appointments (patient_id, start_time);
CREATE INDEX idx_appt_start ON appointments (start_time);

-- ---------------------------------------------------------------- flights (educational demo data only)
CREATE TABLE airports (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    code     VARCHAR(3)   NOT NULL,
    name     VARCHAR(120) NOT NULL,
    city     VARCHAR(80)  NOT NULL,
    country  VARCHAR(80)  NOT NULL,
    timezone VARCHAR(40)  NOT NULL,
    CONSTRAINT uk_airports_code UNIQUE (code)
);

CREATE TABLE flight_routes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_number    VARCHAR(10)    NOT NULL,
    airline          VARCHAR(80)    NOT NULL,
    origin_id        BIGINT         NOT NULL,
    destination_id   BIGINT         NOT NULL,
    departure_local  TIME           NOT NULL,
    duration_minutes INT            NOT NULL,
    base_price       DECIMAL(12, 2) NOT NULL,
    seat_rows        INT            NOT NULL,
    seats_per_row    INT            NOT NULL,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_routes_number UNIQUE (flight_number),
    CONSTRAINT fk_routes_origin FOREIGN KEY (origin_id) REFERENCES airports (id),
    CONSTRAINT fk_routes_destination FOREIGN KEY (destination_id) REFERENCES airports (id)
);

CREATE TABLE flights (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id        BIGINT,
    flight_number   VARCHAR(10)    NOT NULL,
    airline         VARCHAR(80)    NOT NULL,
    origin_id       BIGINT         NOT NULL,
    destination_id  BIGINT         NOT NULL,
    departure_time  DATETIME(6)    NOT NULL,
    arrival_time    DATETIME(6)    NOT NULL,
    base_price      DECIMAL(12, 2) NOT NULL,
    total_seats     INT            NOT NULL,
    available_seats INT            NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_flights_number_departure UNIQUE (flight_number, departure_time),
    CONSTRAINT fk_flights_route FOREIGN KEY (route_id) REFERENCES flight_routes (id),
    CONSTRAINT fk_flights_origin FOREIGN KEY (origin_id) REFERENCES airports (id),
    CONSTRAINT fk_flights_destination FOREIGN KEY (destination_id) REFERENCES airports (id),
    CONSTRAINT ck_flights_seats CHECK (available_seats >= 0 AND available_seats <= total_seats)
);
CREATE INDEX idx_flights_search ON flights (origin_id, destination_id, departure_time);
CREATE INDEX idx_flights_departure ON flights (departure_time);

CREATE TABLE flight_bookings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference       VARCHAR(8)     NOT NULL,
    flight_id       BIGINT         NOT NULL,
    user_id         BIGINT         NOT NULL,
    passenger_count INT            NOT NULL,
    total_price     DECIMAL(12, 2) NOT NULL,
    contact_email   VARCHAR(120)   NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    expires_at      DATETIME(6)    NOT NULL,
    confirmed_at    DATETIME(6),
    cancelled_at    DATETIME(6),
    cancel_reason   VARCHAR(255),
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    CONSTRAINT uk_bookings_reference UNIQUE (reference),
    CONSTRAINT fk_bookings_flight FOREIGN KEY (flight_id) REFERENCES flights (id),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_bookings_user ON flight_bookings (user_id, created_at);
CREATE INDEX idx_bookings_status ON flight_bookings (status, expires_at);

CREATE TABLE flight_seats (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_id   BIGINT      NOT NULL,
    seat_number VARCHAR(4)  NOT NULL,
    status      VARCHAR(12) NOT NULL,
    booking_id  BIGINT,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_seats_flight_number UNIQUE (flight_id, seat_number),
    CONSTRAINT fk_seats_flight FOREIGN KEY (flight_id) REFERENCES flights (id),
    CONSTRAINT fk_seats_booking FOREIGN KEY (booking_id) REFERENCES flight_bookings (id)
);
CREATE INDEX idx_seats_flight_status ON flight_seats (flight_id, status);

CREATE TABLE booking_passengers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT       NOT NULL,
    full_name   VARCHAR(120) NOT NULL,
    seat_number VARCHAR(4)   NOT NULL,
    CONSTRAINT fk_passenger_booking FOREIGN KEY (booking_id) REFERENCES flight_bookings (id)
);

-- ---------------------------------------------------------------- workflow
CREATE TABLE documents (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    number              VARCHAR(20),
    title               VARCHAR(160)   NOT NULL,
    doc_type            VARCHAR(40)    NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    author_id           BIGINT         NOT NULL,
    amount              DECIMAL(12, 2),
    content             VARCHAR(4000)  NOT NULL,
    related_entity_type VARCHAR(40),
    related_entity_id   BIGINT,
    approval_round      INT            NOT NULL DEFAULT 0,
    revision            INT            NOT NULL DEFAULT 1,
    submitted_at        DATETIME(6),
    completed_at        DATETIME(6),
    rejection_reason    VARCHAR(500),
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    CONSTRAINT uk_documents_number UNIQUE (number),
    CONSTRAINT fk_documents_author FOREIGN KEY (author_id) REFERENCES users (id)
);
CREATE INDEX idx_documents_status ON documents (status);
CREATE INDEX idx_documents_author ON documents (author_id, created_at);
CREATE INDEX idx_documents_related ON documents (related_entity_type, related_entity_id);

CREATE TABLE approval_steps (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   BIGINT       NOT NULL,
    approval_round INT         NOT NULL,
    step_order    INT          NOT NULL,
    name          VARCHAR(60)  NOT NULL,
    required_role VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    decided_by    BIGINT,
    decided_at    DATETIME(6),
    decision_comment VARCHAR(500),
    CONSTRAINT uk_steps_order UNIQUE (document_id, approval_round, step_order),
    CONSTRAINT fk_steps_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_steps_decider FOREIGN KEY (decided_by) REFERENCES users (id)
);
CREATE INDEX idx_steps_status_role ON approval_steps (status, required_role);

CREATE TABLE document_comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT        NOT NULL,
    author_id   BIGINT        NOT NULL,
    body        VARCHAR(1000) NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    CONSTRAINT fk_comments_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE document_actions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT       NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    actor_id    BIGINT,
    from_status VARCHAR(20),
    to_status   VARCHAR(20),
    note        VARCHAR(500),
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_docactions_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_docactions_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);
CREATE INDEX idx_docactions_doc ON document_actions (document_id, created_at);

CREATE TABLE document_revisions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT         NOT NULL,
    revision    INT            NOT NULL,
    title       VARCHAR(160)   NOT NULL,
    content     VARCHAR(4000)  NOT NULL,
    amount      DECIMAL(12, 2),
    changed_by  BIGINT,
    created_at  DATETIME(6)    NOT NULL,
    CONSTRAINT uk_revisions UNIQUE (document_id, revision),
    CONSTRAINT fk_revisions_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_revisions_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE TABLE booking_documents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT        NOT NULL,
    document_number VARCHAR(30)   NOT NULL,
    document_id     BIGINT,
    content         VARCHAR(2000) NOT NULL,
    issued_at       DATETIME(6)   NOT NULL,
    CONSTRAINT uk_bookingdocs_booking UNIQUE (booking_id),
    CONSTRAINT uk_bookingdocs_number UNIQUE (document_number),
    CONSTRAINT fk_bookingdocs_booking FOREIGN KEY (booking_id) REFERENCES flight_bookings (id),
    CONSTRAINT fk_bookingdocs_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

-- ---------------------------------------------------------------- notifications
CREATE TABLE notifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id  BIGINT        NOT NULL,
    category      VARCHAR(20)   NOT NULL,
    severity      VARCHAR(10)   NOT NULL,
    title         VARCHAR(160)  NOT NULL,
    message       VARCHAR(1000) NOT NULL,
    link          VARCHAR(255),
    source_module VARCHAR(20)   NOT NULL,
    read_at       DATETIME(6),
    created_at    DATETIME(6)   NOT NULL,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at);
CREATE INDEX idx_notifications_unread ON notifications (recipient_id, read_at);

CREATE TABLE notification_preferences (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    enabled      BOOLEAN     NOT NULL,
    min_severity VARCHAR(10) NOT NULL,
    CONSTRAINT uk_prefs_user_channel UNIQUE (user_id, channel),
    CONSTRAINT fk_prefs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE delivery_attempts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT       NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    status          VARCHAR(12)  NOT NULL,
    attempt_no      INT          NOT NULL,
    detail          VARCHAR(500),
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_attempts_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
);
CREATE INDEX idx_attempts_notification ON delivery_attempts (notification_id);
CREATE INDEX idx_attempts_created ON delivery_attempts (created_at);
