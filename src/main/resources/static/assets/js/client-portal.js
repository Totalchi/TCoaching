(function () {
  const selectors = {
    status: document.querySelector('[data-portal-status]'),
    welcome: document.querySelector('[data-portal-welcome]'),
    subcopy: document.querySelector('[data-portal-subcopy]'),
    metricNextAppointment: document.querySelector('[data-metric-next-appointment]'),
    metricInvoices: document.querySelector('[data-metric-invoices]'),
    metricMessages: document.querySelector('[data-metric-messages]'),
    metricPlan: document.querySelector('[data-metric-plan]'),
    appointments: document.querySelector('[data-portal-appointments]'),
    trainingPlan: document.querySelector('[data-portal-training-plan]'),
    invoices: document.querySelector('[data-portal-invoices]'),
    messages: document.querySelector('[data-portal-messages]'),
    messageForm: document.querySelector('[data-portal-message-form]'),
    messageField: document.querySelector('[data-portal-message-input]'),
    logoutButton: document.querySelector('[data-portal-logout]')
  };

  const readCsrfToken = () => {
    const token = document.cookie
      .split('; ')
      .find((value) => value.startsWith('XSRF-TOKEN='));
    return token ? decodeURIComponent(token.split('=').slice(1).join('=')) : '';
  };

  let csrfPromise = null;

  const ensureCsrfToken = async () => {
    const existing = readCsrfToken();
    if (existing) {
      return existing;
    }
    if (!csrfPromise) {
      csrfPromise = fetch('/api/csrf', { cache: 'no-store', credentials: 'same-origin' })
        .finally(() => {
          csrfPromise = null;
        });
    }
    const response = await csrfPromise;
    if (!response.ok) {
      throw new Error('Beveiligingstoken ontbreekt.');
    }
    return readCsrfToken();
  };

  const apiFetch = async (url, options = {}) => {
    const response = await fetch(url, {
      credentials: 'same-origin',
      cache: 'no-store',
      ...options
    });
    if (response.status === 401) {
      window.location.href = '/inloggen.html';
      throw new Error('Niet aangemeld.');
    }
    return response;
  };

  const sendJson = async (url, method, payload) => {
    const csrfToken = await ensureCsrfToken();
    return apiFetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken
      },
      body: JSON.stringify(payload)
    });
  };

  const setStatus = (kind, text) => {
    if (!selectors.status) {
      return;
    }
    selectors.status.textContent = text || '';
    selectors.status.className = text ? `portal-status ${kind} visible` : 'portal-status';
  };

  const formatDateTime = (value) => {
    if (!value) {
      return 'Nog niet ingepland';
    }
    return new Intl.DateTimeFormat('nl-BE', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  };

  const renderEmpty = (container, message) => {
    if (!container) {
      return;
    }
    container.innerHTML = `<div class="portal-empty">${message}</div>`;
  };

  const renderAppointments = (items) => {
    if (!selectors.appointments) {
      return;
    }
    if (!items.length) {
      renderEmpty(selectors.appointments, 'Er staan nog geen afspraken klaar.');
      return;
    }
    selectors.appointments.innerHTML = items.map((item) => `
      <article class="portal-item">
        <strong>${item.title}</strong>
        <div class="portal-meta">${formatDateTime(item.scheduledAt)} | ${item.type} | ${item.status}</div>
        <div class="portal-meta">${item.location || 'Locatie volgt'}</div>
        ${item.notesShared ? `<p class="portal-muted">${item.notesShared}</p>` : ''}
      </article>
    `).join('');
  };

  const renderTrainingPlan = (plan) => {
    if (!selectors.trainingPlan) {
      return;
    }
    if (!plan) {
      renderEmpty(selectors.trainingPlan, 'Er is nog geen actief trainingsplan beschikbaar.');
      return;
    }

    const items = Array.isArray(plan.items) ? plan.items : [];
    selectors.trainingPlan.innerHTML = `
      <article class="portal-item">
        <strong>${plan.title}</strong>
        <div class="portal-meta">${plan.status}${plan.startDate ? ` | Start ${plan.startDate}` : ''}${plan.endDate ? ` | Einde ${plan.endDate}` : ''}</div>
        ${plan.description ? `<p class="portal-muted">${plan.description}</p>` : ''}
      </article>
      <div class="portal-plan-items">
        ${items.length ? items.map((item) => `
          <article class="portal-plan-item ${item.completedByClient ? 'is-complete' : ''}">
            <label>
              <input type="checkbox" data-plan-item-toggle="${item.id}" ${item.completedByClient ? 'checked' : ''} />
              <span>
                <strong>${item.title}</strong>
                <div class="portal-meta">${item.category}${item.reps ? ` | ${item.reps} reps` : ''}${item.sets ? ` | ${item.sets} sets` : ''}${item.durationSec ? ` | ${Math.round(item.durationSec / 60)} min` : ''}</div>
                ${item.description ? `<div class="portal-muted">${item.description}</div>` : ''}
              </span>
            </label>
          </article>
        `).join('') : '<div class="portal-empty">Dit plan bevat nog geen concrete items.</div>'}
      </div>
    `;

    selectors.trainingPlan.querySelectorAll('[data-plan-item-toggle]').forEach((input) => {
      input.addEventListener('change', async (event) => {
        const checkbox = event.currentTarget;
        checkbox.disabled = true;
        try {
          const response = await sendJson(`/api/client/training-items/${checkbox.dataset.planItemToggle}`, 'PATCH', {
            completed: checkbox.checked
          });
          if (!response.ok) {
            checkbox.checked = !checkbox.checked;
            setStatus('error', 'Opslaan lukt niet.');
            return;
          }
          await loadPortalData(false);
        } catch (error) {
          checkbox.checked = !checkbox.checked;
          setStatus('error', error.message || 'Opslaan lukt niet.');
        } finally {
          checkbox.disabled = false;
        }
      });
    });
  };

  const renderInvoices = (items) => {
    if (!selectors.invoices) {
      return;
    }
    if (!items.length) {
      renderEmpty(selectors.invoices, 'Er staan nog geen facturen klaar.');
      return;
    }
    selectors.invoices.innerHTML = items.map((item) => `
      <article class="portal-item">
        <strong>${item.invoiceNumber}</strong>
        <div class="portal-meta">${(item.amountCents / 100).toFixed(2)} ${item.currency} | ${item.status}${item.dueDate ? ` | Vervaldag ${item.dueDate}` : ''}</div>
        ${item.description ? `<div class="portal-muted">${item.description}</div>` : ''}
        ${item.pdfUrl ? `<div class="portal-meta"><a href="${item.pdfUrl}" target="_blank" rel="noopener">PDF openen</a></div>` : ''}
      </article>
    `).join('');
  };

  const renderMessages = (items) => {
    if (!selectors.messages) {
      return;
    }
    if (!items.length) {
      renderEmpty(selectors.messages, 'Er staan nog geen berichten in dit portaal.');
      return;
    }
    selectors.messages.innerHTML = items.map((item) => `
      <article class="portal-message">
        <strong>${item.sender === 'coach' ? 'Coach' : 'Jij'}</strong>
        <div class="portal-meta">${formatDateTime(item.createdAt)}</div>
        <p class="portal-muted">${item.body}</p>
      </article>
    `).join('');
  };

  const bindMessageForm = () => {
    if (!selectors.messageForm || !selectors.messageField) {
      return;
    }
    const button = selectors.messageForm.querySelector('button[type="submit"]');
    const idleLabel = button ? button.textContent : 'Bericht sturen';
    selectors.messageForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (!selectors.messageField.value.trim()) {
        return;
      }
      if (button) {
        button.disabled = true;
        button.textContent = 'Versturen...';
      }
      try {
        const response = await sendJson('/api/client/messages', 'POST', {
          body: selectors.messageField.value
        });
        if (!response.ok) {
          setStatus('error', 'Bericht kon niet verstuurd worden.');
          return;
        }
        selectors.messageField.value = '';
        setStatus('success', 'Bericht verstuurd.');
        await loadPortalData(false);
      } catch (error) {
        setStatus('error', error.message || 'Bericht kon niet verstuurd worden.');
      } finally {
        if (button) {
          button.disabled = false;
          button.textContent = idleLabel;
        }
      }
    });
  };

  const bindLogout = () => {
    if (!selectors.logoutButton) {
      return;
    }
    selectors.logoutButton.addEventListener('click', async () => {
      selectors.logoutButton.disabled = true;
      try {
        const response = await sendJson('/api/client/logout', 'POST', {});
        if (!response.ok) {
          setStatus('error', 'Afmelden lukt niet.');
          selectors.logoutButton.disabled = false;
          return;
        }
        window.location.href = '/inloggen.html?logout=1';
      } catch (error) {
        setStatus('error', error.message || 'Afmelden lukt niet.');
        selectors.logoutButton.disabled = false;
      }
    });
  };

  const loadPortalData = async (showLoading = true) => {
    if (showLoading) {
      setStatus('success', 'Portaal laden...');
    }
    try {
      const [sessionResponse, dashboardResponse, appointmentsResponse, planResponse, invoicesResponse, messagesResponse] = await Promise.all([
        apiFetch('/api/client/session'),
        apiFetch('/api/client/dashboard'),
        apiFetch('/api/client/appointments'),
        apiFetch('/api/client/training-plan'),
        apiFetch('/api/client/invoices'),
        apiFetch('/api/client/messages')
      ]);

      const session = await sessionResponse.json();
      const dashboard = await dashboardResponse.json();
      const appointments = await appointmentsResponse.json();
      const plan = planResponse.ok ? await planResponse.json() : null;
      const invoices = await invoicesResponse.json();
      const messages = await messagesResponse.json();

      if (selectors.welcome) {
        selectors.welcome.textContent = `Welkom terug, ${session.firstName || session.fullName || 'coachclient'}`;
      }
      if (selectors.subcopy) {
        selectors.subcopy.textContent = `Je portaal gebruikt ${session.email} als accountadres.`;
      }
      if (selectors.metricNextAppointment) {
        selectors.metricNextAppointment.textContent = dashboard.nextAppointment
          ? formatDateTime(dashboard.nextAppointment.scheduledAt)
          : 'Nog geen afspraak';
      }
      if (selectors.metricInvoices) {
        selectors.metricInvoices.textContent = String(dashboard.openInvoiceCount || 0);
      }
      if (selectors.metricMessages) {
        selectors.metricMessages.textContent = String(dashboard.unreadCoachMessages || 0);
      }
      if (selectors.metricPlan) {
        selectors.metricPlan.textContent = dashboard.activeTrainingPlanTitle
          ? `${dashboard.activeTrainingPlanTitle} (${dashboard.activeTrainingItemCount})`
          : 'Nog geen plan';
      }

      renderAppointments(Array.isArray(appointments) ? appointments : []);
      renderTrainingPlan(plan && plan.id ? plan : null);
      renderInvoices(Array.isArray(invoices) ? invoices : []);
      renderMessages(Array.isArray(messages) ? messages : []);
      setStatus('', '');
    } catch (error) {
      setStatus('error', error.message || 'Het portaal kon niet geladen worden.');
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    bindMessageForm();
    bindLogout();
    ensureCsrfToken().catch(() => {});
    loadPortalData(true);
  });
})();
