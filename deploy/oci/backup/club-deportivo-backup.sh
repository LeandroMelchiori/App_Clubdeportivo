#!/usr/bin/env bash
# Backup diario de la base de Club deportivo en OCI Object Storage.
#
# Lo ejecuta club-deportivo-backup.timer como root en la VM. Se autentica con instance principal, así
# que la VM no guarda credenciales de OCI. La VM solo puede crear y leer objetos: no puede borrarlos ni
# sobrescribirlos. Los backups viejos los borra la regla de ciclo de vida del bucket.
#
# El log solo dice qué objeto se subió y cuánto pesa: nunca muestra datos de la base.
set -euo pipefail

CONFIG=/etc/docker-platform/apps/club-deportivo/backup.env
CONTENEDOR=club-deportivo-postgres-1
BASE=clubdeportivo

# Define OCI_NAMESPACE y OCI_BUCKET.
# shellcheck source=/dev/null
source "$CONFIG"
: "${OCI_NAMESPACE:?Falta OCI_NAMESPACE en $CONFIG}" "${OCI_BUCKET:?Falta OCI_BUCKET en $CONFIG}"

objeto="postgres/$BASE-$(date -u +%Y%m%dT%H%M%SZ).dump"
archivo=$(mktemp -p "${TMPDIR:-/var/tmp}" club-deportivo-backup.XXXXXX)
trap 'rm -f "$archivo"' EXIT

docker exec "$CONTENEDOR" pg_dump --format=custom --no-owner --username="$BASE" --dbname="$BASE" > "$archivo"
# Un dump que pg_restore no puede leer no sirve como backup: mejor fallar ahora que al restaurar.
docker exec -i "$CONTENEDOR" pg_restore --list < "$archivo" > /dev/null

oci os object put --auth instance_principal \
  --namespace "$OCI_NAMESPACE" --bucket-name "$OCI_BUCKET" \
  --name "$objeto" --file "$archivo" --no-overwrite --no-multipart > /dev/null

echo "Backup subido: $objeto ($(stat -c %s "$archivo") bytes)"
