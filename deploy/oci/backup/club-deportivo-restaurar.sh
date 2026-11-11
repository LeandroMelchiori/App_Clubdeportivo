#!/usr/bin/env bash
# Restaura un backup en una base aparte y compara cuántas filas tiene cada tabla contra producción.
#
# Uso, como root en la VM:
#   club-deportivo-restaurar                        # el backup más reciente del bucket
#   club-deportivo-restaurar postgres/<objeto>      # un backup puntual del bucket
#   club-deportivo-restaurar --archivo <ruta>       # un dump que ya está en disco
#
# Nunca toca la base de producción: restaura en clubdeportivo_restauracion, que se recrea en cada corrida
# y queda para revisarla. Para devolver producción a un backup, ver el runbook del README.
#
# Solo muestra nombres de tablas y cantidades de filas, nunca su contenido.
set -euo pipefail

CONFIG=/etc/docker-platform/apps/club-deportivo/backup.env
CONTENEDOR=club-deportivo-postgres-1
BASE=clubdeportivo
DESTINO=clubdeportivo_restauracion

archivo=$(mktemp -p "${TMPDIR:-/var/tmp}" club-deportivo-restauracion.XXXXXX)
trap 'rm -f "$archivo"' EXIT

if [ "${1:-}" = "--archivo" ]; then
  cp "${2:?Falta la ruta del dump}" "$archivo"
  origen="$2"
else
  # shellcheck source=/dev/null
  source "$CONFIG"
  : "${OCI_NAMESPACE:?Falta OCI_NAMESPACE en $CONFIG}" "${OCI_BUCKET:?Falta OCI_BUCKET en $CONFIG}"
  oci_os() { oci os object "$@" --auth instance_principal --namespace "$OCI_NAMESPACE" --bucket-name "$OCI_BUCKET"; }
  objeto="${1:-}"
  if [ -z "$objeto" ]; then
    # Los nombres llevan la fecha en UTC, así que el último en orden alfabético es el más reciente.
    objeto=$(oci_os list --prefix postgres/ --all --query 'max_by(data, &name).name' --raw-output)
  fi
  oci_os get --name "$objeto" --file "$archivo" > /dev/null
  origen="$objeto"
fi

echo "Restaurando $origen ($(stat -c %s "$archivo") bytes) en la base $DESTINO"
docker exec "$CONTENEDOR" dropdb --if-exists --username="$BASE" "$DESTINO"
docker exec "$CONTENEDOR" createdb --username="$BASE" "$DESTINO"
docker exec -i "$CONTENEDOR" pg_restore --no-owner --exit-on-error --username="$BASE" --dbname="$DESTINO" < "$archivo"

# Filas por tabla de una base, como "tabla|filas".
filas() {
  docker exec "$CONTENEDOR" psql --username="$BASE" --dbname="$1" --no-align --tuples-only --command="
    SELECT table_name,
           (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), false, true, '')))[1]::text
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    ORDER BY table_name"
}

echo
printf '%-24s %12s %12s\n' "tabla" "produccion" "restaurada"
join -t '|' -a 1 -a 2 -e '-' -o '0,1.2,2.2' <(filas "$BASE") <(filas "$DESTINO") |
  while IFS='|' read -r tabla prod rest; do printf '%-24s %12s %12s\n' "$tabla" "$prod" "$rest"; done
echo
echo "La base $DESTINO queda para revisarla. Se borra con:"
echo "  docker exec $CONTENEDOR dropdb --username=$BASE $DESTINO"
