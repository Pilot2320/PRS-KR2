#!/usr/bin/env bash
# Запуск КР №2: PostgreSQL из КР №1 в Docker, затем JavaFX-приложение.
set -euo pipefail
cd "$(dirname "$0")"

if ! docker info >/dev/null 2>&1; then
  echo "Docker не запущен. Откройте Docker Desktop и повторите."
  exit 1
fi

docker compose up -d --wait

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21+ 2>/dev/null || true)"
  export JAVA_HOME
fi

if command -v mvn >/dev/null 2>&1; then
  exec mvn -q javafx:run
fi
exec ./mvnw -q javafx:run
