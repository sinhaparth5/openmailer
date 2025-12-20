// Toggle password visibility
function togglePasswordVisibility() {
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');
    const eyeSlashIcon = document.getElementById('eyeSlashIcon');
    const toggleButton = document.getElementById('togglePasswordButton');

    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        eyeIcon.classList.add('hidden');
        eyeSlashIcon.classList.remove('hidden');
        toggleButton.setAttribute('aria-label', 'Hide password');
        toggleButton.setAttribute('aria-pressed', 'true');
    } else {
        passwordInput.type = 'password';
        eyeIcon.classList.remove('hidden');
        eyeSlashIcon.classList.add('hidden');
        toggleButton.setAttribute('aria-label', 'Show password');
        toggleButton.setAttribute('aria-pressed', 'false');
    }
}

// Toggle Two-Factor authentication section
function toggleTwoFactor() {
    const section = document.getElementById('twoFactorSection');
    const toggleButton = document.getElementById('twoFactorToggle');
    const isHidden = section.classList.contains('hidden');

    section.classList.toggle('hidden');
    toggleButton.setAttribute('aria-expanded', isHidden ? 'true' : 'false');

    // Focus on the 2FA input when shown
    if (isHidden) {
        setTimeout(() => {
            document.getElementById('twoFactorCode').focus();
        }, 100);
    }
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
    submitButton.setAttribute('aria-busy', 'true');
    buttonText.textContent = 'Signing in...';
    loadingSpinner.classList.remove('hidden');
    const loadingText = document.getElementById('loadingText');
    if (loadingText) {
        loadingText.classList.remove('hidden');
    }

    try {
        // Get CSRF token from meta tags
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        // Prepare headers
        const headers = {
            'Content-Type': 'application/json',
        };

        // Add CSRF token to headers if available
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: headers,
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
        submitButton.setAttribute('aria-busy', 'false');
        buttonText.textContent = 'Sign In';
        loadingSpinner.classList.add('hidden');
        const loadingText = document.getElementById('loadingText');
        if (loadingText) {
            loadingText.classList.add('hidden');
        }
    }
});
