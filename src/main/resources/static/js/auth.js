document.querySelectorAll('.js-logout-form').forEach((form) => {
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
                headers
            });
        } finally {
            window.location.href = '/login';
        }
    });
});
