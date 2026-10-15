$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    throw 'Set JAVA_HOME to JDK 17 or 21 before initializing release signing.'
}
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $keytool)) {
    throw 'JAVA_HOME does not contain keytool.exe.'
}
$directory = Join-Path $env:LOCALAPPDATA 'WorkPause\signing'
$keystore = Join-Path $directory 'workpause-release.p12'
$passwordFile = Join-Path $directory 'release-password.dpapi'
if ((Test-Path -LiteralPath $keystore) -or (Test-Path -LiteralPath $passwordFile)) {
    throw 'Signing material already exists. It will not be overwritten or regenerated.'
}
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $directory /inheritance:r /grant:r "*${sid}:(OI)(CI)F"
if ($LASTEXITCODE -ne 0) {
    throw 'Could not restrict the signing directory permissions.'
}
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
} finally {
    $rng.Dispose()
}
$password = [Convert]::ToBase64String($bytes)
$previous = $env:WORKPAUSE_KEYTOOL_PASSWORD
try {
    $password | ConvertTo-SecureString -AsPlainText -Force |
        ConvertFrom-SecureString | Set-Content -LiteralPath $passwordFile -Encoding ASCII
    $env:WORKPAUSE_KEYTOOL_PASSWORD = $password
    & $keytool -genkeypair -keystore $keystore -storetype PKCS12 `
        -storepass:env WORKPAUSE_KEYTOOL_PASSWORD -keypass:env WORKPAUSE_KEYTOOL_PASSWORD `
        -alias workpause -keyalg RSA -keysize 4096 -validity 10000 `
        -dname 'CN=WorkPause, OU=Mobile Apps' -noprompt
    if ($LASTEXITCODE -ne 0) {
        throw 'Release key generation failed. Inspect the signing directory before retrying.'
    }
    & $keytool -exportcert -rfc -keystore $keystore -storetype PKCS12 `
        -storepass:env WORKPAUSE_KEYTOOL_PASSWORD -alias workpause `
        -file (Join-Path $directory 'workpause-release-certificate.pem')
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not export the public signing certificate.'
    }
} finally {
    $env:WORKPAUSE_KEYTOOL_PASSWORD = $previous
    $password = $null
    [Array]::Clear($bytes, 0, $bytes.Length)
}
Write-Host "Release key created outside the repository: $keystore"
Write-Warning 'Arrange a protected keystore backup and separate password recovery before distribution. The DPAPI file is tied to this Windows account and machine.'
