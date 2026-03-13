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
