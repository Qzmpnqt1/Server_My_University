CREATE INDEX idx_registration_requests_email_status
    ON registration_requests(email, status);
