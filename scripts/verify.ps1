$ErrorActionPreference = 'Stop'

Write-Host 'Backend: compile, tests, and Flyway validation'
$repository = (Join-Path $env:TEMP 'erp-core-maven-repository') -replace '\\', '/'
$settingsPath = Join-Path $env:TEMP 'erp-core-maven-settings.xml'
$settingsXml = '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><localRepository>{0}</localRepository></settings>' -f $repository
Set-Content -LiteralPath $settingsPath -Value $settingsXml -Encoding utf8
mvn -s $settingsPath -B -ntp -f apps/api/pom.xml verify
if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }

Push-Location apps/web
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    npm run lint
    if ($LASTEXITCODE -ne 0) { throw 'Frontend lint/format check failed.' }
    npm test -- --watch=false
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
}
finally {
    Pop-Location
}
