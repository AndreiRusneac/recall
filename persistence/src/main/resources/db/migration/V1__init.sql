-- First version of the schema. Never edit this file after it is merged:
-- add V2__..., V3__... files instead, so every database upgrades the same way.

create table business (
    id         uuid primary key default gen_random_uuid(),
    name       text        not null,
    vertical   text        not null,
    plan       text        not null default 'REMINDERS',
    timezone   text        not null default 'Europe/Bucharest',
    created_at timestamptz not null default now()
);

create table app_user (
    id            uuid primary key default gen_random_uuid(),
    business_id   uuid        not null references business (id),
    email         text        not null unique,
    password_hash text        not null,
    created_at    timestamptz not null default now()
);

create table customer (
    id            uuid primary key default gen_random_uuid(),
    business_id   uuid        not null references business (id),
    name          text        not null,
    phone         text        not null,
    consent       text        not null default 'UNKNOWN',
    last_visit_at timestamptz,
    created_at    timestamptz not null default now(),
    unique (business_id, phone)
);

create table event (
    id               uuid primary key default gen_random_uuid(),
    business_id      uuid        not null references business (id),
    customer_id      uuid        not null references customer (id),
    kind             text        not null,
    starts_at        timestamptz not null,
    status           text        not null default 'SCHEDULED',
    reminder_sent_at timestamptz,
    created_at       timestamptz not null default now()
);

create index event_business_starts_at on event (business_id, starts_at);

-- The three tables below are not used by the code yet. They are here so the
-- schema matches the plan and the first issues can build on them.

create table rule (
    id                uuid primary key default gen_random_uuid(),
    business_id       uuid        not null references business (id),
    kind              text        not null,
    lead_time_minutes integer     not null,
    template          text        not null,
    enabled           boolean     not null default true,
    created_at        timestamptz not null default now()
);

create table message (
    id          uuid primary key default gen_random_uuid(),
    business_id uuid        not null references business (id),
    customer_id uuid        not null references customer (id),
    event_id    uuid references event (id),
    channel     text        not null,
    body        text        not null,
    status      text        not null,
    sent_at     timestamptz,
    created_at  timestamptz not null default now()
);

create table outcome (
    id           uuid primary key default gen_random_uuid(),
    business_id  uuid        not null references business (id),
    message_id   uuid references message (id),
    event_id     uuid references event (id),
    kind         text        not null,
    amount_cents integer,
    created_at   timestamptz not null default now()
);
