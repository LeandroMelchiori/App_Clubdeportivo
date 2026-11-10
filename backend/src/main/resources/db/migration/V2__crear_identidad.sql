-- Identidad: clubes, usuarios, membresías con rol y sesiones con tokens de renovación.
-- Los ids los genera la API. Las fechas las pone la base.

CREATE TABLE clubs (
    id         uuid        PRIMARY KEY,
    name       text        NOT NULL CHECK (length(trim(name)) > 0),
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Un usuario es global: puede pertenecer a varios clubes con un rol distinto en cada uno.
CREATE TABLE users (
    id                       uuid        PRIMARY KEY,
    email                    text        NOT NULL UNIQUE CHECK (email = lower(email)),
    name                     text        NOT NULL CHECK (length(trim(name)) > 0),
    password_hash            text        NOT NULL,
    password_change_required boolean     NOT NULL DEFAULT false,
    created_at               timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE club_memberships (
    user_id       uuid        NOT NULL REFERENCES users (id),
    club_id       uuid        NOT NULL REFERENCES clubs (id),
    role          text        NOT NULL CHECK (role IN ('ADMIN', 'RECEPTION', 'CASHIER', 'READ_ONLY')),
    active        boolean     NOT NULL DEFAULT true,
    created_at    timestamptz NOT NULL DEFAULT now(),
    removed_at    timestamptz,
    last_login_at timestamptz,
    PRIMARY KEY (user_id, club_id)
);

CREATE INDEX club_memberships_club_idx ON club_memberships (club_id);

-- Una sesión es un ingreso de un usuario a un club. Revocarla invalida todos sus tokens de renovación.
CREATE TABLE sessions (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL,
    club_id    uuid        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    FOREIGN KEY (user_id, club_id) REFERENCES club_memberships (user_id, club_id)
);

CREATE INDEX sessions_user_idx ON sessions (user_id);
CREATE INDEX sessions_club_user_idx ON sessions (club_id, user_id);

-- Solo se guarda el hash SHA-256 del token. Los usados quedan para detectar la reutilización de uno robado.
CREATE TABLE refresh_tokens (
    token_hash bytea       PRIMARY KEY,
    session_id uuid        NOT NULL REFERENCES sessions (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at    timestamptz
);

CREATE INDEX refresh_tokens_session_idx ON refresh_tokens (session_id);
