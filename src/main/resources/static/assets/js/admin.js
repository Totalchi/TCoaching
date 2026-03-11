const adminSelectors = {
  status: document.querySelector('[data-admin-status]'),
  generatedAt: document.querySelector('[data-admin-generated-at]'),
  totalLeads: document.querySelector('[data-admin-total-leads]'),
  leads30: document.querySelector('[data-admin-leads-30]'),
  pageViews30: document.querySelector('[data-admin-pageviews-30]'),
  events30: document.querySelector('[data-admin-events-30]'),
  cta30: document.querySelector('[data-admin-cta-30]'),
  recentContacts: document.querySelector('[data-admin-recent-contacts]'),
  topPages: document.querySelector('[data-admin-top-pages]'),
  topEvents: document.querySelector('[data-admin-top-events]')
};

const adminNumberFormat = new Intl.NumberFormat(document.documentElement.lang === 'en' ? 'en-GB' : 'nl-BE');
const adminDateFormat = new Intl.DateTimeFormat(document.documentElement.lang === 'en' ? 'en-GB' : 'nl-BE', {
  dateStyle: 'medium',
  timeStyle: 'short'
});

const formatAdminNumber = (value) => adminNumberFormat.format(Number(value || 0));
const formatAdminDate = (value) => {
  if (!value) {
    return '-';
  }
  return adminDateFormat.format(new Date(value));
};

const setAdminText = (element, value) => {
  if (element) {
    element.textContent = value;
  }
};

const appendCell = (row, value) => {
  const cell = document.createElement('td');
  cell.textContent = value;
  row.appendChild(cell);
};

const renderEmptyState = (container, message) => {
  if (!container) {
    return;
  }

  container.replaceChildren();
  const item = document.createElement('div');
  item.className = 'admin-list-item';
  const label = document.createElement('span');
  label.textContent = message;
  item.appendChild(label);
  container.appendChild(item);
};

const renderRecentContacts = (contacts) => {
  const container = adminSelectors.recentContacts;
  if (!container) {
    return;
  }

  container.replaceChildren();
  if (!contacts.length) {
    renderEmptyState(container, 'Nog geen aanvragen beschikbaar.');
    return;
  }

  contacts.forEach((contact) => {
    const item = document.createElement('div');
    item.className = 'admin-list-item';

    const name = document.createElement('strong');
    name.textContent = `${contact.name} (${contact.email})`;
    item.appendChild(name);

    const meta = document.createElement('span');
    const status = contact.status || 'new';
    const requestType = contact.requestType || 'intake';
    const topic = contact.topic || 'algemeen';
    const page = contact.page || 'onbekend';
    meta.textContent = `${status} | ${requestType} | ${topic} via ${page} op ${formatAdminDate(contact.createdAt)}`;
    item.appendChild(meta);

    container.appendChild(item);
  });
};

const renderTopPages = (pages) => {
  const body = adminSelectors.topPages;
  if (!body) {
    return;
  }

  body.replaceChildren();
  if (!pages.length) {
    const row = document.createElement('tr');
    appendCell(row, 'Geen data');
    appendCell(row, '-');
    appendCell(row, '-');
    body.appendChild(row);
    return;
  }

  pages.forEach((page) => {
    const row = document.createElement('tr');
    appendCell(row, page.path);
    appendCell(row, formatAdminNumber(page.visits));
    appendCell(row, formatAdminDate(page.lastSeen));
    body.appendChild(row);
  });
};

const renderTopEvents = (events) => {
  const body = adminSelectors.topEvents;
  if (!body) {
    return;
  }

  body.replaceChildren();
  if (!events.length) {
    const row = document.createElement('tr');
    appendCell(row, 'Geen data');
    appendCell(row, '-');
    appendCell(row, '-');
    appendCell(row, '-');
    body.appendChild(row);
    return;
  }

  events.forEach((event) => {
    const row = document.createElement('tr');
    appendCell(row, event.eventType || '-');
    appendCell(row, event.eventName || '-');
    appendCell(row, formatAdminNumber(event.total));
    appendCell(row, formatAdminDate(event.lastSeen));
    body.appendChild(row);
  });
};

const loadAdminDashboard = async () => {
  setAdminText(adminSelectors.status, 'Dashboard laden...');

  try {
    const response = await fetch('/api/admin/dashboard', {
      headers: {
        Accept: 'application/json'
      },
      cache: 'no-store'
    });

    if (!response.ok) {
      throw new Error(`Dashboard request failed with status ${response.status}`);
    }

    const payload = await response.json();
    const overview = payload.overview || {};

    setAdminText(adminSelectors.status, 'Dashboard live. Cijfers tonen de laatste 30 dagen waar relevant.');
    setAdminText(adminSelectors.generatedAt, formatAdminDate(payload.generatedAt));
    setAdminText(adminSelectors.totalLeads, formatAdminNumber(overview.totalLeads));
    setAdminText(adminSelectors.leads30, formatAdminNumber(overview.leadsLast30Days));
    setAdminText(adminSelectors.pageViews30, formatAdminNumber(overview.pageViewsLast30Days));
    setAdminText(adminSelectors.events30, formatAdminNumber(overview.eventsLast30Days));
    setAdminText(adminSelectors.cta30, formatAdminNumber(overview.ctaClicksLast30Days));

    renderRecentContacts(payload.recentContacts || []);
    renderTopPages(payload.topPages || []);
    renderTopEvents(payload.topEvents || []);
  } catch (error) {
    setAdminText(adminSelectors.status, 'Dashboard kon niet geladen worden. Controleer je login of backendstatus.');
    renderEmptyState(adminSelectors.recentContacts, 'Geen gegevens beschikbaar.');
    renderTopPages([]);
    renderTopEvents([]);
  }
};

document.addEventListener('DOMContentLoaded', () => {
  loadAdminDashboard();
});
