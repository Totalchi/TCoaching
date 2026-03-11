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
  topEvents: document.querySelector('[data-admin-top-events]'),
  leads: document.querySelector('[data-admin-leads]')
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

const readCsrfToken = () => {
  const token = document.cookie
    .split('; ')
    .find((value) => value.startsWith('XSRF-TOKEN='));
  return token ? decodeURIComponent(token.split('=').slice(1).join('=')) : '';
};

const saveLead = async (id, payload) => {
  const csrfToken = readCsrfToken();
  const response = await fetch(`/api/admin/contacts/${id}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(`Lead update failed with status ${response.status}`);
  }
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

const renderLeads = (leads) => {
  const container = adminSelectors.leads;
  if (!container) {
    return;
  }

  container.replaceChildren();
  if (!leads.length) {
    renderEmptyState(container, 'Nog geen leads beschikbaar.');
    return;
  }

  leads.forEach((lead) => {
    const card = document.createElement('article');
    card.className = 'admin-lead';

    const head = document.createElement('div');
    head.className = 'admin-lead-head';

    const titleWrap = document.createElement('div');
    titleWrap.className = 'admin-lead-title';
    const title = document.createElement('strong');
    title.textContent = lead.name || lead.email || `Lead #${lead.id}`;
    const subtitle = document.createElement('small');
    subtitle.textContent = `${lead.email || '-'}${lead.phone ? ` | ${lead.phone}` : ''}`;
    titleWrap.append(title, subtitle);

    const statusPill = document.createElement('span');
    statusPill.className = 'admin-pill';
    statusPill.textContent = lead.status || 'new';
    head.append(titleWrap, statusPill);

    const meta = document.createElement('div');
    meta.className = 'admin-lead-meta';
    [lead.requestType || 'intake', lead.topic || 'algemeen', lead.page || 'onbekend', `Aangemaakt ${formatAdminDate(lead.createdAt)}`]
      .forEach((value) => {
        const text = document.createElement('span');
        text.textContent = value;
        meta.appendChild(text);
      });

    const copy = document.createElement('div');
    copy.className = 'admin-lead-copy';
    if (lead.goal) {
      const goal = document.createElement('span');
      goal.textContent = `Doel: ${lead.goal}`;
      copy.appendChild(goal);
    }
    if (lead.message) {
      const message = document.createElement('span');
      message.textContent = `Bericht: ${lead.message}`;
      copy.appendChild(message);
    }
    if (lead.preferredTime) {
      const timing = document.createElement('span');
      timing.textContent = `Voorkeur: ${lead.preferredTime}`;
      copy.appendChild(timing);
    }

    const editor = document.createElement('div');
    editor.className = 'admin-lead-grid';

    const statusLabel = document.createElement('label');
    const statusText = document.createElement('span');
    statusText.textContent = 'Status';
    const statusSelect = document.createElement('select');
    ['new', 'in_progress', 'completed', 'archived'].forEach((value) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = value;
      if ((lead.status || 'new') === value) {
        option.selected = true;
      }
      statusSelect.appendChild(option);
    });
    statusLabel.append(statusText, statusSelect);

    const notesLabel = document.createElement('label');
    const notesText = document.createElement('span');
    notesText.textContent = 'Admin notities';
    const notesField = document.createElement('textarea');
    notesField.value = lead.adminNotes || '';
    notesLabel.append(notesText, notesField);

    editor.append(statusLabel, notesLabel);

    const actions = document.createElement('div');
    actions.className = 'admin-lead-actions';

    const saveButton = document.createElement('button');
    saveButton.type = 'button';
    saveButton.className = 'admin-button primary';
    saveButton.textContent = 'Opslaan';

    const archiveButton = document.createElement('button');
    archiveButton.type = 'button';
    archiveButton.className = 'admin-button danger';
    archiveButton.textContent = 'Archiveren';

    const feedback = document.createElement('small');
    feedback.textContent = `Laatst bijgewerkt ${formatAdminDate(lead.updatedAt)}`;

    saveButton.addEventListener('click', async () => {
      feedback.textContent = 'Opslaan...';
      try {
        await saveLead(lead.id, {
          status: statusSelect.value,
          adminNotes: notesField.value
        });
        feedback.textContent = 'Lead opgeslagen.';
        statusPill.textContent = statusSelect.value;
        loadAdminLeads();
      } catch (error) {
        feedback.textContent = 'Opslaan mislukt.';
      }
    });

    archiveButton.addEventListener('click', async () => {
      feedback.textContent = 'Archiveren...';
      try {
        await saveLead(lead.id, {
          status: 'archived',
          adminNotes: notesField.value,
          archived: true
        });
        feedback.textContent = 'Lead gearchiveerd.';
        loadAdminLeads();
      } catch (error) {
        feedback.textContent = 'Archiveren mislukt.';
      }
    });

    actions.append(saveButton, archiveButton, feedback);
    card.append(head, meta);
    if (copy.childNodes.length) {
      card.appendChild(copy);
    }
    card.append(editor, actions);
    container.appendChild(card);
  });
};

const loadAdminLeads = async () => {
  if (!adminSelectors.leads) {
    return;
  }

  try {
    const response = await fetch('/api/admin/contacts', {
      headers: {
        Accept: 'application/json'
      },
      cache: 'no-store'
    });

    if (!response.ok) {
      throw new Error(`Contacts request failed with status ${response.status}`);
    }

    const payload = await response.json();
    renderLeads(payload || []);
  } catch (error) {
    renderEmptyState(adminSelectors.leads, 'Leadopvolging kon niet geladen worden.');
  }
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
  loadAdminLeads();
});
