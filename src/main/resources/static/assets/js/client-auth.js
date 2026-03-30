(function () {
  let cachedCsrfToken = null;
  let csrfPromise = null;

  const ensureCsrfToken = async () => {
    if (cachedCsrfToken) {
      return cachedCsrfToken;
    }
    if (!csrfPromise) {
      csrfPromise = fetch('/api/csrf', { cache: 'no-store', credentials: 'same-origin' })
        .then(async (response) => {
          if (!response.ok) {
            throw new Error('Beveiligingstoken kon niet geladen worden.');
          }
          const data = await response.json();
          if (!data.token) {
            throw new Error('Beveiligingstoken ontbreekt.');
          }
          cachedCsrfToken = data.token;
          return cachedCsrfToken;
        })
        .finally(() => {
          csrfPromise = null;
        });
    }
    return csrfPromise;
  };

  const postJson = async (url, payload) => {
    const csrfToken = await ensureCsrfToken();
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken
      },
      credentials: 'same-origin',
      body: JSON.stringify(payload)
    });
  };

  const setMessage = (element, kind, text) => {
    if (!element) {
      return;
    }
    element.textContent = text || '';
    element.className = `auth-message ${kind} is-visible`;
  };

  const clearMessage = (element) => {
    if (!element) {
      return;
    }
    element.textContent = '';
    element.className = 'auth-message';
  };

  const setBusy = (button, busyLabel, idleLabel, busy) => {
    if (!button) {
      return;
    }
    button.disabled = busy;
    button.textContent = busy ? busyLabel : idleLabel;
  };

  const bindLoginForm = () => {
    const form = document.querySelector('[data-client-login-form]');
    if (!form) {
      return;
    }

    const message = document.querySelector('[data-client-auth-message]');
    const submitButton = form.querySelector('button[type="submit"]');
    const idleLabel = submitButton ? submitButton.textContent : 'Inloggen';
    const params = new URLSearchParams(window.location.search);
    if (params.get('verified') === '1') {
      setMessage(message, 'success', 'Je e-mailadres is bevestigd. Je kan nu inloggen.');
    } else if (params.get('verification') === 'invalid') {
      setMessage(message, 'error', 'De verificatielink is ongeldig of verlopen.');
    } else if (params.get('reset') === 'success') {
      setMessage(message, 'success', 'Je wachtwoord is aangepast. Log opnieuw in.');
    } else if (params.get('logout') === '1') {
      setMessage(message, 'success', 'Je bent afgemeld uit het portaal.');
    }

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearMessage(message);
      setBusy(submitButton, 'Inloggen...', idleLabel, true);

      const formData = new FormData(form);
      try {
        const response = await postJson('/api/client/login', {
          email: String(formData.get('email') || ''),
          password: String(formData.get('password') || '')
        });
        if (!response.ok) {
          setMessage(message, 'error', 'Inloggen lukt niet. Controleer je gegevens of bevestig eerst je e-mail.');
          return;
        }
        window.location.href = '/portaal.html';
      } catch (error) {
        setMessage(message, 'error', error.message || 'Inloggen lukt momenteel niet.');
      } finally {
        setBusy(submitButton, 'Inloggen...', idleLabel, false);
      }
    });
  };

  const bindRegisterForm = () => {
    const form = document.querySelector('[data-client-register-form]');
    if (!form) {
      return;
    }

    const message = document.querySelector('[data-client-auth-message]');
    const submitButton = form.querySelector('button[type="submit"]');
    const idleLabel = submitButton ? submitButton.textContent : 'Account aanmaken';

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearMessage(message);
      setBusy(submitButton, 'Versturen...', idleLabel, true);

      const formData = new FormData(form);
      try {
        const response = await postJson('/api/client/register', {
          firstName: String(formData.get('firstName') || ''),
          lastName: String(formData.get('lastName') || ''),
          email: String(formData.get('email') || ''),
          phone: String(formData.get('phone') || ''),
          password: String(formData.get('password') || ''),
          lang: String(formData.get('lang') || 'nl')
        });
        if (response.status === 409) {
          setMessage(message, 'error', 'Voor dit e-mailadres bestaat al een account.');
          return;
        }
        if (!response.ok) {
          setMessage(message, 'error', 'Registratie lukt momenteel niet.');
          return;
        }
        form.reset();
        setMessage(message, 'success', 'Je account is aangemaakt. Controleer nu je mailbox om je e-mailadres te bevestigen.');
      } catch (error) {
        setMessage(message, 'error', error.message || 'Registratie lukt momenteel niet.');
      } finally {
        setBusy(submitButton, 'Versturen...', idleLabel, false);
      }
    });
  };

  const bindResetForm = () => {
    const page = document.querySelector('[data-reset-page]');
    if (!page) {
      return;
    }

    const requestForm = document.querySelector('[data-reset-request-form]');
    const confirmForm = document.querySelector('[data-reset-confirm-form]');
    const message = document.querySelector('[data-client-auth-message]');
    const token = new URLSearchParams(window.location.search).get('token');

    if (token && requestForm && confirmForm) {
      requestForm.hidden = true;
      confirmForm.hidden = false;
      const hiddenToken = confirmForm.querySelector('input[name="token"]');
      if (hiddenToken) {
        hiddenToken.value = token;
      }
    }

    if (requestForm) {
      const submitButton = requestForm.querySelector('button[type="submit"]');
      const idleLabel = submitButton ? submitButton.textContent : 'Resetlink sturen';
      requestForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearMessage(message);
        setBusy(submitButton, 'Versturen...', idleLabel, true);

        const formData = new FormData(requestForm);
        try {
          const response = await postJson('/api/client/reset-password/request', {
            email: String(formData.get('email') || '')
          });
          if (!response.ok) {
            setMessage(message, 'error', 'De resetaanvraag kon niet verstuurd worden.');
            return;
          }
          requestForm.reset();
          setMessage(message, 'success', 'Als dit e-mailadres gekend is, werd een resetlink verstuurd.');
        } catch (error) {
          setMessage(message, 'error', error.message || 'De resetaanvraag kon niet verstuurd worden.');
        } finally {
          setBusy(submitButton, 'Versturen...', idleLabel, false);
        }
      });
    }

    if (confirmForm) {
      const submitButton = confirmForm.querySelector('button[type="submit"]');
      const idleLabel = submitButton ? submitButton.textContent : 'Wachtwoord opslaan';
      confirmForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearMessage(message);
        setBusy(submitButton, 'Opslaan...', idleLabel, true);

        const formData = new FormData(confirmForm);
        const password = String(formData.get('password') || '');
        const passwordRepeat = String(formData.get('passwordRepeat') || '');
        if (password !== passwordRepeat) {
          setMessage(message, 'error', 'De wachtwoorden zijn niet gelijk.');
          setBusy(submitButton, 'Opslaan...', idleLabel, false);
          return;
        }

        try {
          const response = await postJson('/api/client/reset-password/confirm', {
            token: String(formData.get('token') || ''),
            password
          });
          if (!response.ok) {
            setMessage(message, 'error', 'De resetlink is ongeldig of verlopen.');
            return;
          }
          window.location.href = '/inloggen.html?reset=success';
        } catch (error) {
          setMessage(message, 'error', error.message || 'Het wachtwoord kon niet aangepast worden.');
        } finally {
          setBusy(submitButton, 'Opslaan...', idleLabel, false);
        }
      });
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    ensureCsrfToken().catch(() => {});
    bindLoginForm();
    bindRegisterForm();
    bindResetForm();
  });
})();
