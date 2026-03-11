param(
    [string]$OutputDir = "build/pages"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceDir = Join-Path $repoRoot "src/main/resources/static"
$targetDir = Join-Path $repoRoot $OutputDir

if (-not (Test-Path $sourceDir)) {
    throw "Static source directory not found: $sourceDir"
}

if (Test-Path $targetDir) {
    Remove-Item -Recurse -Force $targetDir
}

New-Item -ItemType Directory -Path $targetDir | Out-Null
Copy-Item -Path (Join-Path $sourceDir "*") -Destination $targetDir -Recurse -Force

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
window.TCOACHING_CONFIG = {
  apiEnabled: false,
  contactEmail: 'hello@tcoaching.be',
  bookingUrl: '',
  mode: 'github-pages'
};
"@
Set-Content -Path (Join-Path $targetDir "assets/js/site-config.js") -Value $configJs -Encoding UTF8

foreach ($page in $publicPages) {
    $pagePath = Join-Path $targetDir $page
    $content = Get-Content -Raw -Path $pagePath

    $pageUrl = if ($page -eq "index.html") { "$siteBaseUrl/" } else { "$siteBaseUrl/$page" }
    $content = $content -replace 'content="/assets/img/og-card\.svg"', "content=""$siteBaseUrl/assets/img/og-card.svg"""

    if ($content -notmatch 'rel="canonical"') {
        $canonical = "<link rel=""canonical"" href=""$pageUrl"" />"
        $content = $content -replace '(<meta name="theme-color"[^>]*>\s*)', "`$1  $canonical`r`n"
    }

    if ($content -notmatch 'property="og:url"') {
        $ogUrl = "<meta property=""og:url"" content=""$pageUrl"" />"
        $content = $content -replace '(<meta property="og:type"[^>]*>\s*)', "`$1  $ogUrl`r`n"
    }

    $content = $content -replace '<script src="assets/js/main\.js"></script>', "<script src=""assets/js/site-config.js""></script>`r`n  <script src=""assets/js/main.js""></script>"
    Set-Content -Path $pagePath -Value $content -Encoding UTF8
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
    $loc = if ($page -eq "index.html") { "$siteBaseUrl/" } else { "$siteBaseUrl/$page" }
    @"
  <url>
    <loc>$loc</loc>
    <lastmod>$today</lastmod>
  </url>
"@
}

$sitemap = @"
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
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
