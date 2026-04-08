-- Password: Vernhes (BCrypt hash, will be verified/updated at startup by DataInitializer)
INSERT INTO developers (email, password, username, title, discord_link, role, created_at, updated_at)
VALUES (
    'albin@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Albin',
    'Administrator',
    'https://discord.com/users/378853461464186890',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
