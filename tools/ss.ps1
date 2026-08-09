$ProjectCheckCommand = "secret-scan"
. "$PSScriptRoot\Invoke-homecheckProjectCheck.ps1"
$ProjectCheckScript = Resolve-homecheckProjectCheck
$ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path

& $ProjectCheckScript -ProjectCheckCommand $ProjectCheckCommand -Root $ProjectRoot -ProjectId "homecheck" @args
exit $LASTEXITCODE
