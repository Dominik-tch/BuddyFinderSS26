// --- KONFIGURATION ---
const BASE_URL = 'http://localhost:8080/api'; // Stelle sicher, dass der Port stimmt!

// --- STATE ---
let isLoginMode = true;
let currentFilter = 'getAll'; 

// --- INITIALISIERUNG ---
document.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus();
});

// --- UTILS ---
function showAlert(message, isError = false) {
    const alertBox = document.getElementById('alert-box');
    alertBox.textContent = message;
    alertBox.className = 'alert ' + (isError ? 'alert-error' : 'alert-success');
    alertBox.classList.remove('hidden');
    
    setTimeout(() => { alertBox.classList.add('hidden'); }, 4000);
}

function getToken() {
    return localStorage.getItem('sessionToken');
}

async function apiFetch(endpoint, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, {
            ...options,
            headers
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            if (response.status === 401) logout(false);
            throw new Error(data.error || 'Ein Fehler ist aufgetreten');
        }
        return data;
    } catch (error) {
        showAlert(error.message, true);
        throw error;
    }
}

// --- UI LOGIK ---
function checkAuthStatus() {
    if (getToken()) {
        document.getElementById('view-auth').classList.add('hidden');
        document.getElementById('view-dashboard').classList.remove('hidden');
        document.getElementById('btn-logout').classList.remove('hidden');
        loadActivities('getAll');
    } else {
        document.getElementById('view-auth').classList.remove('hidden');
        document.getElementById('view-dashboard').classList.add('hidden');
        document.getElementById('btn-logout').classList.add('hidden');
    }
}

function toggleAuthMode() {
    isLoginMode = !isLoginMode;
    const regFields = document.getElementById('register-fields');
    
    // UI Texte anpassen
    document.getElementById('auth-title').textContent = isLoginMode ? 'Anmelden' : 'Registrieren';
    document.getElementById('auth-btn-text').textContent = isLoginMode ? 'Login' : 'Konto erstellen';
    document.getElementById('auth-toggle-link').textContent = isLoginMode ? 'Noch kein Konto? Hier registrieren.' : 'Bereits ein Konto? Hier anmelden.';
    
    // Felder ein/ausblenden und Required-Attribut umschalten
    if (isLoginMode) {
        regFields.classList.add('hidden');
        document.getElementById('email').required = false;
        document.getElementById('firstName').required = false;
        document.getElementById('lastName').required = false;
    } else {
        regFields.classList.remove('hidden');
        document.getElementById('email').required = true;
        document.getElementById('firstName').required = true;
        document.getElementById('lastName').required = true;
    }
}

function updateTabUI(filterType) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById(`tab-${filterType}`).classList.add('active');
}

// --- AUTH API ---
async function handleAuth(event) {
    event.preventDefault();
    const endpoint = isLoginMode ? '/users/login' : '/users/register';
    
    // Basis-Payload
    let payload = {
        userName: document.getElementById('username').value,
        password: document.getElementById('password').value
    };

    // Zusätzliche Felder, wenn es eine Registrierung ist
    if (!isLoginMode) {
        payload.email = document.getElementById('email').value;
        payload.firstName = document.getElementById('firstName').value;
        payload.lastName = document.getElementById('lastName').value;
    }

    try {
        const data = await apiFetch(endpoint, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (isLoginMode && data.sessionID) {
            localStorage.setItem('sessionToken', data.sessionID);
            showAlert('Erfolgreich eingeloggt!');
            checkAuthStatus();
        } else if (!isLoginMode) {
            showAlert('Erfolgreich registriert! Bitte logge dich nun ein.');
            toggleAuthMode();
            // Formular leeren für sauberen Login
            document.getElementById('auth-form').reset();
        }
    } catch (error) {
        console.error("Auth Error", error);
    }
}

async function logout(callApi = true) {
    if (callApi && getToken()) {
        try {
            await apiFetch('/users/logout', { method: 'POST' });
        } catch (e) { console.error(e); }
    }
    localStorage.removeItem('sessionToken');
    checkAuthStatus();
    showAlert('Erfolgreich abgemeldet.');
}

// --- ACTIVITY API ---
async function loadActivities(filterType) {
    currentFilter = filterType;
    updateTabUI(filterType);

    let title = "Alle Aktivitäten";
    if(filterType === 'getAllOwned') title = "Meine erstellten Aktivitäten";
    if(filterType === 'getAllJoined') title = "Meine Teilnahmen";
    document.getElementById('list-title').textContent = title;

    const container = document.getElementById('activities-container');
    container.innerHTML = '<p>Lade Aktivitäten...</p>';

    try {
        const data = await apiFetch(`/activities/${filterType}`);
        renderActivities(data);
    } catch (error) {
        container.innerHTML = `<p style="color: var(--danger)">Fehler beim Laden: ${error.message}</p>`;
    }
}

function renderActivities(activities) {
    const container = document.getElementById('activities-container');
    container.innerHTML = '';

    if (!activities || activities.length === 0) {
        container.innerHTML = '<p>Keine Aktivitäten gefunden.</p>';
        return;
    }

    activities.forEach(act => {
        const card = document.createElement('div');
        card.className = "activity-card";
        
        card.innerHTML = `
            <div>
                <div class="activity-header">
                    <h4>${act.title || 'Ohne Titel'}</h4>
                    <span class="badge">Erstellt von: ${act.owner || 'Unbekannt'}</span>
                </div>
                <div style="font-size: 0.85rem; color: var(--brand); margin-bottom: 0.5rem; font-weight: bold;">
                    📍 ${act.location} &nbsp;|&nbsp; 💰 ${act.price}€ &nbsp;|&nbsp; 👥 Limit: ${act.currentParticipants}/${act.userLimit} Teilnehmer
                </div>
                <p class="activity-desc">${act.description || 'Keine Beschreibung'}</p>
                <p class="participants-list">Teilnehmer:${act.participants?.map(user => 
                `<span class="participant-link" onclick="openUser('${user.id}')">${user.userName}</span>`).join(", ") || "Keine"}
                </p>
            </div>
            <div class="activity-actions">
                ${currentFilter === 'getAllJoined' ? `<button onclick="leaveActivity('${act.id}')" class="btn btn-danger">Verlassen</button>`
                : `<button onclick="joinActivity('${act.id}', this)" class="btn btn-secondary">Beitreten</button>`
}
                ${currentFilter === 'getAllOwned' ? `<button onclick='openEditBox(${JSON.stringify(act)})' class="btn btn-primary">Bearbeiten</button>
                                                    <button onclick="deleteActivity('${act.id}')" class="btn btn-danger">Löschen</button>` : ''}
            </div>
        `;
        container.appendChild(card);
    });
}

async function handleAddActivity(event) {
    event.preventDefault();
    
    // Daten auslesen und Datentypen sicherstellen (int für Price und Limit)
    const payload = {
        title: document.getElementById('act-title').value,
        description: document.getElementById('act-desc').value,
        location: document.getElementById('act-location').value,
        price: parseInt(document.getElementById('act-price').value),
        userLimit: parseInt(document.getElementById('act-limit').value)
    };

    try {
        await apiFetch('/activities/add', {
            method: 'POST',
            body: JSON.stringify(payload) 
        });
        showAlert('Aktivität erfolgreich erstellt!');
        document.getElementById('add-activity-form').reset();
        loadActivities(currentFilter);
    } catch (error) {
        console.error("Add Activity Error", error);
    }
}

async function joinActivity(id, button) {

    button.disabled = true;
    button.textContent = "Beigetreten";
    try {
        await apiFetch(`/activities/join/${id}`, {
            method: 'POST'
        });
        showAlert('Erfolgreich beigetreten!');
    } catch (error) {
        button.disabled = false;
        button.textContent = "Beitreten";
        console.error(error);
    }
}

async function leaveActivity(id) {
    try {
        await apiFetch(`/activities/leave/${id}`, {
            method: 'DELETE'
        });
        showAlert('Aktivität verlassen.');
        loadActivities(currentFilter);
    } catch (error) {
        console.error(error);
    }
}

async function deleteActivity(id) {
    if(!confirm('Bist du sicher, dass du diese Aktivität löschen möchtest?')) return;
    
    try {
        await apiFetch(`/activities/delete/${id}`, { method: 'DELETE' });
        showAlert('Aktivität gelöscht.');
        loadActivities(currentFilter);
    } catch (error) {
        console.error("Delete Activity Error", error);
    }
}

async function searchActivities() {

    const title = document.getElementById('search-title').value;
    const location = document.getElementById('search-location').value;
    const maxPrice = document.getElementById('search-price').value;

    let query = [];

    if(title) query.push(`title=${encodeURIComponent(title)}`);
    if(location) query.push(`location=${encodeURIComponent(location)}`);
    if(maxPrice) query.push(`maxPrice=${maxPrice}`);

    const url = `/activities/search?${query.join("&")}`;

    try {
        const data = await apiFetch(url);
        renderActivities(data);
    } catch(error) {
        console.error(error);
    }
}

async function editActivity(id) {

    const title =
        prompt("Neuer Titel:");

    const location =
        prompt("Neuer Ort:");

    const price =
        prompt("Neuer Preis:");

    const description =
        prompt("Neue Beschreibung:");

    const userLimit =
        prompt("Neues Teilnehmerlimit:");

    if (!title || !location) return;

    const payload = {
        title,
        location,
        price: parseInt(price),
        description,
        userLimit: parseInt(userLimit)
    };

    try {

        await apiFetch(`/activities/update/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });

        showAlert("Aktivität aktualisiert!");

        loadActivities(currentFilter);

    } catch (error) {

        console.error(error);

    }
}

function openEditBox(act) {

    document
        .getElementById('edit-activity-box')
        .classList.remove('hidden');

    document.getElementById('edit-id').value = act.id;
    document.getElementById('edit-title').value = act.title;
    document.getElementById('edit-description').value = act.description;
    document.getElementById('edit-location').value = act.location;
    document.getElementById('edit-price').value = act.price;
    document.getElementById('edit-limit').value = act.userLimit;
}

function closeEditBox() {

    document
        .getElementById('edit-activity-box')
        .classList.add('hidden');
}

async function submitEditActivity(event) {

    event.preventDefault();

    const id = document.getElementById('edit-id').value;

    const payload = {
        title: document.getElementById('edit-title').value,
        description: document.getElementById('edit-description').value,
        location: document.getElementById('edit-location').value,
        price: parseInt(document.getElementById('edit-price').value),
        userLimit: parseInt(document.getElementById('edit-limit').value)
    };

    try {
        await apiFetch(`/activities/update/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        showAlert("Aktivität aktualisiert!");
        closeEditBox();
        loadActivities(currentFilter);
    } catch (error) {
        console.error(error);
    }

    function openUser(userId) {
        console.log("User öffnen:", userId);
        // später:
        // Profil anzeigen
    }
}
