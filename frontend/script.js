// --- CONFIGURATION ---
const BASE_URL = 'http://localhost:8081/api'; // Make sure the port is correct!

// --- STATE ---
let isLoginMode = true;
let currentFilter = 'getAll'; 

// --- INITIALIZATION ---
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
            throw new Error(data.error || 'An error occurred');
        }
        return data;
    } catch (error) {
        showAlert(error.message, true);
        throw error;
    }
}

// --- UI LOGIC ---
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
    
    // Update UI texts
    document.getElementById('auth-title').textContent = isLoginMode ? 'Login' : 'Register';
    document.getElementById('auth-btn-text').textContent = isLoginMode ? 'Login' : 'Create Account';
    document.getElementById('auth-toggle-link').textContent = isLoginMode ? "Don't have an account yet? Register here." : "Already have an account? Login here.";
    
    // Toggle fields and required attribute
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
    
    // Basic payload
    let payload = {
        userName: document.getElementById('username').value,
        password: document.getElementById('password').value
    };

    // Additional fields if it's a registration
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
            showAlert('Successfully logged in!');
            checkAuthStatus();
        } else if (!isLoginMode) {
            showAlert('Successfully registered! Please log in now.');
            toggleAuthMode();
            // Clear form for a clean login
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
    showAlert('Successfully logged out.');
}

// --- ACTIVITY API ---
async function loadActivities(filterType) {
    currentFilter = filterType;
    updateTabUI(filterType);

    let title = "All Activities";
    if(filterType === 'getAllOwned') title = "My Created Activities";
    if(filterType === 'getAllJoined') title = "My Joined Activities";
    document.getElementById('list-title').textContent = title;

    const container = document.getElementById('activities-container');
    container.innerHTML = '<p>Loading activities...</p>';

    try {
        const data = await apiFetch(`/activities/${filterType}`);
        renderActivities(data);
    } catch (error) {
        container.innerHTML = `<p style="color: var(--danger)">Error loading: ${error.message}</p>`;
    }
}

function renderActivities(activities) {
    const container = document.getElementById('activities-container');
    container.innerHTML = '';

    if (!activities || activities.length === 0) {
        container.innerHTML = '<p>No activities found.</p>';
        return;
    }

    activities.forEach(act => {
        const card = document.createElement('div');
        card.className = "activity-card";
        
        // --- NEW LOGIC: Generate the clickable map link if coordinates exist ---
        const mapLink = (act.latitude && act.longitude) 
            ? `<a href="https://www.google.com/maps/search/?api=1&query=${act.latitude},${act.longitude}" target="_blank" style="color: var(--brand); text-decoration: underline;">📍 ${act.location}</a>` 
            : `📍 ${act.location}`;
        
        card.innerHTML = `
            <div>
                <div class="activity-header">
                    <h4>${act.title || 'Untitled'}</h4>
                    <span class="badge">Created by: ${act.owner || 'Unknown'}</span>
                </div>
                
                <div style="font-size: 0.85rem; color: var(--brand); margin-bottom: 0.5rem; font-weight: bold;">
                    ${mapLink} &nbsp;|&nbsp; 💰 ${act.price}€ &nbsp;|&nbsp; 👥 Limit: ${act.currentParticipants}/${act.userLimit} Participants
                </div>
                
                <p class="activity-desc">${act.description || 'No description'}</p>
                <p class="participants-list">Participants:${act.participants?.map(user => 
                `<span class="participant-link" onclick="openUser('${user.id}')">${user.userName}</span>`).join(", ") || "None"}
                </p>
            </div>
            <div class="activity-actions">
                ${currentFilter === 'getAllJoined' ? `<button onclick="leaveActivity('${act.id}')" class="btn btn-danger">Leave</button>`
                : `<button onclick="joinActivity('${act.id}', this)" class="btn btn-secondary">Join</button>`
}
                ${currentFilter === 'getAllOwned' ? `<button onclick='openEditBox(${JSON.stringify(act)})' class="btn btn-primary">Edit</button>
                                                    <button onclick="deleteActivity('${act.id}')" class="btn btn-danger">Delete</button>` : ''}
            </div>
        `;
        container.appendChild(card);
    });
}

async function handleAddActivity(event) {
    event.preventDefault();
    
    // Read data and ensure correct data types (int for Price and Limit)
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
        showAlert('Activity successfully created!');
        document.getElementById('add-activity-form').reset();
        loadActivities(currentFilter);
    } catch (error) {
        console.error("Add Activity Error", error);
    }
}

async function joinActivity(id, button) {

    button.disabled = true;
    button.textContent = "Joined";
    try {
        await apiFetch(`/activities/join/${id}`, {
            method: 'POST'
        });
        showAlert('Successfully joined!');
    } catch (error) {
        button.disabled = false;
        button.textContent = "Join";
        console.error(error);
    }
}

async function leaveActivity(id) {
    try {
        await apiFetch(`/activities/leave/${id}`, {
            method: 'DELETE'
        });
        showAlert('Left activity.');
        loadActivities(currentFilter);
    } catch (error) {
        console.error(error);
    }
}

async function deleteActivity(id) {
    if(!confirm('Are you sure you want to delete this activity?')) return;
    
    try {
        await apiFetch(`/activities/delete/${id}`, { method: 'DELETE' });
        showAlert('Activity deleted.');
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
        prompt("New Title:");

    const location =
        prompt("New Location:");

    const price =
        prompt("New Price:");

    const description =
        prompt("New Description:");

    const userLimit =
        prompt("New Participant Limit:");

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

        showAlert("Activity updated!");

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
        showAlert("Activity updated!");
        closeEditBox();
        loadActivities(currentFilter);
    } catch (error) {
        console.error(error);
    }

    function openUser(userId) {
        console.log("Open user:", userId);
        // later:
        // show profile
    }
}