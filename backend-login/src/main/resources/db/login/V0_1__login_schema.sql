-- Canonical schema for the platform login module: user accounts + refresh tokens.
--
-- NEW consumers opt in by adding `classpath:db/login` to spring.flyway.locations
-- (with its own flyway history table). Apps that already created these tables under
-- their own migration history keep that history and do NOT add this location.
-- IF NOT EXISTS keeps it safe if the tables happen to pre-exist.

create table if not exists users (
    id            uuid primary key,
    email         varchar(320) not null unique,
    password_hash varchar(100),
    provider      varchar(16)  not null default 'LOCAL',
    provider_id   varchar(255),
    created_at    timestamptz  not null default now()
);

create table if not exists refresh_tokens (
    id         uuid primary key,
    user_id    uuid        not null references users(id),
    token_hash varchar(64) not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    revoked    boolean     not null default false
);

create index if not exists idx_refresh_tokens_user on refresh_tokens(user_id);
