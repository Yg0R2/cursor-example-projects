-- Seed user: username 'test', password 'test' (BCrypt cost 10)
-- Hash generated with BCryptPasswordEncoder for "test"
INSERT INTO users (id, username, password) VALUES ('098f6bcd-4621-3373-8ade-4e832627b4f6', 'test', '$2a$10$71ABmR8IQQf6gdqfM6i86.8gObbVudyzbeCdeZpt2YxctEbF7KApS');
INSERT INTO user_roles (user_entity_id, role) VALUES ('098f6bcd-4621-3373-8ade-4e832627b4f6', 'ROLE_ADMIN');
