# Резервная копия MySQL (docker-compose: контейнер my_university_mysql, БД my_university).
# Требуется: Docker, запущенный контейнер или доступ к mysqldump.
param(
    [string]$ContainerName = "my_university_mysql",
    [string]$Database = "my_university",
    [string]$User = "root",
    [string]$Password = "root",
    [string]$OutDir = "."
)
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$out = Join-Path $OutDir "my_university_backup_$ts.sql"
docker exec $ContainerName mysqldump -u$User -p$Password --single-transaction --routines --databases $Database | Out-File -FilePath $out -Encoding utf8
Write-Host "Saved: $out"
