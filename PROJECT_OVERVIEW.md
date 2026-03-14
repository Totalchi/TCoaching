# TCoaching Project Overview

## 1. Project Summary

TCoaching is a bilingual coaching website and Spring Boot application for a coaching practice. The project combines:

- a public marketing site with multiple service pages
- a contact and intake flow
- optional direct booking support
- pageview tracking
- an admin area for leads and dashboard data
- a static export path for GitHub Pages

There are effectively two ways this project can run:

1. Full application mode  
   Spring Boot serves the website, API endpoints, admin page, tracking, captcha-backed contact flow, and database-backed features.

2. Static export mode  
   The public site is exported to `docs/` or `build/pages/` for GitHub Pages. In this mode the backend APIs are disabled and the site behaves as a static frontend.

## 2. Tech Stack

- Backend: Spring Boot, see [pom.xml](pom.xml) for the current version
- Language: Java, see [pom.xml](pom.xml) for the current version
- Build tool: Maven
- Templating: Thymeleaf for admin page
- Database: MySQL in app mode, H2 for tests
- Migrations: Flyway
- Security: Spring Security
- Mail: Spring Mail
- Frontend: static HTML, CSS, vanilla JavaScript
- Deployment options: Docker / Railway / static export

Key build file:

- [pom.xml](pom.xml)

Version note:

- Treat [pom.xml](pom.xml) as the single source of truth for the current Spring Boot and Java versions.
- If this document and `pom.xml` ever disagree, `pom.xml` is the authoritative version.

## 3. Local Development Prerequisites

Before running the full application locally, make sure the machine has:

- Java installed in the version required by [pom.xml](pom.xml)
- Maven Wrapper support available through `mvnw` or `mvnw.cmd`
- a local MySQL instance for app mode
- PowerShell available for the sync/export scripts

Recommended local setup:

- install the Java version declared in `pom.xml`
- run MySQL locally, typically on port `3306`
- copy [.env.example](.env.example) to `.env.local.properties`
- fill in local values for database, admin login, mail, and optional captcha/booking settings

Notes:

- the application imports `.env.local.properties` through `spring.config.import`
- tests do not require your local MySQL setup because they use H2
- if you only want to inspect the static frontend, exporting `docs/` is enough

## 4. Repository Structure

```text
TCoaching/
|- src/
|  |- main/
|  |  |- java/be/vdab/tcoaching/
|  |  |  |- admin/
|  |  |  |- api/
|  |  |  |- config/
|  |  |- resources/
|  |  |  |- application.properties
|  |  |  |- application-prod.properties
|  |  |  |- db/migration/
|  |  |  |- static/
|  |  |  |  |- *.html
|  |  |  |  |- assets/css/
|  |  |  |  |- assets/js/
|  |  |  |  |- assets/img/
|  |  |  |  |- _partials/
|  |  |  |- templates/admin.html
|  |- test/
|- scripts/
|  |- sync-public-layout.ps1
|  |- export-pages.ps1
|- docs/
|- build/pages/
|- Dockerfile
|- railway.json
|- HELP.md
```

## 5. Public Site Pages

The public pages live in [src/main/resources/static](src/main/resources/static).

Main pages:

- `index.html`: homepage
- `about.html`: about page
- `life-coaching.html`
- `personal-training.html`
- `stress-burnout.html`
- `assertiviteit.html`
- `prijzen.html`: pricing and entry point
- `inzichten.html`: insights/content page
- `contact.html`: intake and contact page
- `privacy.html`

Support files:

- `robots.txt`
- `sitemap.xml`
- `site.webmanifest`

Generated static output:

- [docs](docs): GitHub Pages-ready output
- [build/pages](build/pages): alternate export target

Important rule:

- Edit the source pages in `src/main/resources/static`
- Do not manually maintain `docs/` or `build/pages/`; those are generated

## 6. Frontend Architecture

### HTML

The public site is built as standalone HTML pages rather than a frontend framework application.

Shared layout is centralized through partials:

- [site-header.html](src/main/resources/static/_partials/site-header.html)
- [site-footer.html](src/main/resources/static/_partials/site-footer.html)

Those partials are injected into all public pages by the sync script before export.

Important workflow note:

- partials are not runtime includes
- changing `_partials/` does not automatically update the public HTML source files
- after every change to `_partials/`, run [sync-public-layout.ps1](scripts/sync-public-layout.ps1) if you want the updated header/footer reflected in the source pages
- [export-pages.ps1](scripts/export-pages.ps1) runs layout sync automatically before generating `docs/` or `build/pages/`

### CSS

Main stylesheet files:

- [styles.css](src/main/resources/static/assets/css/styles.css): main entry CSS
- [base.css](src/main/resources/static/assets/css/base.css): variables, fonts, base styles, utilities
- [layout.css](src/main/resources/static/assets/css/layout.css): shared layout and component styling
- [nav.css](src/main/resources/static/assets/css/components/nav.css): navigation styling
- [contact.css](src/main/resources/static/assets/css/pages/contact.css): contact page and booking UI
- [admin.css](src/main/resources/static/assets/css/pages/admin.css): admin UI

### JavaScript

Main frontend logic:

- [main.js](src/main/resources/static/assets/js/main.js)

This script handles:

- language switching
- theme switching
- local/session storage handling with safe fallbacks
- intake modal logic
- contact form behavior
- tracking requests
- public config loading
- booking link/embed activation
- active nav state
- visual interaction logic for cards and page behavior

Admin-specific logic:

- [admin.js](src/main/resources/static/assets/js/admin.js)

## 7. Language and SEO Model

The site is bilingual: Dutch and English.

Current approach:

- Dutch is the base source in the HTML
- English content is stored in HTML attributes like `data-lang-en`, `data-content-en`, `data-placeholder-en`, `data-alt-en`, and `data-aria-label-en`
- `main.js` swaps visible/runtime text in app mode
- `export-pages.ps1` generates real English static pages under `docs/en/`

This means:

- page copy and SEO-sensitive metadata live in HTML
- runtime-only status strings live in JavaScript
- static export produces separate EN pages with their own metadata and canonical/hreflang links

## 8. Booking and Contact Flow

The contact and intake flow is centered around [contact.html](src/main/resources/static/contact.html).

Main user journeys:

- free intake request
- waiting list request
- direct booking when `BOOKING_URL` is configured

Relevant runtime pieces:

- `main.js` loads public config from `/api/public-config`
- if a `bookingUrl` exists, the direct booking UI becomes active
- if not, the form remains the fallback route

The pricing page also contains clickable pricing cards that open the intake flow.

Backend endpoint:

- `POST /api/contact`

Relevant backend classes:

- [ContactController.java](src/main/java/be/vdab/tcoaching/api/contact/ContactController.java)
- [ContactRequest.java](src/main/java/be/vdab/tcoaching/api/contact/ContactRequest.java)
- [CaptchaVerificationService.java](src/main/java/be/vdab/tcoaching/api/contact/CaptchaVerificationService.java)
- [EmailNotificationService.java](src/main/java/be/vdab/tcoaching/api/contact/EmailNotificationService.java)

## 9. Tracking and Analytics

Pageview tracking is available in full app mode.

Backend endpoint:

- `POST /api/track`

Relevant classes:

- [TrackingController.java](src/main/java/be/vdab/tcoaching/api/tracking/TrackingController.java)
- [PageViewRequest.java](src/main/java/be/vdab/tcoaching/api/tracking/PageViewRequest.java)
- [AnalyticsCleanupJob.java](src/main/java/be/vdab/tcoaching/config/AnalyticsCleanupJob.java)

Frontend behavior:

- tracking is deduplicated per tab/session window
- tracking window is configurable via runtime config
- fallback storage is used if browser storage is blocked

## 10. Admin Area

The project includes a protected admin area.

Routes:

- `GET /admin`
- `GET /api/admin/dashboard`
- `GET /api/admin/contacts`

Relevant files:

- [AdminPageController.java](src/main/java/be/vdab/tcoaching/admin/AdminPageController.java)
- [AdminDashboardController.java](src/main/java/be/vdab/tcoaching/api/admin/AdminDashboardController.java)
- [admin.html](src/main/resources/templates/admin.html)
- [admin.js](src/main/resources/static/assets/js/admin.js)

Security is configured through:

- [SecurityConfig.java](src/main/java/be/vdab/tcoaching/config/SecurityConfig.java)

## 11. Backend Configuration

Main application config:

- [application.properties](src/main/resources/application.properties)
- [application-prod.properties](src/main/resources/application-prod.properties)

Important configuration areas:

- server port and compression
- datasource and Flyway
- admin credentials
- captcha
- booking URL
- rate limiting
- mail delivery
- analytics retention

Important environment variables include:

- `PORT`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `ADMIN_USER`
- `ADMIN_PASSWORD`
- `CAPTCHA_ENABLED`
- `CAPTCHA_SITE_KEY`
- `CAPTCHA_SECRET`
- `BOOKING_URL`
- `MAIL_USER`
- `MAIL_PASSWORD`
- `MAIL_TO`
- `MAIL_FROM`
- `SPRING_PROFILES_ACTIVE`

Local overrides can be loaded from:

- `.env.local.properties`

Suggested starting point:

- copy [.env.example](.env.example) to `.env.local.properties`
- or use [.env.local.properties.example](.env.local.properties.example) if you want a file named exactly like the imported local config

Minimal local example:

```properties
SPRING_PROFILES_ACTIVE=
DB_URL=jdbc:mysql://127.0.0.1:3306/tcoaching?allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=your_local_db_user
DB_PASSWORD=your_local_db_password
ADMIN_USER=your_admin_user
ADMIN_PASSWORD=your_admin_password
CAPTCHA_ENABLED=false
BOOKING_URL=
MAIL_USER=
MAIL_PASSWORD=
MAIL_TO=
MAIL_FROM=
```

## 12. Database and Migrations

Schema management uses Flyway.

Migration folders:

- [db/migration/common](src/main/resources/db/migration/common)
- [db/migration/h2](src/main/resources/db/migration/h2)
- [db/migration/mysql](src/main/resources/db/migration/mysql)

This setup allows:

- shared migrations across databases
- test-specific data-quality steps for H2
- MySQL-specific migration variants where needed

## 13. Scripts and Generated Outputs

### Layout sync

Script:

- [sync-public-layout.ps1](scripts/sync-public-layout.ps1)

Purpose:

- inject shared header/footer partials into all public HTML pages
- normalize corrupted files if multiple `<!doctype html>` blocks appear
- keep navigation tracking IDs page-specific

### Static export

Script:

- [export-pages.ps1](scripts/export-pages.ps1)

Purpose:

- sync layout first
- copy public static files to an output directory
- generate `docs/en/` or `build/pages/en/`
- convert copy and attributes to English in static pages
- add canonical, hreflang, OG URL, and site-config metadata
- remove admin artifacts from static output
- generate `.nojekyll`, `404.html`, `sitemap.xml`, `robots.txt`, and `site.webmanifest`

Script runtime note:

- these scripts require PowerShell
- on Windows you can use `powershell`
- on macOS or Linux you need PowerShell 7+ (`pwsh`) installed
- the scripts are intended to be cross-platform PowerShell scripts, but they should be verified on the target OS if your team is not developing on Windows

Important generated directories:

- [docs](docs)
- [build/pages](build/pages)

## 14. Build and Run

### Run the full app locally

```powershell
.\mvnw.cmd spring-boot:run
```

### Run tests

```powershell
.\mvnw.cmd test
```

### Package the app

```powershell
.\mvnw.cmd package
```

### Generate static export

```powershell
powershell -ExecutionPolicy Bypass -File scripts/export-pages.ps1 -OutputDir docs
```

or

```powershell
powershell -ExecutionPolicy Bypass -File scripts/export-pages.ps1 -OutputDir build/pages
```

## 15. Deployment

### Full application deployment

Container deployment is supported through:

- [Dockerfile](Dockerfile)
- [railway.json](railway.json)

Docker build flow:

- Maven builds the JAR in a build stage
- the runtime image runs `app.jar` on port `8080`

Railway settings:

- healthcheck path: `/actuator/health`
- restart policy: always

Recommended use:

- use full app mode for real production deployments where contact forms, tracking, captcha, admin, and booking config need to work end-to-end
- Railway is the intended deployment target for that mode

### Static deployment

The public site can also be exported and hosted as a static site.

Important limitation:

- static mode does not provide live backend APIs
- forms, tracking, admin, and captcha-backed server behavior are limited or disabled in that mode

Recommended use:

- use static export for GitHub Pages previews, shareable demos, review links, or moments where you only need the public marketing site
- do not use static export as the primary production mode if you need working backend features

## 16. Testing

Test sources live in [src/test/java](src/test/java).

Current tests include:

- [ContactControllerTests.java](src/test/java/be/vdab/tcoaching/ContactControllerTests.java)
- [SecurityConfigTests.java](src/test/java/be/vdab/tcoaching/SecurityConfigTests.java)
- [TrackingControllerTests.java](src/test/java/be/vdab/tcoaching/TrackingControllerTests.java)

Shared test base:

- [AbstractWebMvcTest.java](src/test/java/be/vdab/tcoaching/AbstractWebMvcTest.java)

How to run tests:

```powershell
.\mvnw.cmd test
```

Testing notes:

- tests run with the test configuration from [src/test/resources/application.properties](src/test/resources/application.properties)
- the test suite uses H2, so a local MySQL server is not required
- you do not need your local `.env.local.properties` file to run the automated tests unless you add tests that depend on external infrastructure

## 17. Key Maintenance Rules

- Edit source HTML in `src/main/resources/static`, not in `docs/`
- Treat `docs/` and `build/pages/` as generated output
- Re-run layout sync or export after shared header/footer changes
- Re-run static export after public page, SEO, or i18n changes
- Keep booking/contact changes aligned between frontend config and backend config
- When changing metadata or bilingual content, verify both NL and EN outputs

## 18. Recommended Handover Notes

If another developer takes over this project, they should understand these five things first:

1. The project is both a Spring Boot app and a static-exportable marketing site.
2. Public page source lives in `src/main/resources/static`.
3. Shared header/footer are maintained through `_partials` plus PowerShell sync/export scripts.
4. Bilingual behavior is attribute-driven in HTML, with static EN export generated automatically.
5. `docs/` is output, not source.

## 19. Supporting Files

Additional project notes already present in the repository:

- [HELP.md](HELP.md)

That file is useful for environment variables and deployment context, while this document is intended as the complete high-level project reference.

