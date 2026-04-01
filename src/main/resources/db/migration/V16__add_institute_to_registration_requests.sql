ALTER TABLE registration_requests
    ADD COLUMN institute_id BIGINT NULL,
    ADD CONSTRAINT fk_registration_requests_institute
        FOREIGN KEY (institute_id) REFERENCES institutes(id);
