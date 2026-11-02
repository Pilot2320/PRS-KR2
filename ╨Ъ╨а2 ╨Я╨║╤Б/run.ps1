$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker не запущен. Откройте Docker Desktop и повторите."
    exit 1
}

docker compose up -d --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn -q javafx:run
} else {
    .\mvnw.cmd -q javafx:run
}
