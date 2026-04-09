# Восстановление из дампа (осторожно: перезапишет данные в БД).
param(
    [Parameter(Mandatory = $true)][string]$SqlFile,
    [string]$ContainerName = "my_university_mysql",
    [string]$User = "root",
    [string]$Password = "root"
)
Get-Content $SqlFile -Raw | docker exec -i $ContainerName mysql -u$User -p$Password
Write-Host "Restore completed from $SqlFile"
