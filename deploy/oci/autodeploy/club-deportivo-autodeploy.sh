#!/usr/bin/env bash
# Despliegue automático de la API de Club deportivo en la VM (lo ejecuta un temporizador de systemd).
#
# El workflow "Despliegue del backend" publica en GHCR la imagen de cada etiqueta de versión y mueve
# :stable a esa versión. Si :stable cambió, este script la despliega fijada por digest y espera a que
# el contenedor quede healthy. Si no lo logra, vuelve a la versión anterior y recuerda el digest fallido
# para no reintentarlo en cada ciclo.
#
# Flyway migra la base al arrancar la API. El rollback vuelve a la imagen anterior, pero no deshace una
# migración: por eso cada migración tiene que ser compatible con la versión anterior de la API.
set -euo pipefail

IMAGEN="ghcr.io/leandromelchiori/club-deportivo-api"
ORIGEN="https://github.com/LeandroMelchiori/App_Clubdeportivo"
DIR="/opt/apps/apps/club-deportivo"
RUNTIME_ENV="/etc/docker-platform/apps/club-deportivo/runtime.env"
CONTENEDOR="club-deportivo-api-1"
ESTADO="/var/lib/club-deportivo-autodeploy"

mkdir -p "$ESTADO"
cd "$DIR"
compose() { docker compose --env-file .env --env-file "$RUNTIME_ENV" "$@"; }

docker pull --quiet "$IMAGEN:stable" >/dev/null
nueva=$(docker image inspect --format '{{range .RepoDigests}}{{println .}}{{end}}' "$IMAGEN:stable" | grep "^$IMAGEN@" | head -n 1)
actual=$(sed -n 's/^CLUB_API_IMAGE=//p' .env)

if [ -z "$nueva" ] || [ "$nueva" = "$actual" ]; then
  exit 0
fi
if [ "$nueva" = "$(cat "$ESTADO/fallida" 2>/dev/null || true)" ]; then
  exit 0
fi

echo "Desplegando $nueva (antes: ${actual:-ninguna})"
sed -i "s|^CLUB_API_IMAGE=.*|CLUB_API_IMAGE=$nueva|" .env
compose up -d

# La API tarda alrededor de un minuto en arrancar con medio núcleo ARM.
for _ in $(seq 1 36); do
  estado=$(docker inspect --format '{{.State.Health.Status}}' "$CONTENEDOR" 2>/dev/null || echo desconocido)
  if [ "$estado" = "healthy" ]; then
    echo "Desplegado: $nueva"
    rm -f "$ESTADO/fallida"
    # Borra solo versiones viejas de esta API que ya no usa ningún contenedor.
    docker image prune --force --filter "label=org.opencontainers.image.source=$ORIGEN" >/dev/null
    exit 0
  fi
  sleep 5
done

echo "La versión nueva no quedó healthy" >&2
echo "$nueva" > "$ESTADO/fallida"
if [ -n "$actual" ]; then
  echo "Vuelvo a $actual" >&2
  sed -i "s|^CLUB_API_IMAGE=.*|CLUB_API_IMAGE=$actual|" .env
  compose up -d
fi
exit 1
