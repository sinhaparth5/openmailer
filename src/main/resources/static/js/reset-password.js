function showAlert(message, type = 'error') {
    const container = document.getElementById('alertContainer');
    const box = document.getElementById('alertBox');
    const icon = document.getElementById('alertIcon');
    const messageEl = document.getElementById('alertMessage');

    container.classList.remove('hidden');
    messageEl.textContent = message;

    if (type === 'success') {
        box.className = 'rounded-xl p-4 bg-emerald-50 border border-emerald-200';
        icon.className = 'mr-3 mt-0.5 h-5 w-5 flex-shrink-0 text-emerald-600';
        icon.innerHTML = '<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>';
    } else {
        box.className = 'rounded-xl p-4 bg-rose-50 border border-rose-200';
        icon.className = 'mr-3 mt-0.5 h-5 w-5 flex-shrink-0 text-rose-600';
        icon.innerHTML = '<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path>';
    }
}

function hideAlert() {
    document.getElementById('alertContainer').classList.add('hidden');
}

function setLoadingState(isLoading) {
    const submitButton = document.getElementById('submitButton');
    const buttonText = document.getElementById('buttonText');
    const loadingSpinner = document.getElementById('loadingSpinner');

    submitButton.disabled = isLoading;
    buttonText.textContent = isLoading ? 'Saving password...' : 'Save New Password';
    loadingSpinner.classList.toggle('hidden', !isLoading);
}

function setFormEnabled(enabled) {
    document.querySelectorAll('#resetPasswordForm input, #resetPasswordForm button').forEach((element) => {
        element.disabled = !enabled;
    });
}

function getCsrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

function getPasswordChecks(password, confirmPassword) {
    return {
        length: password.length >= 8,
        upper: /[A-Z]/.test(password),
        lower: /[a-z]/.test(password),
        number: /\d/.test(password),
        special: /[^A-Za-z\d]/.test(password),
        match: password.length > 0 && password === confirmPassword
    };
}

function setRuleState(elementId, isValid) {
    const element = document.getElementById(elementId);
    if (!element) {
        return;
    }

    const dot = element.querySelector('span');
    element.classList.toggle('text-emerald-700', isValid);
    element.classList.toggle('text-slate-600', !isValid);
    dot.classList.toggle('bg-emerald-500', isValid);
    dot.classList.toggle('bg-slate-300', !isValid);
}

function updatePasswordFeedback() {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const checks = getPasswordChecks(password, confirmPassword);

    setRuleState('passwordRuleLength', checks.length);
    setRuleState('passwordRuleUpper', checks.upper);
    setRuleState('passwordRuleLower', checks.lower);
    setRuleState('passwordRuleNumber', checks.number);
    setRuleState('passwordRuleSpecial', checks.special);
    setRuleState('passwordRuleMatch', checks.match);

    return checks;
}

async function validateToken() {
    const root = document.getElementById('resetPasswordRoot');
    const token = root.dataset.resetToken;

    if (!token) {
        setFormEnabled(false);
        showAlert('Reset link is missing. Request a new password reset email.');
        return null;
    }

    try {
        const response = await fetch(`/api/auth/reset-password/validate?token=${encodeURIComponent(token)}`, {
            method: 'GET',
            credentials: 'same-origin'
        });
        const result = await response.json();
        const isValid = Boolean(result?.data);

        if (!response.ok || !isValid) {
            setFormEnabled(false);
            showAlert('Reset link is invalid or has expired. Request a new one.');
            return null;
        }

        return token;
    } catch (error) {
        console.error('Reset token validation error:', error);
        setFormEnabled(false);
        showAlert('Unable to validate reset link right now.');
        return null;
    }
}

document.getElementById('resetPasswordForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    const token = document.getElementById('resetPasswordRoot').dataset.resetToken;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const checks = updatePasswordFeedback();

    if (!checks.length || !checks.upper || !checks.lower || !checks.number || !checks.special) {
        showAlert('Password does not meet the required rules yet.');
        return;
    }

    if (password !== confirmPassword) {
        showAlert('Passwords do not match.');
        return;
    }

    setLoadingState(true);

    try {
        const response = await fetch('/api/auth/reset-password', {
            method: 'POST',
            credentials: 'same-origin',
            headers: getCsrfHeaders(),
            body: JSON.stringify({ token, password })
        });
        const result = await response.json();

        if (!response.ok || !result.success) {
            showAlert(result?.error?.message || result?.message || 'Unable to reset password.');
            return;
        }

        showAlert(result.message || 'Password reset successfully. Redirecting to sign in...', 'success');
        setFormEnabled(false);
        setTimeout(() => {
            window.location.href = '/login';
        }, 1200);
    } catch (error) {
        console.error('Reset password error:', error);
        showAlert('An unexpected error occurred. Please try again.');
    } finally {
        setLoadingState(false);
    }
});

document.getElementById('password').addEventListener('input', updatePasswordFeedback);
document.getElementById('confirmPassword').addEventListener('input', updatePasswordFeedback);

updatePasswordFeedback();
validateToken();
