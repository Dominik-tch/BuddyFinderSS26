document.addEventListener('DOMContentLoaded', () => {
    // Session Guard
    if (!getToken()) {
        window.location.href = 'index.html';
        return;
    }
    
    loadProfile();
});

async function loadProfile() {
    const saveBtn = document.getElementById('prof-save-btn');
    saveBtn.disabled = true;
    saveBtn.textContent = "Loading...";

    try {
        // Fetch the profile data
        const userData = await apiFetch('/users/editProfile');
        
        // Populate the form
        document.getElementById('prof-username').value = userData.userName || '';
        document.getElementById('prof-email').value = userData.email || '';
        document.getElementById('prof-firstName').value = userData.firstName || '';
        document.getElementById('prof-lastName').value = userData.lastName || '';
        
    } catch (error) {
        console.error("Failed to load profile", error);
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "Save Changes";
    }
}

async function handleProfileUpdate(event) {
    event.preventDefault();
    const saveBtn = document.getElementById('prof-save-btn');
    
    const payload = {
        email: document.getElementById('prof-email').value,
        firstName: document.getElementById('prof-firstName').value,
        lastName: document.getElementById('prof-lastName').value
    };

    saveBtn.disabled = true;
    saveBtn.textContent = "Saving...";

    try {
        // Update the profile data
        await apiFetch('/users/editProfile', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        
        showAlert('Profile updated successfully!');
    } catch (error) {
        console.error("Failed to update profile", error);
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "Save Changes";
    }
}