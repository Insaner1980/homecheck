#Requires -Version 5.1

function Assert-homecheckProjectCheckEngine {
    param(
        [Parameter(Mandatory)]
        [string]$ProjectCheckScript
    )

    $modulePath = Join-Path (Split-Path -Parent $ProjectCheckScript) "AndroidProjectChecks.psm1"
    if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
        throw "ANDROID_CHECK_ENGINE_INVALID: AndroidProjectChecks.psm1 is missing beside '$ProjectCheckScript'."
    }

    $scriptTokens = $null
    $scriptErrors = $null
    $scriptAst = [System.Management.Automation.Language.Parser]::ParseFile(
        $ProjectCheckScript,
        [ref]$scriptTokens,
        [ref]$scriptErrors
    )
    $moduleTokens = $null
    $moduleErrors = $null
    $moduleAst = [System.Management.Automation.Language.Parser]::ParseFile(
        $modulePath,
        [ref]$moduleTokens,
        [ref]$moduleErrors
    )
    if ($scriptErrors.Count -gt 0 -or $moduleErrors.Count -gt 0) {
        throw "ANDROID_CHECK_ENGINE_INVALID: The Android-check engine contains PowerShell syntax errors."
    }

    $parameterNames = @($scriptAst.ParamBlock.Parameters | ForEach-Object { $_.Name.VariablePath.UserPath })
    foreach ($requiredParameter in @("ProjectCheckCommand", "Root", "ProjectId", "ResolveOnly", "PlanOnly")) {
        if ($requiredParameter -notin $parameterNames) {
            throw "ANDROID_CHECK_ENGINE_INVALID: '$ProjectCheckScript' does not declare parameter '$requiredParameter'."
        }
    }

    $commandNames = @($scriptAst.FindAll({
        param($node)
        $node -is [System.Management.Automation.Language.CommandAst]
    }, $true) | ForEach-Object { $_.GetCommandName() })
    $invokeFunction = $moduleAst.Find({
        param($node)
        $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
            $node.Name -eq "Invoke-AndroidProjectCheck"
    }, $true)
    if ("Import-Module" -notin $commandNames -or "Invoke-AndroidProjectCheck" -notin $commandNames -or $null -eq $invokeFunction) {
        throw "ANDROID_CHECK_ENGINE_INVALID: '$ProjectCheckScript' does not expose the required Android-check command contract."
    }
}

function Resolve-homecheckProjectCheck {
    $projectRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
    $candidateRoots = New-Object System.Collections.Generic.List[string]

    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_CHECK_ROOT)) {
        $candidateRoots.Add($env:ANDROID_CHECK_ROOT)
    }

    $candidateRoots.Add("C:\Dev\Android-check")
    $candidateRoots.Add((Join-Path (Split-Path -Parent $projectRoot) "Android-check"))

    foreach ($candidateRoot in $candidateRoots) {
        $projectCheckScript = Join-Path $candidateRoot "tools\InvokeProjectCheck.ps1"
        if (Test-Path -LiteralPath $projectCheckScript -PathType Leaf) {
            $resolvedScript = (Resolve-Path -LiteralPath $projectCheckScript).Path
            Assert-homecheckProjectCheckEngine -ProjectCheckScript $resolvedScript
            return $resolvedScript
        }
    }

    throw "ANDROID_CHECK_ENGINE_NOT_FOUND: Set ANDROID_CHECK_ROOT to an Android-check checkout or install C:\Dev\Android-check."
}
