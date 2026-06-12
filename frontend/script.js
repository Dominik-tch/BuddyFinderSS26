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
        // 1. Create the main card container
        const card = document.createElement('div');
        card.className = "activity-card";
        
        const contentDiv = document.createElement('div');

        // 2. Build the Header (Title & Owner)
        const headerDiv = document.createElement('div');
        headerDiv.className = "activity-header";
        
        const titleH4 = document.createElement('h4');
        titleH4.textContent = act.title || 'Untitled';
        
        const ownerSpan = document.createElement('span');
        ownerSpan.className = "badge";
        ownerSpan.textContent = `Created by: ${act.owner || 'Unknown'}`;
        
        headerDiv.append(titleH4, ownerSpan);

        // 3. Build Meta Info (Location/Map, Price, Limit)
        const metaDiv = document.createElement('div');
        metaDiv.className = "activity-meta";

        if (act.latitude && act.longitude) {
            const mapAnchor = document.createElement('a');
            mapAnchor.href = `https://maps.google.com/?q=${act.latitude},${act.longitude}`;
            mapAnchor.target = "_blank";
            mapAnchor.className = "map-link";
            mapAnchor.textContent = `📍 ${act.location}`;
            metaDiv.appendChild(mapAnchor);
        } else {
            const locationSpan = document.createElement('span');
            locationSpan.textContent = `📍 ${act.location}`;
            metaDiv.appendChild(locationSpan);
        }

        const detailsSpan = document.createElement('span');
        detailsSpan.textContent = `  |  💰 ${act.price}€  |  👥 Limit: ${act.currentParticipants}/${act.userLimit} Participants`;
        metaDiv.appendChild(detailsSpan);

        // 4. Build Weather
        const weatherDiv = document.createElement('div');
        weatherDiv.className = "activity-weather";
        weatherDiv.textContent = `🌤️ Weather: ${act.weather || 'No weather data available'}`;

        // 5. Build Description
        const descP = document.createElement('p');
        descP.className = "activity-desc";
        descP.textContent = act.description || 'No description';

        // 6. Build Participants List
        const participantsP = document.createElement('p');
        participantsP.className = "participants-list";
        participantsP.textContent = "Participants: ";
        
        if (act.participants && act.participants.length > 0) {
            act.participants.forEach((user, index) => {
                const userSpan = document.createElement('span');
                userSpan.className = "participant-link";
                userSpan.textContent = user.userName;
                // Add click listener safely
                userSpan.onclick = () => openUser(user.id);
                participantsP.appendChild(userSpan);

                // Add commas between names
                if (index < act.participants.length - 1) {
                    participantsP.appendChild(document.createTextNode(", "));
                }
            });
        } else {
            participantsP.appendChild(document.createTextNode("None"));
        }

        // Combine all content parts
        contentDiv.append(headerDiv, metaDiv, weatherDiv, descP, participantsP);

        // 7. Build Action Buttons
        const actionsDiv = document.createElement('div');
        actionsDiv.className = "activity-actions";

        if (currentFilter === 'getAllJoined') {
            const leaveBtn = document.createElement('button');
            leaveBtn.className = "btn btn-danger";
            leaveBtn.textContent = "Leave";
            leaveBtn.onclick = () => leaveActivity(act.id);
            actionsDiv.appendChild(leaveBtn);
        } else {
            const joinBtn = document.createElement('button');
            joinBtn.className = "btn btn-secondary";
            joinBtn.textContent = "Join";
            joinBtn.onclick = function() { joinActivity(act.id, this); };
            actionsDiv.appendChild(joinBtn);
        }

        if (currentFilter === 'getAllOwned') {
            const editBtn = document.createElement('button');
            editBtn.className = "btn btn-primary";
            editBtn.textContent = "Edit";
            editBtn.onclick = () => openEditBox(act);

            const deleteBtn = document.createElement('button');
            deleteBtn.className = "btn btn-danger";
            deleteBtn.textContent = "Delete";
            deleteBtn.onclick = () => deleteActivity(act.id);

            actionsDiv.append(editBtn, deleteBtn);
        }

        // Attach content and actions to the card, and add card to the DOM
        card.append(contentDiv, actionsDiv);
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