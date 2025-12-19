// Toggle Two-Factor authentication section
function toggleTwoFactor() {
    const section = document.getElementById('twoFactorSection');
    section.classList.toggle('hidden');
}

// Show alert message
function showAlert(message, type = 'error') {
    const container = document.getElementById('alertContainer');
    const box = document.getElementById('alertBox');
    const icon = document.getElementById('alertIcon');
    const messageEl = document.getElementById('alertMessage');

    container.classList.remove('hidden');
    messageEl.textContent = message;

    if (type === 'error') {
        box.className = 'rounded-lg p-4 bg-red-50 border border-red-200';
        icon.className = 'w-5 h-5 flex-shrink-0 mr-3 mt-0.5 text-red-600';
        icon.innerHTML = '<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path>';
    } else if (type === 'success') {
        box.className = 'rounded-lg p-4 bg-green-50 border border-green-200';
        icon.className = 'w-5 h-5 flex-shrink-0 mr-3 mt-0.5 text-green-600';
        icon.innerHTML = '<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>';
    }
}

// Hide alert message
function hideAlert() {
    document.getElementById('alertContainer').classList.add('hidden');
}

// Handle form submission
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();

    const submitButton = document.getElementById('submitButton');
    const buttonText = document.getElementById('buttonText');
    const loadingSpinner = document.getElementById('loadingSpinner');

    // Get form values
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const twoFactorCode = document.getElementById('twoFactorCode').value || null;
    const rememberMe = document.getElementById('rememberMe').checked;

    // Prepare request body
    const loginData = {
        email: email,
        password: password
    };

    if (twoFactorCode && twoFactorCode.length === 6) {
        loginData.twoFactorCode = twoFactorCode;
    }

    // Disable button and show loading
    submitButton.disabled = true;
    buttonText.textContent = 'Signing in...';
    loadingSpinner.classList.remove('hidden');

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(loginData)
        });

        const result = await response.json();

        if (response.ok && result.success) {
            // Store tokens
            if (result.data.accessToken) {
                if (rememberMe) {
                    localStorage.setItem('accessToken', result.data.accessToken);
                    if (result.data.refreshToken) {
                        localStorage.setItem('refreshToken', result.data.refreshToken);
                    }
                } else {
                    sessionStorage.setItem('accessToken', result.data.accessToken);
                    if (result.data.refreshToken) {
                        sessionStorage.setItem('refreshToken', result.data.refreshToken);
                    }
                }
            }

            showAlert('Login successful! Redirecting...', 'success');

            // Redirect to dashboard or home page
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);
        } else {
            // Handle error response
            const errorMessage = result.message || 'Login failed. Please check your credentials.';
            showAlert(errorMessage, 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showAlert('An error occurred. Please try again later.', 'error');
    } finally {
        // Re-enable button and hide loading
        submitButton.disabled = false;
        buttonText.textContent = 'Sign In';
        loadingSpinner.classList.add('hidden');
    }
});
