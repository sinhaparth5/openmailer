document.addEventListener('DOMContentLoaded', function() {
    const mobileMenuButton = document.getElementById('mobileMenuButton');
    const mobileMenu = document.getElementById('mobileMenu');
    const mobileMenuBackdrop = document.getElementById('mobileMenuBackdrop');

    if (mobileMenuButton && mobileMenu && mobileMenuBackdrop) {
        const setOpenState = (isOpen) => {
            mobileMenuButton.setAttribute('aria-expanded', String(isOpen));
            mobileMenu.classList.toggle('hidden', !isOpen);
            mobileMenuBackdrop.classList.toggle('hidden', !isOpen);
            document.body.classList.toggle('overflow-hidden', isOpen);
        };

        mobileMenuButton.addEventListener('click', function() {
            const isExpanded = mobileMenuButton.getAttribute('aria-expanded') === 'true';
            setOpenState(!isExpanded);
        });

        mobileMenuBackdrop.addEventListener('click', function() {
            setOpenState(false);
        });

        document.addEventListener('click', function(e) {
            const clickedInsideNav = mobileMenu.contains(e.target) || mobileMenuButton.contains(e.target);
            if (!clickedInsideNav && mobileMenuButton.getAttribute('aria-expanded') === 'true') {
                setOpenState(false);
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && !mobileMenu.classList.contains('hidden')) {
                setOpenState(false);
                mobileMenuButton.focus();
            }
        });

        window.addEventListener('resize', function() {
            if (window.innerWidth >= 768 && mobileMenuButton.getAttribute('aria-expanded') === 'true') {
                setOpenState(false);
            }
        });
    }
});
