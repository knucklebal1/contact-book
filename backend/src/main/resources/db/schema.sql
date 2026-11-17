-- MySQL 8 reference schema.
-- The application can initialize the tables automatically with ddl-auto=update.

CREATE DATABASE IF NOT EXISTS contact_book
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE contact_book;

CREATE TABLE IF NOT EXISTS contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    company VARCHAR(150),
    job_title VARCHAR(100),
    note VARCHAR(500),
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_contact_favorite (is_favorite),
    INDEX idx_contact_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contact_method (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contact_id BIGINT NOT NULL,
    method_type VARCHAR(20) NOT NULL,
    custom_label VARCHAR(32),
    method_value VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_contact_method_contact
        FOREIGN KEY (contact_id) REFERENCES contact (id)
        ON DELETE CASCADE,
    INDEX idx_contact_method_contact (contact_id),
    INDEX idx_contact_method_value (method_value(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
