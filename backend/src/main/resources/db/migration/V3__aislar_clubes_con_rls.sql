-- Reglas por fila (row-level security) como respaldo del filtro por club que hace la API.
--
-- La API opera los datos de un club con el rol club_app, que no es dueño de las tablas y por eso
-- cumple estas reglas. Al empezar cada transacción fija el club activo con
-- set_config('app.club_id', <club>, true), que dura solo hasta el final de la transacción.
-- Sin club activo, las reglas no dejan ver ni escribir nada.
--
-- Las operaciones de identidad (alta de club, login, renovación, cambio de contraseña) no pertenecen
-- a un solo club y corren con el usuario dueño de las tablas, al que estas reglas no se aplican.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'club_app') THEN
        CREATE ROLE club_app NOLOGIN;
    END IF;
END
$$;

-- El usuario con el que se conecta la API tiene que poder pasar a club_app con SET ROLE.
GRANT club_app TO CURRENT_USER;

CREATE FUNCTION current_club_id() RETURNS uuid
    LANGUAGE sql STABLE
    AS $$ SELECT nullif(current_setting('app.club_id', true), '')::uuid $$;

ALTER TABLE clubs ENABLE ROW LEVEL SECURITY;
CREATE POLICY clubs_del_club_activo ON clubs
    USING (id = current_club_id());

ALTER TABLE club_memberships ENABLE ROW LEVEL SECURITY;
CREATE POLICY memberships_del_club_activo ON club_memberships
    USING (club_id = current_club_id())
    WITH CHECK (club_id = current_club_id());

ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY sessions_del_club_activo ON sessions
    USING (club_id = current_club_id())
    WITH CHECK (club_id = current_club_id());

-- Un usuario solo es visible desde los clubes en los que tiene una membresía. La subconsulta también
-- cumple la regla de club_memberships, así que solo ve las del club activo.
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY users_del_club_activo ON users FOR SELECT
    USING (EXISTS (SELECT 1 FROM club_memberships m WHERE m.user_id = users.id));
CREATE POLICY users_alta_desde_un_club ON users FOR INSERT
    WITH CHECK (current_club_id() IS NOT NULL);

-- Los tokens de renovación son solo de identidad: club_app no tiene permisos ni reglas sobre ellos.
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- Permisos mínimos para lo que hoy se hace dentro de un club: administrar sus miembros.
GRANT USAGE ON SCHEMA public TO club_app;
GRANT SELECT ON clubs TO club_app;
GRANT SELECT, INSERT, UPDATE (role, active, removed_at) ON club_memberships TO club_app;
GRANT SELECT, UPDATE (revoked_at) ON sessions TO club_app;
-- Sin acceso al hash de la contraseña, salvo para cargarlo al dar de alta a alguien.
GRANT SELECT (id, email, name, created_at),
      INSERT (id, email, name, password_hash, password_change_required)
    ON users TO club_app;

-- Para sumar a un club a alguien que ya tiene cuenta en otro hay que encontrarlo por email, aunque la
-- regla de users no lo deje ver. Esta función lo hace con los permisos del dueño y solo devuelve el id.
CREATE FUNCTION user_id_by_email(p_email text) RETURNS uuid
    LANGUAGE sql STABLE SECURITY DEFINER
    SET search_path = public, pg_temp
    AS $$ SELECT id FROM users WHERE email = lower(p_email) $$;

REVOKE ALL ON FUNCTION user_id_by_email(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION user_id_by_email(text) TO club_app;
