param(
    [switch]$Format,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Files
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$version = "1.8.0"
$expectedSha256 = "A3FD620207D5C40DA6CA789B95E7F823C54E854B7FADE7F613E91096A3706D75"
$repoRoot = Split-Path -Parent $PSScriptRoot
$cacheDir = Join-Path $repoRoot ".cache/ktlint"
$ktlintJar = Join-Path $cacheDir "ktlint-$version.jar"
$downloadUrl = "https://github.com/ktlint/ktlint/releases/download/$version/ktlint"

function Assert-KtlintHash {
    param([string]$Path)

    $actualSha256 = Get-FileSha256 -Path $Path
    if ($actualSha256 -ne $expectedSha256) {
        throw "ktlint $version SHA-256 mismatch. expected=$expectedSha256 actual=$actualSha256"
    }
}

function Get-FileSha256 {
    param([string]$Path)

    if (Get-Command Get-FileHash -ErrorAction SilentlyContinue) {
        return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToUpperInvariant()
    }

    $certutilOutput = & certutil -hashfile $Path SHA256
    if ($LASTEXITCODE -ne 0) {
        throw "certutil failed to calculate SHA-256 for $Path"
    }

    $hashLine = $certutilOutput |
        Where-Object { $_ -match "^[0-9A-Fa-f ]{64,}$" } |
        Select-Object -First 1
    if (-not $hashLine) {
        throw "certutil SHA-256 output was not recognized for $Path"
    }
    return ($hashLine -replace "\s", "").ToUpperInvariant()
}

function Ensure-Ktlint {
    if (Test-Path -LiteralPath $ktlintJar) {
        Assert-KtlintHash -Path $ktlintJar
        return
    }

    New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
    $tempPath = "$ktlintJar.download"
    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $tempPath
        Assert-KtlintHash -Path $tempPath
        Move-Item -LiteralPath $tempPath -Destination $ktlintJar -Force
    } catch {
        if (Test-Path -LiteralPath $tempPath) {
            Remove-Item -LiteralPath $tempPath -Force
        }
        throw
    }
}

if ($Files.Count -eq 0) {
    Write-Host "No Kotlin files for ktlint."
    exit 0
}

Ensure-Ktlint

$ktlintArgs = @("-jar", $ktlintJar, "--relative")
if ($Format) {
    $ktlintArgs += "--format"
}
$ktlintArgs += $Files

& java @ktlintArgs
exit $LASTEXITCODE
