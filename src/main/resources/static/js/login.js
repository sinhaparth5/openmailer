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

function toggleTwoFactor(forceOpen = null) {
    const section = document.getElementById('twoFactorSection');
    const toggleButton = document.getElementById('twoFactorToggle');
    const shouldOpen = forceOpen === null ? section.classList.contains('hidden') : forceOpen;

    section.classList.toggle('hidden', !shouldOpen);
    toggleButton.setAttribute('aria-expanded', shouldOpen ? 'true' : 'false');

    if (shouldOpen) {
        setTimeout(() => {
            document.getElementById('twoFactorCode')?.focus();
        }, 100);
    }
}

function setTwoFactorStepActive(isActive) {
    const buttonText = document.getElementById('buttonText');
    const helpText = document.getElementById('twoFactorHelpText');
    const toggleButton = document.getElementById('twoFactorToggle');
    const stepIndicator = document.getElementById('loginStepIndicator');
    const primaryCredentialSection = document.getElementById('primaryCredentialSection');
    const formActions = document.getElementById('loginFormActions');
    const submitButton = document.getElementById('submitButton');
    const rememberMeInput = document.getElementById('rememberMe');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');

    if (isActive) {
        toggleTwoFactor(true);
        stepIndicator.classList.remove('hidden');
        buttonText.textContent = 'Verify & Sign In';
        helpText.textContent = 'This account has two-factor authentication enabled. Enter your 6-digit authenticator code or 8-character backup code to continue.';
        toggleButton.textContent = 'Two-factor code required';
        toggleButton.classList.add('text-slate-900');
        primaryCredentialSection.classList.add('opacity-45');
        formActions.classList.add('opacity-70');
        submitButton.classList.remove('app-button-primary');
        submitButton.classList.add('bg-[#171717]');
        rememberMeInput.disabled = true;
        emailInput.readOnly = true;
        passwordInput.readOnly = true;
    } else {
        stepIndicator.classList.add('hidden');
        toggleTwoFactor(false);
        buttonText.textContent = 'Sign In';
        helpText.textContent = 'Use a 6-digit authenticator code or an 8-character backup code.';
        toggleButton.textContent = 'Need 2FA or backup code?';
        toggleButton.classList.remove('text-slate-900');
        primaryCredentialSection.classList.remove('opacity-45');
        formActions.classList.remove('opacity-70');
        submitButton.classList.add('app-button-primary');
        submitButton.classList.remove('bg-[#171717]');
        rememberMeInput.disabled = false;
        emailInput.readOnly = false;
        passwordInput.readOnly = false;
    }
}

let pendingTwoFactorToken = null;

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

async function parseLoginResponse(response) {
    const contentType = response.headers.get('content-type') || '';

    if (contentType.includes('application/json')) {
        return response.json();
    }

    const bodyText = await response.text();
    return {
        success: false,
        message: extractLoginErrorMessage(response.status, bodyText)
    };
}

function extractLoginErrorMessage(status, bodyText) {
    if (status === 401) {
        return 'Invalid email or password.';
    }

    if (status === 403) {
        return 'This request was blocked. Refresh the page and try again.';
    }

    if (bodyText && !bodyText.trim().startsWith('<')) {
        return bodyText.trim();
    }

    return 'An unexpected error occurred. Please try again later.';
}

function requiresTwoFactor(result) {
    return Boolean(result?.data?.requiresTwoFactor);
}

// Handle form submission
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();

    const submitButton = document.getElementById('submitButton');
    const buttonText = document.getElementById('buttonText');
    const loadingSpinner = document.getElementById('loadingSpinner');
    let keepTwoFactorStepActive = !document.getElementById('twoFactorSection').classList.contains('hidden');

    // Get form values
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const twoFactorCode = document.getElementById('twoFactorCode').value || null;
    const rememberMe = document.getElementById('rememberMe').checked;

    let requestUrl = '/api/auth/login';
    let requestBody = {
        email: email,
        password: password,
        rememberMe: rememberMe
    };

    if (pendingTwoFactorToken) {
        requestUrl = '/api/auth/login/2fa';
        requestBody = {
            pendingTwoFactorToken: pendingTwoFactorToken,
            code: (twoFactorCode || '').trim().toUpperCase(),
            rememberMe: rememberMe
        };
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

        const response = await fetch(requestUrl, {
            method: 'POST',
            credentials: 'same-origin',
            headers: headers,
            body: JSON.stringify(requestBody)
        });

        const result = await parseLoginResponse(response);

        if (response.ok && result.success && requiresTwoFactor(result)) {
            pendingTwoFactorToken = result.data.pendingTwoFactorToken;
            keepTwoFactorStepActive = true;
            setTwoFactorStepActive(true);
            showAlert('Password verified. Enter your authenticator code or backup code to continue.', 'success');
            const twoFactorCodeInput = document.getElementById('twoFactorCode');
            if (twoFactorCodeInput) {
                twoFactorCodeInput.value = '';
                twoFactorCodeInput.focus();
            }
        } else if (response.ok && result.success) {
            pendingTwoFactorToken = null;
            showAlert('Login successful! Redirecting...', 'success');
            window.location.href = '/dashboard';
        } else {
            // Handle error response
            const errorMessage = result?.error?.message || result?.message || 'Login failed. Please check your credentials.';
            if (pendingTwoFactorToken) {
                keepTwoFactorStepActive = true;
                setTwoFactorStepActive(true);
                document.getElementById('twoFactorCode')?.focus();
            } else {
                pendingTwoFactorToken = null;
                keepTwoFactorStepActive = false;
                setTwoFactorStepActive(false);
            }
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
        setTwoFactorStepActive(keepTwoFactorStepActive);
    }
});
