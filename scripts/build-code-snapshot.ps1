param(
    [string]$OutputFile = "ALL_CODE.txt"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $repoRoot $OutputFile
$excludedDirectories = @(".git", ".idea", "target", "build", "docs")
$textExtensions = @(
    ".java", ".html", ".css", ".js", ".json", ".xml", ".properties",
    ".ps1", ".md", ".sql", ".txt", ".cmd", ".yml", ".yaml", ".svg",
    ".dockerignore", ".gitignore", ".gitattributes"
)
$allowedRootNames = @(
    "Dockerfile", "mvnw", "mvnw.cmd", "pom.xml", "railway.json",
    "HELP.md", ".env.example", ".env.local.properties.example", "PROJECT_OVERVIEW.md"
)
$sensitiveFiles = @(
    ".env.local.properties"
)

$files = Get-ChildItem -Path $repoRoot -Recurse -File | Where-Object {
    if ($_.FullName -eq $outputPath) {
        return $false
    }
    $relative = $_.FullName.Substring($repoRoot.Length + 1)
    $parts = $relative -split '[\\/]'
    if ($parts | Where-Object { $excludedDirectories -contains $_ }) {
        return $false
    }
    if ($sensitiveFiles -contains $_.Name) {
        return $false
    }
    return $textExtensions -contains $_.Extension.ToLowerInvariant() -or $allowedRootNames -contains $_.Name
} | Sort-Object FullName

$builder = New-Object System.Text.StringBuilder
[void]$builder.AppendLine("TCoaching code snapshot")
[void]$builder.AppendLine("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
[void]$builder.AppendLine("Included: source, config, scripts, docs markdown, text assets")
[void]$builder.AppendLine("Excluded: docs/, build/, target/, .idea/, .git/, binary assets, local secret files")
[void]$builder.AppendLine("")

foreach ($file in $files) {
    $relative = $file.FullName.Substring($repoRoot.Length + 1)
    [void]$builder.AppendLine(("=" * 100))
    [void]$builder.AppendLine("FILE: $relative")
    [void]$builder.AppendLine(("=" * 100))
    try {
        $content = Get-Content -Raw -Path $file.FullName
    } catch {
        $content = "[ERROR READING FILE]"
    }
    [void]$builder.AppendLine($content)
    if (-not $content.EndsWith("`n")) {
        [void]$builder.AppendLine("")
    }
    [void]$builder.AppendLine("")
}

[System.IO.File]::WriteAllText($outputPath, $builder.ToString(), [System.Text.Encoding]::UTF8)
