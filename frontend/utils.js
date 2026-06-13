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
            if (response.status === 401) {
                localStorage.removeItem('sessionToken');
                window.location.href = 'index.html';
            }
            throw new Error(data.error || 'An error occurred');
        }
        return data;
    } catch (error) {
        showAlert(error.message, true);
        throw error;
    }
}