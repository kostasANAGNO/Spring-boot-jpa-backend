-- DEV-ONLY seed: default ADMIN account (username "admin", password "Admin123!", BCrypt strength 10).
-- Change the password immediately (or remove this seed) before any real deployment.

INSERT INTO users (username, email, password, full_name, enabled, created_at, updated_at)
VALUES ('admin', 'admin@his.local', '$2a$10$hoT0UmsWj6w0Z6wu941PN.ujm8N4iglNpQ/W/vRdoubGV8XBiC2f6',
        'System Administrator', TRUE, now(), now());

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin';
