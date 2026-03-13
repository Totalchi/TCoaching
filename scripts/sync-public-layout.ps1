param(
    [string]$SourceDir = "src/main/resources/static"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$staticDir = Join-Path $repoRoot $SourceDir
$partialsDir = Join-Path $staticDir "_partials"
$headerTemplatePath = Join-Path $partialsDir "site-header.html"
$footerTemplatePath = Join-Path $partialsDir "site-footer.html"
$regexOptions = [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Singleline

$publicPages = @(
    "index.html",
    "about.html",
    "life-coaching.html",
    "personal-training.html",
    "stress-burnout.html",
    "assertiviteit.html",
    "prijzen.html",
    "inzichten.html",
    "contact.html",
    "privacy.html"
)

$navTrackIds = @{
    "index.html" = "nav_intake_home"
    "about.html" = "nav_intake_about"
    "life-coaching.html" = "nav_intake_life"
    "personal-training.html" = "nav_intake_pt"
    "stress-burnout.html" = "nav_intake_stress"
    "assertiviteit.html" = "nav_intake_assertive"
    "prijzen.html" = "nav_intake_pricing"
    "inzichten.html" = "nav_intake_insights"
    "contact.html" = "nav_intake_contact"
    "privacy.html" = "nav_intake_privacy"
}

if (-not (Test-Path $headerTemplatePath)) {
    throw "Header template not found: $headerTemplatePath"
}

if (-not (Test-Path $footerTemplatePath)) {
    throw "Footer template not found: $footerTemplatePath"
}

$headerTemplate = Get-Content -Raw -Path $headerTemplatePath
$footerTemplate = Get-Content -Raw -Path $footerTemplatePath

function Normalize-DocumentContent {
    param([string]$Content)

    $doctypeMatches = [regex]::Matches($Content, '<!doctype html>', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if ($doctypeMatches.Count -le 1) {
        return $Content
    }

    $lastDoctype = $doctypeMatches[$doctypeMatches.Count - 1]
    return $Content.Substring($lastDoctype.Index)
}

foreach ($page in $publicPages) {
    $pagePath = Join-Path $staticDir $page
    if (-not (Test-Path $pagePath)) {
        throw "Public page not found: $pagePath"
    }

    $headerMarkup = $headerTemplate.Replace('href="../', 'href="').Replace('{{NAV_TRACK_ID}}', $navTrackIds[$page])
    $footerMarkup = $footerTemplate.Replace('href="../', 'href="')
    $content = Normalize-DocumentContent -Content (Get-Content -Raw -Path $pagePath)
    if (-not [regex]::IsMatch($content, '<header>.*?</header>', $regexOptions) -or -not [regex]::IsMatch($content, '<footer\b[^>]*class="footer"[^>]*>.*?</footer>', $regexOptions)) {
        throw "Expected header/footer blocks were not found in $page"
    }
    $updated = [regex]::Replace($content, '<header>.*?</header>', $headerMarkup, $regexOptions)
    $updated = [regex]::Replace($updated, '<footer\b[^>]*class="footer"[^>]*>.*?</footer>', $footerMarkup, $regexOptions)

    Set-Content -Path $pagePath -Value $updated -Encoding UTF8
}
