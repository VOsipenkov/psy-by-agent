create table users (
    id bigserial primary key,
    username varchar(100) not null unique,
    password_hash varchar(255) not null,
    role varchar(20) not null,
    created_at timestamp not null default now()
);

create table dream_sessions (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    title varchar(255) not null,
    status varchar(20) not null,
    final_interpretation text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_dream_sessions_user_id on dream_sessions(user_id);

create table dream_messages (
    id bigserial primary key,
    session_id bigint not null references dream_sessions(id) on delete cascade,
    sender varchar(20) not null,
    content text not null,
    created_at timestamp not null default now()
);

create index idx_dream_messages_session_id on dream_messages(session_id);
