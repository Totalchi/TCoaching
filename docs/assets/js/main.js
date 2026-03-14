const runtimeConfig = window.TCOACHING_CONFIG || {};

const normalizeBaseUrl = (value) => {
  if (!value) {
    return '';
  }
  return String(value).trim().replace(/\/+$/, '');
};

const buildApiUrl = (path) => {
  const trimmedPath = path.startsWith('/') ? path : `/${path}`;
  const baseUrl = normalizeBaseUrl(runtimeConfig.apiBaseUrl);
  return baseUrl ? `${baseUrl}${trimmedPath}` : trimmedPath;
};

const apiConfig = {
  enabled: runtimeConfig.apiEnabled !== false,
  contactEndpoint: runtimeConfig.contactEndpoint || buildApiUrl('/api/contact'),
  trackEndpoint: runtimeConfig.trackEndpoint || buildApiUrl('/api/track'),
  publicConfigEndpoint: runtimeConfig.publicConfigEndpoint || buildApiUrl('/api/public-config'),
  contactEmail: runtimeConfig.contactEmail || 'hello@tcoaching.be',
  bookingUrl: runtimeConfig.bookingUrl || '',
  mode: runtimeConfig.mode || 'app'
};
const defaultTrackingWindowMs = 15 * 60 * 1000;
const configuredTrackingWindowMs = Number(runtimeConfig.trackingWindowMs);
const trackingWindowMs = Number.isFinite(configuredTrackingWindowMs) && configuredTrackingWindowMs > 0
  ? configuredTrackingWindowMs
  : defaultTrackingWindowMs;
const autoPopupQuietWindowMs = 4000;
const localFallbackStore = new Map();
const sessionFallbackStore = new Map();

const resolveStorage = (storageName) => {
  if (typeof window === 'undefined' || !window[storageName]) {
    return null;
  }
  try {
    const storage = window[storageName];
    const probeKey = `tcoaching.${storageName}.probe`;
    storage.setItem(probeKey, '1');
    storage.removeItem(probeKey);
    return storage;
  } catch (error) {
    return null;
  }
};

const localStore = resolveStorage('localStorage');
const sessionStore = resolveStorage('sessionStorage');

const readStoredValue = (storage, fallback, key) => {
  try {
    if (storage) {
      return storage.getItem(key);
    }
  } catch (error) {
    // Fall through to in-memory fallback below.
  }
  return fallback.has(key) ? fallback.get(key) : null;
};

const writeStoredValue = (storage, fallback, key, value) => {
  const normalized = String(value);
  try {
    if (storage) {
      storage.setItem(key, normalized);
      fallback.delete(key);
      return;
    }
  } catch (error) {
    // Fall through to in-memory fallback below.
  }
  fallback.set(key, normalized);
};

const runtimeMessages = {
  nl: {
    formStatusSending: 'Even wachten, je aanvraag wordt veilig verstuurd.',
    formStatusSuccess: 'Dank je, je aanvraag is verstuurd en je hoort snel van mij.',
    formStatusError: 'Er ging iets mis. Probeer opnieuw of mail direct naar ons.',
    formStatusCaptcha: 'Bevestig de captcha om je aanvraag te versturen.',
    formStatusConfigMissing: 'Formulier is tijdelijk niet beschikbaar. Probeer later opnieuw of stuur een mail.'
  },
  en: {
    formStatusSending: 'One moment, your request is being sent securely.',
    formStatusSuccess: 'Thank you, your request was sent and you will hear from me soon.',
    formStatusError: 'Something went wrong. Please try again or email us directly.',
    formStatusCaptcha: 'Please complete the captcha to send your request.',
    formStatusConfigMissing: 'The form is temporarily unavailable. Please try again later or send an email.'
  }
};

let currentLang = 'nl';
let currentTheme = 'light';
let captchaEnabled = false;
let captchaReady = false;
let publicConfigCache = null;
let lastUserActivityAt = Date.now();
const autoPopupMaxAttempts = 5;
// Preferences should persist across visits when storage is available.
// Deduplication and popup timing only need to survive the current tab session.
const intakeAutoPopupStorageKey = 'tcoaching.intakePopupAutoShown';

const getRuntimeMessage = (key, lang = currentLang || 'nl') => {
  if (runtimeMessages[lang] && runtimeMessages[lang][key]) {
    return runtimeMessages[lang][key];
  }
  return runtimeMessages.nl[key] || null;
};

// i18n boundary:
// - Page copy and SEO-sensitive metadata live in HTML via data-lang-* / data-*-en attributes.
// - Runtime-only status text lives in runtimeMessages when it has no HTML source node.
const cacheLanguageDefaults = () => {
  document.querySelectorAll('[data-lang-en]').forEach((el) => {
    if (!el.dataset.langNl) {
      el.dataset.langNl = el.innerHTML;
    }
  });

  document.querySelectorAll('[data-placeholder-en]').forEach((el) => {
    if (!el.dataset.placeholderNl) {
      el.dataset.placeholderNl = el.getAttribute('placeholder') || '';
    }
  });

  document.querySelectorAll('[data-content-en]').forEach((el) => {
    if (!el.dataset.contentNl) {
      el.dataset.contentNl = el.getAttribute('content') || '';
    }
  });
  document.querySelectorAll('[data-alt-en]').forEach((el) => {
    if (!el.dataset.altNl) {
      el.dataset.altNl = el.getAttribute('alt') || '';
    }
  });

  document.querySelectorAll('[data-aria-label-en]').forEach((el) => {
    if (!el.dataset.ariaLabelNl) {
      el.dataset.ariaLabelNl = el.getAttribute('aria-label') || '';
    }
  });

  document.querySelectorAll('[data-value-en]').forEach((el) => {
    if (!el.dataset.valueNl) {
      el.dataset.valueNl = el.getAttribute('value') || '';
    }
  });
};

const syncDocumentMetadata = () => {
  const titleElement = document.querySelector('title');
  if (!titleElement) {
    return;
  }

  const nextTitle = titleElement.textContent.trim();
  if (nextTitle) {
    document.title = nextTitle;
  }
};

const syncStaticModeMessageElements = () => {
  if (apiConfig.enabled) {
    return;
  }

  const staticModeMessage = getStaticModeMessage();
  document.querySelectorAll('[data-form-status], [data-static-mode-hint]').forEach((element) => {
    element.textContent = staticModeMessage;
  });
};

const applyContentLanguage = (lang) => {
  const english = lang === 'en';

  document.querySelectorAll('[data-lang-en]').forEach((el) => {
    const nextValue = english ? el.dataset.langEn : el.dataset.langNl;
    if (typeof nextValue === 'string') {
      el.innerHTML = nextValue;
    }
  });

  document.querySelectorAll('[data-placeholder-en]').forEach((el) => {
    const nextValue = english ? el.dataset.placeholderEn : el.dataset.placeholderNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('placeholder', nextValue);
    }
  });

  document.querySelectorAll('[data-content-en]').forEach((el) => {
    const nextValue = english ? el.dataset.contentEn : el.dataset.contentNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('content', nextValue);
    }
  });
  document.querySelectorAll('[data-alt-en]').forEach((el) => {
    const nextValue = english ? el.dataset.altEn : el.dataset.altNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('alt', nextValue);
    }
  });

  document.querySelectorAll('[data-aria-label-en]').forEach((el) => {
    const nextValue = english ? el.dataset.ariaLabelEn : el.dataset.ariaLabelNl;
    if (typeof nextValue === 'string' && nextValue.length) {
      el.setAttribute('aria-label', nextValue);
    }
  });

  document.querySelectorAll('[data-value-en]').forEach((el) => {
    const nextValue = english ? el.dataset.valueEn : el.dataset.valueNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('value', nextValue);
    }
  });
};

const getThemeToggleLabel = () => {
  return currentTheme === 'dark' ? '\u2600' : '\u263E';
};

const getThemeToggleAriaLabel = () => {
  if (currentLang === 'en') {
    return currentTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
  }
  return currentTheme === 'dark' ? 'Schakel naar lichte modus' : 'Schakel naar donkere modus';
};

const updateThemeColor = () => {
  const themeColor = currentTheme === 'dark' ? '#0c0c0d' : '#f6f1e8';
  document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => {
    meta.setAttribute('content', themeColor);
  });
};

const updateThemeLabel = () => {
  const label = document.querySelector('[data-theme-label]');
  const button = document.querySelector('[data-theme-toggle]');
  if (!label) {
    return;
  }
  label.textContent = getThemeToggleLabel();
  if (button) {
    const ariaLabel = getThemeToggleAriaLabel();
    button.setAttribute('aria-label', ariaLabel);
    button.setAttribute('title', ariaLabel);
  }
};

const getMobileMenuLabel = (expanded) => {
  if (currentLang === 'en') {
    return expanded ? 'Close menu' : 'Menu';
  }
  return expanded ? 'Sluit menu' : 'Menu';
};

const normalizePageHref = (href) => {
  if (!href) {
    return null;
  }

  const normalized = href.split('#')[0].split('?')[0].replace(/^\.\//, '');
  if (!normalized || normalized === '/') {
    return 'index.html';
  }
  if (/^(mailto:|tel:|https?:)/i.test(normalized)) {
    return null;
  }
  return normalized;
};

const getCurrentPageHref = () => {
  const pathname = window.location.pathname || '/';
  if (!pathname || pathname.endsWith('/')) {
    return 'index.html';
  }

  const filename = pathname.split('/').pop();
  return filename || 'index.html';
};

const updateCurrentNavState = () => {
  const currentPageHref = getCurrentPageHref();
  document.querySelectorAll('header nav a[aria-current]').forEach((link) => {
    link.removeAttribute('aria-current');
  });

  document.querySelectorAll('header nav a[href]').forEach((link) => {
    const normalizedHref = normalizePageHref(link.getAttribute('href'));
    if (normalizedHref === currentPageHref) {
      link.setAttribute('aria-current', 'page');
    }
  });
};

const syncMobileNavUi = () => {
  const button = document.querySelector('[data-nav-toggle]');
  const nav = document.querySelector('[data-nav]');
  if (!button || !nav) {
    return;
  }
  const expanded = nav.classList.contains('is-open');
  button.setAttribute('aria-expanded', String(expanded));
  button.setAttribute('aria-label', getMobileMenuLabel(expanded));
  button.setAttribute('title', getMobileMenuLabel(expanded));
};

const setLanguage = (lang) => {
  currentLang = lang;
  writeStoredValue(localStore, localFallbackStore, 'siteLang', lang);
  document.documentElement.lang = lang;
  document.documentElement.dataset.lang = lang;
  applyContentLanguage(lang);
  syncDocumentMetadata();
  syncStaticModeMessageElements();
  updateThemeLabel();
  syncMobileNavUi();
  document.querySelectorAll('[data-lang-button]').forEach((btn) => {
    btn.classList.toggle('is-active', btn.dataset.lang === lang);
  });
};

const getInitialLang = () => {
  const stored = readStoredValue(localStore, localFallbackStore, 'siteLang');
  if (stored) {
    return stored;
  }
  if (navigator.language && navigator.language.toLowerCase().startsWith('en')) {
    return 'en';
  }
  return 'nl';
};

const setTheme = (theme) => {
  currentTheme = theme;
  writeStoredValue(localStore, localFallbackStore, 'siteTheme', theme);
  document.documentElement.dataset.theme = theme;
  updateThemeLabel();
  updateThemeColor();
};

const getInitialTheme = () => {
  const stored = readStoredValue(localStore, localFallbackStore, 'siteTheme');
  if (stored) {
    return stored;
  }
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
    return 'light';
  }
  return 'dark';
};

const initThemeToggle = () => {
  const button = document.querySelector('[data-theme-toggle]');
  if (!button) {
    return;
  }
  button.addEventListener('click', () => {
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
  });
};

const initLangToggle = () => {
  document.querySelectorAll('[data-lang-button]').forEach((btn) => {
    btn.addEventListener('click', () => {
      setLanguage(btn.dataset.lang);
    });
  });
};

const initMobileNav = () => {
  const nav = document.querySelector('header nav');
  const headerInner = document.querySelector('.header-inner');
  const brand = headerInner ? headerInner.querySelector('.brand') : null;
  if (!nav || !headerInner || !brand) {
    return;
  }

  nav.id = nav.id || 'site-nav';
  nav.setAttribute('data-nav', '');

  let button = document.querySelector('[data-nav-toggle]');
  if (!button) {
    button = document.createElement('button');
    button.className = 'nav-toggle';
    button.type = 'button';
    button.setAttribute('data-nav-toggle', '');
    button.setAttribute('aria-expanded', 'false');
    button.setAttribute('aria-controls', nav.id);
    button.innerHTML = '<span></span><span></span><span></span>';
    brand.insertAdjacentElement('afterend', button);
  } else {
    button.setAttribute('aria-controls', nav.id);
  }

  const mobileQuery = window.matchMedia('(max-width: 820px)');
  const closeNav = () => {
    nav.classList.remove('is-open');
    document.body.classList.remove('nav-open');
    syncMobileNavUi();
  };

  button.addEventListener('click', () => {
    if (nav.classList.contains('is-open')) {
      closeNav();
      return;
    }
    nav.classList.add('is-open');
    document.body.classList.add('nav-open');
    syncMobileNavUi();
  });

  nav.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', () => {
      if (mobileQuery.matches) {
        closeNav();
      }
    });
  });

  const handleViewportChange = (event) => {
    if (!event.matches) {
      closeNav();
    }
  };

  if (mobileQuery.addEventListener) {
    mobileQuery.addEventListener('change', handleViewportChange);
  } else if (mobileQuery.addListener) {
    mobileQuery.addListener(handleViewportChange);
  }

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && nav.classList.contains('is-open')) {
      closeNav();
    }
  });

  syncMobileNavUi();
};

const initReveal = () => {
  const items = document.querySelectorAll('.reveal');
  if (!items.length) {
    return;
  }
  items.forEach((item, index) => {
    const delay = (index % 6) * 0.08;
    item.style.transitionDelay = `${delay}s`;
  });
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.2 }
  );
  items.forEach((item) => observer.observe(item));
};

const initPointerGlow = () => {
  const reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  if (reduceMotion || !finePointer) {
    return;
  }

  document.body.classList.add('has-custom-cursor');
  const dot = document.createElement('div');
  dot.className = 'cursor-dot';
  dot.setAttribute('aria-hidden', 'true');

  const ring = document.createElement('div');
  ring.className = 'cursor-ring';
  ring.setAttribute('aria-hidden', 'true');

  document.body.append(dot, ring);

  let frameId = null;
  let nextX = -9999;
  let nextY = -9999;
  let ringX = -9999;
  let ringY = -9999;

  const render = () => {
    frameId = null;
    dot.style.transform = `translate3d(${nextX}px, ${nextY}px, 0)`;

    ringX += (nextX - ringX) * 0.2;
    ringY += (nextY - ringY) * 0.2;
    ring.style.transform = `translate3d(${ringX}px, ${ringY}px, 0)`;

    if (Math.abs(nextX - ringX) > 0.1 || Math.abs(nextY - ringY) > 0.1) {
      frameId = window.requestAnimationFrame(render);
    }
  };

  const queueRender = (event) => {
    nextX = Math.round(event.clientX);
    nextY = Math.round(event.clientY);
    if (ringX < -9000 || ringY < -9000) {
      ringX = nextX;
      ringY = nextY;
    }
    if (frameId === null) {
      frameId = window.requestAnimationFrame(render);
    }
  };

  const activateRing = (active) => {
    ring.classList.toggle('is-active', active);
  };

  const enhancePointerGlowTargets = (root = document) => {
    const scope = root.querySelectorAll ? root : document;
    scope.querySelectorAll('a, button, input, textarea, select, summary, label').forEach((element) => {
      if (element.dataset.pointerGlowBound === 'true') {
        return;
      }
      element.dataset.pointerGlowBound = 'true';
      element.addEventListener('pointerenter', () => activateRing(true), { passive: true });
      element.addEventListener('pointerleave', () => activateRing(false), { passive: true });
    });
  };

  window.addEventListener('pointermove', queueRender, { passive: true });
  enhancePointerGlowTargets(document);
  window.TCoachingEnhancePointerGlowTargets = enhancePointerGlowTargets;
  const observer = new MutationObserver((entries) => {
    entries.forEach((entry) => {
      entry.addedNodes.forEach((node) => {
        if (!(node instanceof Element)) {
          return;
        }
        if (node.matches('a, button, input, textarea, select, summary, label')) {
          enhancePointerGlowTargets(node.parentElement || document);
          return;
        }
        enhancePointerGlowTargets(node);
      });
    });
  });
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener('pointerleave', () => {
    dot.style.transform = 'translate3d(-9999px, -9999px, 0)';
    ring.style.transform = 'translate3d(-9999px, -9999px, 0)';
  });
};

const enhanceCardSpotlights = (root = document) => {
  const scope = root.querySelectorAll ? root : document;
  const selector = '.card-surface, .admin-card, .admin-table-card, .admin-metric, .admin-lead';

  scope.querySelectorAll(selector).forEach((card) => {
    if (card.dataset.cardSpotlightBound === 'true') {
      return;
    }
    card.dataset.cardSpotlightBound = 'true';

    const setGlowPosition = (event) => {
      const rect = card.getBoundingClientRect();
      card.style.setProperty('--card-glow-x', `${event.clientX - rect.left}px`);
      card.style.setProperty('--card-glow-y', `${event.clientY - rect.top}px`);
    };

    card.addEventListener('pointerenter', setGlowPosition);
    card.addEventListener('pointermove', setGlowPosition);
  });
};

const initCardSpotlights = () => {
  const reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  if (reduceMotion || !finePointer) {
    return;
  }

  enhanceCardSpotlights(document);
  window.TCoachingEnhanceCardSpotlights = enhanceCardSpotlights;

  const observer = new MutationObserver((entries) => {
    entries.forEach((entry) => {
      entry.addedNodes.forEach((node) => {
        if (!(node instanceof Element)) {
          return;
        }
        if (node.matches('.card-surface, .admin-card, .admin-table-card, .admin-metric, .admin-lead')) {
          enhanceCardSpotlights(node.parentElement || document);
          return;
        }
        enhanceCardSpotlights(node);
      });
    });
  });
  observer.observe(document.body, { childList: true, subtree: true });
};

const initPopup = () => {
  const overlays = Array.from(document.querySelectorAll('[data-intake-modal]'));
  if (!overlays.length) {
    return;
  }
  const openButtons = document.querySelectorAll('[data-open-intake]');
  const defaultOverlay = overlays[0];
  let autoOpened = false;
  let autoPopupAttempts = 0;
  const hasSeenAutoPopup = () => readStoredValue(localStore, localFallbackStore, intakeAutoPopupStorageKey) === 'true';
  const markUserActivity = () => {
    lastUserActivityAt = Date.now();
  };
  const hasFocusedFormField = () =>
    Boolean(document.querySelector('[data-contact-form] input:focus, [data-contact-form] select:focus, [data-contact-form] textarea:focus'));
  const shouldAutoOpen = () => {
    if (Date.now() - lastUserActivityAt < autoPopupQuietWindowMs) {
      return false;
    }
    return !hasFocusedFormField();
  };

  const resolveOverlay = (trigger) => {
    const target = trimValue(trigger?.dataset?.intakeTarget || '');
    if (target) {
      return overlays.find((overlay) =>
        overlay.id === target
        || overlay.dataset.intakeModal === target
        || overlay.dataset.intakeId === target
      ) || defaultOverlay;
    }
    return defaultOverlay;
  };

  const closeModal = (overlay) => {
    overlay.classList.remove('is-visible');
  };

  const openModal = (overlay, source = 'manual') => {
    overlay.classList.add('is-visible');
    renderAvailableCaptchaWidgets(overlay);
    autoOpened = true;
    if (source === 'auto') {
      writeStoredValue(localStore, localFallbackStore, intakeAutoPopupStorageKey, 'true');
    }
  };

  const scheduleAutoOpen = (delayMs) => {
    window.setTimeout(() => {
      if (autoOpened || hasSeenAutoPopup()) {
        return;
      }
      autoPopupAttempts += 1;
      if (autoPopupAttempts > autoPopupMaxAttempts) {
        return;
      }
      if (shouldAutoOpen()) {
        openModal(defaultOverlay, 'auto');
        return;
      }
      scheduleAutoOpen(autoPopupQuietWindowMs);
    }, delayMs);
  };

  overlays.forEach((overlay) => {
    overlay.querySelectorAll('[data-close-intake]').forEach((button) => {
      button.addEventListener('click', () => closeModal(overlay));
    });
    overlay.addEventListener('click', (event) => {
      if (event.target === overlay) {
        closeModal(overlay);
      }
    });
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      overlays.forEach((overlay) => closeModal(overlay));
    }
  });

  openButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      openModal(resolveOverlay(btn), 'manual');
    });

    if (!btn.matches('button, a, input, select, textarea')) {
      btn.addEventListener('keydown', (event) => {
        if (event.key !== 'Enter' && event.key !== ' ') {
          return;
        }
        event.preventDefault();
        openModal(resolveOverlay(btn), 'manual');
      });
    }
  });

  window.addEventListener('scroll', markUserActivity, { passive: true });
  window.addEventListener('pointerdown', markUserActivity, { passive: true });
  window.addEventListener('touchstart', markUserActivity, { passive: true });
  document.addEventListener('keydown', markUserActivity);
  document.addEventListener('input', markUserActivity);

  scheduleAutoOpen(8000);
};

const initHeaderState = () => {
  const header = document.querySelector('header');
  if (!header) {
    return;
  }

  const mobileLayout = window.matchMedia && window.matchMedia('(max-width: 820px)');
  let ticking = false;

  const syncHeaderHeight = () => {
    document.documentElement.style.setProperty('--header-height', `${header.offsetHeight}px`);
  };

  const syncHeaderState = () => {
    ticking = false;
    const shouldCondense = !(mobileLayout && mobileLayout.matches) && window.scrollY > 56;
    header.classList.toggle('is-condensed', shouldCondense);
    syncHeaderHeight();
  };

  const queueSync = () => {
    if (ticking) {
      return;
    }
    ticking = true;
    window.requestAnimationFrame(syncHeaderState);
  };

  syncHeaderState();
  window.addEventListener('scroll', queueSync, { passive: true });
  window.addEventListener('resize', syncHeaderState, { passive: true });
  if (typeof ResizeObserver === 'function') {
    new ResizeObserver(syncHeaderState).observe(header);
  }
  if (mobileLayout) {
    if (typeof mobileLayout.addEventListener === 'function') {
      mobileLayout.addEventListener('change', syncHeaderState);
    } else if (typeof mobileLayout.addListener === 'function') {
      mobileLayout.addListener(syncHeaderState);
    }
  }
};

const trimValue = (value) => {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length ? trimmed : null;
};

const limitValue = (value, max) => {
  if (!value) {
    return null;
  }
  return value.length > max ? value.slice(0, max) : value;
};

const getStaticModeMessage = () => {
  if (currentLang === 'en') {
    return 'This shared static version cannot send requests directly. Use the live app version with backend support.';
  }
  return 'Deze gedeelde statische versie kan geen aanvragen rechtstreeks versturen. Gebruik de live appversie met backend.';
};

const buildTrackingPayload = (overrides = {}) => {
  const path = limitValue(overrides.path || window.location.pathname || '/', 200);
  if (!path) {
    return null;
  }

  return {
    path,
    title: limitValue(overrides.title ?? document.title, 200),
    referrer: limitValue(overrides.referrer ?? document.referrer, 255),
    lang: limitValue(overrides.lang ?? currentLang, 10),
    eventType: limitValue(overrides.eventType, 40),
    eventName: limitValue(overrides.eventName, 80),
    eventValue: limitValue(overrides.eventValue, 255)
  };
};

const sendJson = async (url, payload, keepalive = false) => {
  if (!url) {
    throw new Error('Missing endpoint');
  }
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
    keepalive
  });
  if (!response.ok) {
    throw new Error('Request failed');
  }
};

const dispatchTrackingPayload = (payload, { keepalive = true, immediate = false } = {}) => {
  if (!payload || !apiConfig.enabled || !apiConfig.trackEndpoint) {
    return;
  }

  const sendTracking = () => {
    sendJson(apiConfig.trackEndpoint, payload, keepalive).catch(() => {});
  };

  if (immediate) {
    sendTracking();
    return;
  }

  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(sendTracking, { timeout: 1500 });
    return;
  }

  window.setTimeout(sendTracking, 250);
};

const fetchPublicConfig = async () => {
  if (publicConfigCache) {
    return publicConfigCache;
  }
  if (!apiConfig.enabled || !apiConfig.publicConfigEndpoint) {
    return null;
  }
  try {
    const response = await fetch(apiConfig.publicConfigEndpoint, { cache: 'no-store' });
    if (!response.ok) {
      return null;
    }
    publicConfigCache = await response.json();
    if (publicConfigCache && publicConfigCache.bookingUrl && !apiConfig.bookingUrl) {
      apiConfig.bookingUrl = publicConfigCache.bookingUrl;
    }
    return publicConfigCache;
  } catch (error) {
    return null;
  }
};

const loadScript = (src, timeoutMs = 8000) =>
  new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      if (existing.dataset.loadState === 'loaded') {
        resolve();
        return;
      }
      if (existing.dataset.loadState === 'failed') {
        reject(new Error('Script load failed'));
        return;
      }
      const handleExistingLoad = () => {
        existing.dataset.loadState = 'loaded';
        resolve();
      };
      const handleExistingError = () => {
        existing.dataset.loadState = 'failed';
        reject(new Error('Script load failed'));
      };
      existing.addEventListener('load', handleExistingLoad, { once: true });
      existing.addEventListener('error', handleExistingError, { once: true });
      window.setTimeout(() => {
        existing.removeEventListener('load', handleExistingLoad);
        existing.removeEventListener('error', handleExistingError);
        reject(new Error('Script load timed out'));
      }, timeoutMs);
      return;
    }
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.defer = true;
    script.onload = () => {
      script.dataset.loadState = 'loaded';
      resolve();
    };
    script.onerror = () => {
      script.dataset.loadState = 'failed';
      reject(new Error('Script load failed'));
    };
    document.head.appendChild(script);
    window.setTimeout(() => {
      if (script.dataset.loadState === 'loaded') {
        return;
      }
      script.dataset.loadState = 'failed';
      reject(new Error('Script load timed out'));
    }, timeoutMs);
  });

const initCaptcha = async () => {
  if (!apiConfig.enabled) {
    return;
  }

  const config = await fetchPublicConfig();
  if (!config || !config.captchaEnabled) {
    return;
  }
  captchaEnabled = true;

  if (!config.captchaSiteKey) {
    return;
  }

  try {
    await loadScript('https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit');
  } catch (error) {
    return;
  }

  if (!window.turnstile) {
    return;
  }

  captchaReady = true;
  document.documentElement.dataset.captchaSiteKey = config.captchaSiteKey;
  renderAvailableCaptchaWidgets(document);
};

const isCaptchaWidgetVisible = (widget) => {
  if (!(widget instanceof HTMLElement)) {
    return false;
  }
  if (widget.closest('[hidden]')) {
    return false;
  }
  const overlay = widget.closest('[data-intake-modal]');
  if (overlay && !overlay.classList.contains('is-visible')) {
    return false;
  }
  return widget.getClientRects().length > 0;
};

const renderCaptchaWidget = (widget) => {
  if (!captchaEnabled || !captchaReady || !window.turnstile || !widget || widget.dataset.captchaWidgetId) {
    return;
  }
  if (!isCaptchaWidgetVisible(widget)) {
    return;
  }

  const siteKey = trimValue(document.documentElement.dataset.captchaSiteKey || '');
  if (!siteKey) {
    return;
  }

  const form = widget.closest('form');
  const tokenField = form ? form.querySelector('[data-captcha-token]') : null;
  const widgetId = window.turnstile.render(widget, {
    sitekey: siteKey,
    callback: (token) => {
      if (tokenField) {
        tokenField.value = token;
      }
    },
    'error-callback': () => {
      if (tokenField) {
        tokenField.value = '';
      }
    },
    'expired-callback': () => {
      if (tokenField) {
        tokenField.value = '';
      }
    }
  });
  widget.dataset.captchaWidgetId = String(widgetId);
};

const renderAvailableCaptchaWidgets = (root = document) => {
  const scope = root.querySelectorAll ? root : document;
  scope.querySelectorAll('[data-captcha-widget]').forEach((widget) => {
    renderCaptchaWidget(widget);
  });
};

const initBooking = async () => {
  const config = await fetchPublicConfig();
  const bookingUrl = trimValue((config && config.bookingUrl) || apiConfig.bookingUrl);
  const directLink = document.querySelector('[data-booking-link]:not([data-booking-fallback])');
  const fallbackLink = document.querySelector('[data-booking-link][data-booking-fallback]');
  const status = document.querySelector('[data-booking-status]');
  const embed = document.querySelector('[data-booking-embed]');
  const embedShell = document.querySelector('[data-booking-embed-shell]');
  const placeholder = document.querySelector('[data-booking-placeholder]');

  if (!bookingUrl) {
    if (directLink) {
      directLink.hidden = true;
    }
    return;
  }

  if (directLink) {
    directLink.hidden = false;
    directLink.href = bookingUrl;
    directLink.target = '_blank';
    directLink.rel = 'noreferrer';
  }

  if (fallbackLink) {
    fallbackLink.textContent = currentLang === 'en' ? 'Request via form' : 'Vraag aan via formulier';
  }

  if (status) {
    status.textContent = currentLang === 'en'
      ? 'A direct booking tool is active. You can open it immediately or still use the form below for a personal proposal.'
      : 'Er is een directe boekingstool actief. Je kan die meteen openen of alsnog het formulier hieronder gebruiken voor een persoonlijk voorstel.';
  }

  if (embed && embedShell) {
    embed.src = bookingUrl;
    embedShell.hidden = false;
  }
  if (placeholder) {
    placeholder.hidden = true;
  }
};

const resetCaptcha = (form) => {
  const tokenField = form.querySelector('[data-captcha-token]');
  if (tokenField) {
    tokenField.value = '';
  }
  const widget = form.querySelector('[data-captcha-widget]');
  if (widget && widget.dataset.captchaWidgetId && window.turnstile) {
    window.turnstile.reset(widget.dataset.captchaWidgetId);
  }
};

const buildContactPayload = (form) => {
  const formData = new FormData(form);
  const payload = {};

  formData.forEach((value, key) => {
    if (key === 'captchaToken') {
      return;
    }
    const normalized = trimValue(String(value));
    if (normalized) {
      payload[key] = normalized;
    }
  });

  const captchaToken = trimValue(String(formData.get('captchaToken') || ''));
  if (captchaToken) {
    payload.captchaToken = captchaToken;
  }
  payload.lang = currentLang;
  return payload;
};

const resetInteractiveForm = (form) => {
  form.reset();
  applyContentLanguage(currentLang);
};

const initTrackedClicks = () => {
  document.querySelectorAll('[data-track-click], [data-open-intake]').forEach((element) => {
    element.addEventListener('click', () => {
      const eventName = trimValue(element.dataset.trackClick)
        || (element.hasAttribute('data-open-intake') ? 'intake_open' : 'cta_click');
      const eventValue = limitValue(
        trimValue(element.dataset.trackValue)
          || trimValue(element.textContent)
          || element.getAttribute('href'),
        255
      );

      dispatchTrackingPayload(
        buildTrackingPayload({
          eventType: 'cta_click',
          eventName,
          eventValue
        }),
        { keepalive: true, immediate: true }
      );
    });
  });
};

const wireForms = () => {
  const forms = document.querySelectorAll('[data-contact-form]');
  if (!forms.length) {
    return;
  }

  forms.forEach((form) => {
    const trackingName = trimValue(form.dataset.contactForm)
      || trimValue(form.querySelector('input[name="page"]')?.value)
      || 'contact-form';
    const markFormStarted = () => {
      if (form.dataset.formStarted === 'true') {
        return;
      }
      form.dataset.formStarted = 'true';
      dispatchTrackingPayload(
        buildTrackingPayload({
          eventType: 'form_start',
          eventName: trackingName,
          eventValue: trimValue(form.querySelector('select[name="topic"]')?.value)
        }),
        { keepalive: true, immediate: true }
      );
    };

    form.querySelectorAll('input, select, textarea').forEach((field) => {
      field.addEventListener('focus', markFormStarted, { passive: true });
      field.addEventListener('input', markFormStarted, { passive: true });
    });

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const status = form.querySelector('[data-form-status]');
      const setStatus = (key) => {
        if (!status) {
          return;
        }
        const value = getRuntimeMessage(key);
        status.textContent = value || '';
      };

      setStatus('formStatusSending');

      if (captchaEnabled) {
        if (!captchaReady) {
          setStatus('formStatusConfigMissing');
          return;
        }
        renderAvailableCaptchaWidgets(form);
        const tokenField = form.querySelector('[data-captcha-token]');
        const token = tokenField ? trimValue(tokenField.value) : null;
        if (!token) {
          setStatus('formStatusCaptcha');
          resetCaptcha(form);
          return;
        }
      }

      const payload = buildContactPayload(form);
      if (payload.website) {
        setStatus('formStatusSuccess');
        resetInteractiveForm(form);
        if (captchaEnabled) {
          resetCaptcha(form);
        }
        return;
      }

      if (!apiConfig.enabled) {
        setStatus(getStaticModeMessage());
        return;
      }

      try {
        await sendJson(apiConfig.contactEndpoint, payload);
        setStatus('formStatusSuccess');
        dispatchTrackingPayload(
          buildTrackingPayload({
            eventType: 'form_submit_success',
            eventName: trackingName,
            eventValue: payload.topic || payload.page || null
          }),
          { keepalive: true, immediate: true }
        );
        resetInteractiveForm(form);
        delete form.dataset.formStarted;
        if (captchaEnabled) {
          resetCaptcha(form);
        }
      } catch (error) {
        setStatus('formStatusError');
        if (captchaEnabled) {
          resetCaptcha(form);
        }
      }
    });
  });
};

const initStaticModeHints = () => {
  if (apiConfig.enabled) {
    return;
  }

  const staticModeMessage = getStaticModeMessage();

  document.documentElement.dataset.siteMode = apiConfig.mode;
  document.querySelectorAll('[data-contact-form]').forEach((form) => {
    form.dataset.staticMode = 'true';
    const submitButton = form.querySelector('button[type="submit"], input[type="submit"]');
    const statusElement = form.querySelector('[data-form-status]');
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.setAttribute('aria-disabled', 'true');
      submitButton.title = staticModeMessage;
    }

    if (!statusElement && !form.querySelector('[data-static-mode-hint]')) {
      const hint = document.createElement('p');
      hint.className = 'form-status';
      hint.setAttribute('data-static-mode-hint', '');
      hint.textContent = staticModeMessage;
      if (submitButton) {
        submitButton.insertAdjacentElement('afterend', hint);
      } else {
        form.append(hint);
      }
    }
  });
  document.querySelectorAll('[data-form-status]').forEach((element) => {
    element.textContent = staticModeMessage;
  });
  document.querySelectorAll('[data-captcha-widget]').forEach((element) => {
    element.remove();
  });
};

const trackPageView = () => {
  const payload = buildTrackingPayload();
  if (!payload) {
    return;
  }

  try {
    const storageKey = `pageview:${payload.path}`;
    const previous = Number(readStoredValue(sessionStore, sessionFallbackStore, storageKey) || 0);
    const now = Date.now();
    if (previous && now - previous < trackingWindowMs) {
      return;
    }
    writeStoredValue(sessionStore, sessionFallbackStore, storageKey, String(now));
  } catch (error) {
    // Ignore timestamp read failures and continue with best-effort tracking.
  }

  dispatchTrackingPayload(payload, { keepalive: true, immediate: false });
};
document.addEventListener('DOMContentLoaded', async () => {
  document.body.classList.add('is-loaded');
  cacheLanguageDefaults();
  currentLang = getInitialLang();
  currentTheme = getInitialTheme();
  setTheme(currentTheme);
  setLanguage(currentLang);
  initMobileNav();
  updateCurrentNavState();
  initHeaderState();
  initThemeToggle();
  initLangToggle();
  initReveal();
  initPointerGlow();
  initCardSpotlights();
  initPopup();
  initTrackedClicks();
  initStaticModeHints();
  trackPageView();
  wireForms();
  initBooking().catch(() => {});
  initCaptcha().catch(() => {});
});



