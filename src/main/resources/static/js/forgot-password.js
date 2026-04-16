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
    buttonText.textContent = isLoading ? 'Sending link...' : 'Send Reset Link';
    loadingSpinner.classList.toggle('hidden', !isLoading);
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

document.getElementById('forgotPasswordForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();
    setLoadingState(true);

    try {
        const response = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            credentials: 'same-origin',
            headers: getCsrfHeaders(),
            body: JSON.stringify({
                email: document.getElementById('email').value.trim()
            })
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            showAlert(result?.error?.message || result?.message || 'Unable to send reset email right now.');
            return;
        }

        showAlert(result.message || 'If an account exists for that email, a reset link has been sent.', 'success');
    } catch (error) {
        console.error('Forgot password error:', error);
        showAlert('An unexpected error occurred. Please try again.');
    } finally {
        setLoadingState(false);
    }
});
