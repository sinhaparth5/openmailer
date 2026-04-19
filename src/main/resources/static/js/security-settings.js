const securityState = {
    twoFactorEnabled: false,
    backupCodes: []
};

function showAlert(message, type = 'error') {
    const container = document.getElementById('alertContainer');
    const box = document.getElementById('alertBox');
    const icon = document.getElementById('alertIcon');
    const messageEl = document.getElementById('alertMessage');

    container.classList.remove('hidden');
    messageEl.textContent = message;

    if (type === 'success') {
        box.className = 'app-flash app-flash-success';
        icon.className = 'mr-3 mt-0.5 h-5 w-5 flex-shrink-0 text-emerald-700';
        icon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>';
    } else {
        box.className = 'app-flash app-flash-error';
        icon.className = 'mr-3 mt-0.5 h-5 w-5 flex-shrink-0 text-rose-700';
        icon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M4.93 19h14.14c1.54 0 2.5-1.67 1.73-3L13.73 4c-.77-1.33-2.69-1.33-3.46 0L3.2 16c-.77 1.33.19 3 1.73 3z"></path>';
    }
}

function hideAlert() {
    document.getElementById('alertContainer').classList.add('hidden');
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

function getCsrfTokenPair() {
    return {
        token: document.querySelector('meta[name="_csrf"]')?.getAttribute('content'),
        header: document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
    };
}

async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'same-origin',
        headers: {
            ...getCsrfHeaders(),
            ...(options.headers || {})
        },
        ...options
    });
    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result?.error?.message || result?.message || 'Request failed');
    }

    return result;
}

function renderStatus() {
    document.getElementById('twoFactorSummary').textContent = securityState.twoFactorEnabled ? 'Enabled' : 'Not enabled';
    document.getElementById('twoFactorBadge').textContent = securityState.twoFactorEnabled ? 'Enabled' : 'Inactive';
    document.getElementById('backupCountBadge').textContent = `${securityState.backupCodes.length || Number(document.getElementById('securitySettingsRoot').dataset.backupCodeCount)} left`;
    document.getElementById('disableTwoFactorButton').classList.toggle('hidden', !securityState.twoFactorEnabled);
    document.getElementById('regenerateCodesButton').classList.toggle('hidden', !securityState.twoFactorEnabled);
    document.getElementById('startSetupButton').textContent = securityState.twoFactorEnabled ? 'Rotate 2FA Setup' : 'Start 2FA Setup';
}

function renderBackupCodes(codes) {
    const panel = document.getElementById('backupCodesPanel');
    const list = document.getElementById('backupCodesList');

    securityState.backupCodes = codes;
    document.getElementById('securitySettingsRoot').dataset.backupCodeCount = String(codes.length);
    document.getElementById('backupCountBadge').textContent = `${codes.length} left`;

    if (!codes.length) {
        panel.classList.add('hidden');
        list.innerHTML = '';
        return;
    }

    list.innerHTML = codes.map((code) => `
        <div class="rounded-[18px] border border-[rgba(57,48,36,0.08)] bg-white px-4 py-3 font-mono text-sm font-semibold tracking-[0.18em] text-slate-900">
            ${code}
        </div>
    `).join('');
    panel.classList.remove('hidden');
}

async function refreshStatus() {
    const result = await apiRequest('/api/auth/2fa/status', { method: 'GET', headers: {} });
    securityState.twoFactorEnabled = Boolean(result.data.enabled);
    document.getElementById('securitySettingsRoot').dataset.backupCodeCount = String(result.data.backupCodesRemaining);
    renderStatus();
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
    const password = document.getElementById('newPassword')?.value || '';
    const confirmPassword = document.getElementById('confirmNewPassword')?.value || '';
    const checks = getPasswordChecks(password, confirmPassword);

    setRuleState('passwordRuleLength', checks.length);
    setRuleState('passwordRuleUpper', checks.upper);
    setRuleState('passwordRuleLower', checks.lower);
    setRuleState('passwordRuleNumber', checks.number);
    setRuleState('passwordRuleSpecial', checks.special);
    setRuleState('passwordRuleMatch', checks.match);

    return checks;
}

document.getElementById('profileForm')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    try {
        const result = await apiRequest('/api/auth/me', {
            method: 'PUT',
            body: JSON.stringify({
                firstName: document.getElementById('firstName').value.trim() || null,
                lastName: document.getElementById('lastName').value.trim() || null,
                username: document.getElementById('username').value.trim(),
                email: document.getElementById('email').value.trim()
            })
        });
        showAlert(result.message || 'Profile updated successfully.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('avatarForm')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    const fileInput = document.getElementById('profileImageInput');
    const file = fileInput?.files?.[0];
    if (!file) {
        showAlert('Choose an image before uploading.');
        return;
    }

    const allowedTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
        showAlert('Only PNG, JPEG, GIF, or WEBP images are allowed.');
        return;
    }

    const maxBytes = 2 * 1024 * 1024;
    if (file.size > maxBytes) {
        showAlert('Profile images must be 2MB or smaller.');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    const { token, header } = getCsrfTokenPair();
    const headers = {};
    if (token && header) {
        headers[header] = token;
    }

    try {
        const response = await fetch('/api/auth/me/avatar', {
            method: 'POST',
            credentials: 'same-origin',
            headers,
            body: formData
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result?.error?.message || result?.message || 'Upload failed');
        }

        const avatarUrl = `/users/me/avatar?v=${Date.now()}`;
        const image = document.getElementById('profileAvatarImage');
        const fallback = document.getElementById('profileAvatarFallback');

        if (image) {
            image.src = avatarUrl;
            image.classList.remove('hidden');
        } else if (fallback) {
            const img = document.createElement('img');
            img.id = 'profileAvatarImage';
            img.alt = 'Profile picture';
            img.src = avatarUrl;
            img.className = 'h-24 w-24 rounded-full object-cover shadow-[0_10px_24px_-12px_rgba(15,23,42,0.45)]';
            fallback.replaceWith(img);
        }

        document.getElementById('removeAvatarButton')?.classList.remove('hidden');
        fileInput.value = '';
        showAlert(result.message || 'Profile picture updated successfully.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('removeAvatarButton')?.addEventListener('click', async () => {
    hideAlert();

    if (!window.confirm('Remove your current profile picture?')) {
        return;
    }

    try {
        const result = await apiRequest('/api/auth/me/avatar', {
            method: 'DELETE',
            body: JSON.stringify({})
        });

        const image = document.getElementById('profileAvatarImage');
        if (image) {
            const fallback = document.createElement('div');
            fallback.id = 'profileAvatarFallback';
            fallback.className = 'flex h-24 w-24 items-center justify-center rounded-full bg-gradient-to-br from-[var(--card-lime)]/65 to-[var(--card-mauve)]/65 text-2xl font-extrabold text-white shadow-[0_10px_24px_-12px_rgba(15,23,42,0.45)]';
            fallback.textContent = document.getElementById('securitySettingsRoot')?.dataset.currentUserInitials || 'U';
            image.replaceWith(fallback);
        }

        document.getElementById('removeAvatarButton')?.classList.add('hidden');
        showAlert(result.message || 'Profile picture removed successfully.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('changePasswordForm')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    const checks = updatePasswordFeedback();
    if (!checks.length || !checks.upper || !checks.lower || !checks.number || !checks.special) {
        showAlert('Password does not meet the required rules yet.');
        return;
    }

    try {
        const result = await apiRequest('/api/auth/change-password', {
            method: 'POST',
            body: JSON.stringify({
                currentPassword: document.getElementById('currentPassword').value,
                newPassword: document.getElementById('newPassword').value
            })
        });
        document.getElementById('changePasswordForm').reset();
        updatePasswordFeedback();
        showAlert(result.message || 'Password changed successfully.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('startSetupButton').addEventListener('click', async () => {
    hideAlert();

    try {
        const result = await apiRequest('/api/auth/2fa/setup', {
            method: 'POST',
            body: JSON.stringify({})
        });

        document.getElementById('qrCodeImage').src = result.data.qrCodeDataUrl;
        document.getElementById('secretText').textContent = result.data.secret;
        document.getElementById('setupPanel').classList.remove('hidden');
        document.getElementById('enableCode').focus();
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('cancelSetupButton').addEventListener('click', () => {
    document.getElementById('setupPanel').classList.add('hidden');
    document.getElementById('enableCode').value = '';
});

document.getElementById('confirmEnableButton').addEventListener('click', async () => {
    hideAlert();
    const code = document.getElementById('enableCode').value.trim();

    if (!/^[0-9]{6}$/.test(code)) {
        showAlert('Enter the 6-digit authenticator code.');
        return;
    }

    try {
        const result = await apiRequest('/api/auth/2fa/enable', {
            method: 'POST',
            body: JSON.stringify({ code })
        });

        securityState.twoFactorEnabled = true;
        renderBackupCodes(result.data.backupCodes || []);
        renderStatus();
        document.getElementById('setupPanel').classList.add('hidden');
        document.getElementById('enableCode').value = '';
        showAlert(result.data.message || 'Two-factor authentication enabled successfully.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('regenerateCodesButton').addEventListener('click', async () => {
    hideAlert();

    if (!window.confirm('Regenerate backup codes? Existing codes will stop working.')) {
        return;
    }

    try {
        const result = await apiRequest('/api/auth/2fa/backup-codes', {
            method: 'POST',
            body: JSON.stringify({})
        });
        renderBackupCodes(result.data.backupCodes || []);
        await refreshStatus();
        showAlert(result.data.message || 'Backup codes regenerated.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('disableTwoFactorButton').addEventListener('click', async () => {
    hideAlert();

    if (!window.confirm('Disable two-factor authentication for this account?')) {
        return;
    }

    try {
        await apiRequest('/api/auth/2fa/disable', {
            method: 'POST',
            body: JSON.stringify({})
        });
        securityState.twoFactorEnabled = false;
        renderBackupCodes([]);
        await refreshStatus();
        showAlert('Two-factor authentication disabled.', 'success');
    } catch (error) {
        showAlert(error.message);
    }
});

document.getElementById('copyBackupCodesButton').addEventListener('click', async () => {
    if (!securityState.backupCodes.length) {
        return;
    }

    try {
        await navigator.clipboard.writeText(securityState.backupCodes.join('\n'));
        showAlert('Backup codes copied to clipboard.', 'success');
    } catch (error) {
        console.error('Clipboard error:', error);
        showAlert('Unable to copy backup codes. Copy them manually instead.');
    }
});

securityState.twoFactorEnabled = document.getElementById('securitySettingsRoot').dataset.twoFactorEnabled === 'true';
document.getElementById('newPassword')?.addEventListener('input', updatePasswordFeedback);
document.getElementById('confirmNewPassword')?.addEventListener('input', updatePasswordFeedback);
updatePasswordFeedback();
renderStatus();
