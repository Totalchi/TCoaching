param(
    [string]$OutputDir = "build/pages"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceDir = Join-Path $repoRoot "src/main/resources/static"
$targetDir = Join-Path $repoRoot $OutputDir
$layoutSyncScript = Join-Path $PSScriptRoot "sync-public-layout.ps1"
$regexOptions = [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Singleline

function Set-AttributeFromEnglishData {
    param(
        [string]$Content,
        [string]$TargetAttribute,
        [string]$DataAttribute
    )

    $pattern = "<(?<tag>[a-z0-9:-]+)\b(?<attrs>[^>]*\b$DataAttribute=""(?<en>[^""]*)""[^>]*)>"
    return [regex]::Replace($Content, $pattern, {
        param($match)

        $englishValue = $match.Groups["en"].Value
        $tagMarkup = $match.Value
        [regex]::Replace($tagMarkup, "\b$TargetAttribute=""[^""]*""", "$TargetAttribute=""$englishValue""")
    }, $regexOptions)
}

function Convert-PageCopyToEnglish {
    param([string]$Content)

    $converted = $Content
    $converted = [regex]::Replace($converted, '<html(?<before>[^>]*)\blang="nl"(?<after>[^>]*)>', {
        param($match)
        "<html$($match.Groups["before"].Value) lang=""en""$($match.Groups["after"].Value)>"
    }, $regexOptions)

    $converted = [regex]::Replace(
        $converted,
        '(?<open><(?<tag>[a-z0-9:-]+)\b[^>]*\bdata-lang-en="(?<lang>[^"]*)"[^>]*>)(?<body>.*?)(?<close></\k<tag>>)',
        {
            param($match)
            $match.Groups["open"].Value + $match.Groups["lang"].Value + $match.Groups["close"].Value
        },
        $regexOptions
    )

    $converted = Set-AttributeFromEnglishData -Content $converted -TargetAttribute "placeholder" -DataAttribute "data-placeholder-en"
    $converted = Set-AttributeFromEnglishData -Content $converted -TargetAttribute "content" -DataAttribute "data-content-en"
    $converted = Set-AttributeFromEnglishData -Content $converted -TargetAttribute "alt" -DataAttribute "data-alt-en"
    $converted = Set-AttributeFromEnglishData -Content $converted -TargetAttribute "aria-label" -DataAttribute "data-aria-label-en"
    $converted = Set-AttributeFromEnglishData -Content $converted -TargetAttribute "value" -DataAttribute "data-value-en"

    return $converted
}

function Update-RelativePathsForEnglish {
    param([string]$Content)

    $updated = $Content
    $updated = $updated -replace '((?:href|src)=")assets/', '$1../assets/'
    $updated = $updated -replace '(href=")site\.webmanifest"', '$1../site.webmanifest"'
    return $updated
}

function Add-SeoMetadata {
    param(
        [string]$Content,
        [string]$CanonicalUrl,
        [string]$NlUrl,
        [string]$EnUrl,
        [string]$SiteConfigPath,
        [string]$SiteBaseUrl
    )

    $updated = $Content
    $updated = $updated -replace 'content="/assets/img/og-card\.svg"', "content=""$SiteBaseUrl/assets/img/og-card.svg"""

    $languageLinks = @(
        "<link rel=""canonical"" href=""$CanonicalUrl"" />"
        "<link rel=""alternate"" hreflang=""nl"" href=""$NlUrl"" />"
        "<link rel=""alternate"" hreflang=""en"" href=""$EnUrl"" />"
        "<link rel=""alternate"" hreflang=""x-default"" href=""$NlUrl"" />"
    ) -join "`r`n  "

    if ($updated -notmatch 'rel="canonical"') {
        $updated = $updated -replace '(<meta name="theme-color"[^>]*>\s*)', "`$1  $languageLinks`r`n"
    }

    if ($updated -notmatch 'property="og:url"') {
        $ogUrl = "<meta property=""og:url"" content=""$CanonicalUrl"" />"
        $updated = $updated -replace '(<meta property="og:type"[^>]*>\s*)', "`$1  $ogUrl`r`n"
    }

    $updated = $updated -replace '<script src="assets/js/main\.js"></script>', "<script src=""$SiteConfigPath""></script>`r`n  <script src=""assets/js/main.js""></script>"
    $updated = $updated -replace '<script src="\.\./assets/js/main\.js"></script>', "<script src=""$SiteConfigPath""></script>`r`n  <script src=""../assets/js/main.js""></script>"

    return $updated
}

if (-not (Test-Path $sourceDir)) {
    throw "Static source directory not found: $sourceDir"
}

if (-not (Test-Path $layoutSyncScript)) {
    throw "Layout sync script not found: $layoutSyncScript"
}

& $layoutSyncScript

if (Test-Path $targetDir) {
    Remove-Item -Recurse -Force $targetDir
}

New-Item -ItemType Directory -Path $targetDir | Out-Null
Copy-Item -Path (Join-Path $sourceDir "*") -Destination $targetDir -Recurse -Force
New-Item -ItemType Directory -Path (Join-Path $targetDir "en") | Out-Null
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $targetDir "_partials")

$owner = if ($env:GITHUB_REPOSITORY_OWNER) { $env:GITHUB_REPOSITORY_OWNER.ToLowerInvariant() } else { "totalchi" }
$repoName = if ($env:GITHUB_REPOSITORY) { ($env:GITHUB_REPOSITORY -split "/")[-1] } else { "TCoaching" }
$customDomain = [string]::IsNullOrWhiteSpace($env:GH_PAGES_CNAME) -eq $false

if ($customDomain) {
    $siteBaseUrl = "https://$($env:GH_PAGES_CNAME.Trim())"
} else {
    $siteBaseUrl = "https://$($owner).github.io/$repoName"
}

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

$configJs = @"
/**
 * Static runtime config for exported GitHub Pages builds.
 * - apiEnabled: disables direct backend calls in the preview build
 * - contactEmail: fallback contact address shown in the UI
 * - bookingUrl: optional direct booking URL (Calendly, Acuity, ...)
 * - trackingWindowMs: pageview deduplication window in milliseconds
 * - mode: runtime label for static-preview behavior
 */
window.TCOACHING_CONFIG = {
  configVersion: 1,
  apiEnabled: false,
  contactEmail: 'hello@tcoaching.be',
  bookingUrl: '',
  trackingWindowMs: 900000,
  mode: 'github-pages'
};
"@
Set-Content -Path (Join-Path $targetDir "assets/js/site-config.js") -Value $configJs -Encoding UTF8

foreach ($page in $publicPages) {
    $sourcePagePath = Join-Path $sourceDir $page
    $rootPagePath = Join-Path $targetDir $page
    $englishPagePath = Join-Path (Join-Path $targetDir "en") $page

    $rootUrl = if ($page -eq "index.html") { "$siteBaseUrl/" } else { "$siteBaseUrl/$page" }
    $englishUrl = if ($page -eq "index.html") { "$siteBaseUrl/en/" } else { "$siteBaseUrl/en/$page" }

    $rootContent = Get-Content -Raw -Path $sourcePagePath
    $rootContent = Add-SeoMetadata -Content $rootContent -CanonicalUrl $rootUrl -NlUrl $rootUrl -EnUrl $englishUrl -SiteConfigPath "assets/js/site-config.js" -SiteBaseUrl $siteBaseUrl
    Set-Content -Path $rootPagePath -Value $rootContent -Encoding UTF8

    $englishContent = Get-Content -Raw -Path $sourcePagePath
    $englishContent = Convert-PageCopyToEnglish -Content $englishContent
    $englishContent = Update-RelativePathsForEnglish -Content $englishContent
    $englishContent = Add-SeoMetadata -Content $englishContent -CanonicalUrl $englishUrl -NlUrl $rootUrl -EnUrl $englishUrl -SiteConfigPath "../assets/js/site-config.js" -SiteBaseUrl $siteBaseUrl
    Set-Content -Path $englishPagePath -Value $englishContent -Encoding UTF8
}

Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $targetDir "admin.html")
Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $targetDir "assets/js/admin.js")
New-Item -ItemType File -Path (Join-Path $targetDir ".nojekyll") -Force | Out-Null
Copy-Item -Path (Join-Path $targetDir "index.html") -Destination (Join-Path $targetDir "404.html") -Force

$manifest = @"
{
  "name": "TCoaching",
  "short_name": "TCoaching",
  "start_url": "./index.html",
  "scope": "./",
  "display": "standalone",
  "background_color": "#f5f1ea",
  "theme_color": "#f5f1ea",
  "icons": [
    {
      "src": "assets/img/favicon.svg",
      "sizes": "any",
      "type": "image/svg+xml"
    }
  ]
}
"@
Set-Content -Path (Join-Path $targetDir "site.webmanifest") -Value $manifest -Encoding UTF8

$today = Get-Date -Format "yyyy-MM-dd"
$sitemapItems = foreach ($page in $publicPages) {
    $rootLoc = if ($page -eq "index.html") { "$siteBaseUrl/" } else { "$siteBaseUrl/$page" }
    $englishLoc = if ($page -eq "index.html") { "$siteBaseUrl/en/" } else { "$siteBaseUrl/en/$page" }
    @"
  <url>
    <loc>$rootLoc</loc>
    <lastmod>$today</lastmod>
  </url>
  <url>
    <loc>$englishLoc</loc>
    <lastmod>$today</lastmod>
  </url>
"@
}

$sitemap = @"
<?xml version="1.0" encoding="UTF-8"?>
<urlset>
$(($sitemapItems -join "`r`n"))
</urlset>
"@
Set-Content -Path (Join-Path $targetDir "sitemap.xml") -Value $sitemap -Encoding UTF8

$robots = @"
User-agent: *
Allow: /

Sitemap: $siteBaseUrl/sitemap.xml
"@
Set-Content -Path (Join-Path $targetDir "robots.txt") -Value $robots -Encoding UTF8

if ($customDomain) {
    Set-Content -Path (Join-Path $targetDir "CNAME") -Value $env:GH_PAGES_CNAME.Trim() -Encoding ASCII
}
