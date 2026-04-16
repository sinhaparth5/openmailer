document.addEventListener('DOMContentLoaded', () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {};
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    document.querySelectorAll('.js-logout-form').forEach((form) => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();

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

    const profileToggle = document.querySelector('.js-profile-menu-toggle');
    const profileMenu = document.querySelector('.js-profile-menu');

    if (profileToggle && profileMenu) {
        const setMenuOpen = (isOpen) => {
            profileToggle.setAttribute('aria-expanded', String(isOpen));
            profileMenu.classList.toggle('hidden', !isOpen);
        };

        profileToggle.addEventListener('click', () => {
            const isOpen = profileToggle.getAttribute('aria-expanded') === 'true';
            setMenuOpen(!isOpen);
        });

        document.addEventListener('click', (event) => {
            const clickedInsideMenu = profileMenu.contains(event.target) || profileToggle.contains(event.target);
            if (!clickedInsideMenu) {
                setMenuOpen(false);
            }
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                setMenuOpen(false);
                profileToggle.focus();
            }
        });
    }
});
