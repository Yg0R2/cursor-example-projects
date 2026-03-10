-- Seed user: username 'test', password 'test' (BCrypt cost 10)
-- Hash generated with BCryptPasswordEncoder for "test"
INSERT INTO users (id, username, password) VALUES (1, 'test', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOe5L6Y5qY5qY5qY5qY5qY5qY5qY5qY5qY');
INSERT INTO user_roles (user_entity_id, role) VALUES (1, 'ROLE_ADMIN');
