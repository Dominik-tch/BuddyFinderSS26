// --- CONFIGURATION & STATE ---
const BASE_URL = 'http://localhost:8081/api'; 

// --- UTILS ---
function showAlert(message, isError = false) {
    const alertBox = document.getElementById('alert-box');
    alertBox.textContent = message;
    alertBox.className = 'alert ' + (isError ? 'alert-error' : 'alert-success');
    alertBox.classList.remove('hidden');
    
    setTimeout(() => { alertBox.classList.add('hidden'); }, 4000);
}

async function apiFetch(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    const fetchOptions = {
        ...options,
        headers,
        credentials: 'include' 
    };

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, fetchOptions);
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw { status: response.status, message: data.error || 'Ein Fehler ist aufgetreten' };
        }
        return data;
    } catch (error) {
        if (error.status !== 401) {
            showAlert(error.message || 'Ein Fehler ist aufgetreten', true);
        }
        throw error;
    }
}