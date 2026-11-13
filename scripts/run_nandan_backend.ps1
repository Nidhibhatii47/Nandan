$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $RepoRoot "backend"
$LogDir = Join-Path $BackendDir "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$VenvPython = Join-Path $BackendDir ".venv\Scripts\python.exe"
if (Test-Path $VenvPython) {
    $Python = $VenvPython
} else {
    $PythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if (-not $PythonCommand) {
        throw "Python was not found. Install Python 3.11+ or create backend\.venv first."
    }
    $Python = $PythonCommand.Source
}

Set-Location $BackendDir
$LogFile = Join-Path $LogDir "reminder-engine.log"
"[$(Get-Date -Format o)] Starting Nandan backend." | Out-File -FilePath $LogFile -Append -Encoding utf8
& $Python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 *>> $LogFile
$ExitCode = $LASTEXITCODE
"[$(Get-Date -Format o)] Backend exited with code $ExitCode." | Out-File -FilePath $LogFile -Append -Encoding utf8
exit $ExitCode
