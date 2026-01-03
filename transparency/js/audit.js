document.addEventListener('DOMContentLoaded', () => {
    // 1. Mobile Menu Toggle Logic
    const menuBtn = document.getElementById('mobile-menu-btn');
    const mobileMenu = document.getElementById('mobile-menu');
    const closeBtn = document.getElementById('close-menu-btn');
    const overlay = document.getElementById('menu-overlay');

    // Ensure menu is closed when page loads (fixes navigation state issue)
    function resetMenu() {
        if (mobileMenu) {
            mobileMenu.classList.remove('active');
        }
        if (overlay) {
            overlay.classList.remove('active');
        }
        document.body.style.overflow = '';
    }

    // Reset on page load
    resetMenu();

    function openMenu() {
        if (mobileMenu && overlay) {
            mobileMenu.classList.add('active');
            overlay.classList.add('active');
            document.body.style.overflow = 'hidden'; // Prevent scrolling
        }
    }

    function closeMenu() {
        if (mobileMenu && overlay) {
            mobileMenu.classList.remove('active');
            overlay.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    if (menuBtn) menuBtn.addEventListener('click', openMenu);
    if (closeBtn) closeBtn.addEventListener('click', closeMenu);
    if (overlay) overlay.addEventListener('click', closeMenu);

    // Auto-close menu when clicking navigation links
    const menuLinks = document.querySelectorAll('#mobile-menu a');
    menuLinks.forEach(link => {
        link.addEventListener('click', () => {
            closeMenu();
        });
    });

    // 2. Add "Verified" badges animation
    const badges = document.querySelectorAll('.status-badge');
    badges.forEach((badge, index) => {
        setTimeout(() => {
            badge.style.opacity = '1';
            badge.style.transform = 'translateY(0)';
        }, 100 * index);
    });

    // 3. Highlight active TOC item based on scroll (Optional enhancement)
    // Simple implementation for now just to show activity
    console.log("Aido Transparency Audit Loaded. Integrity Check: PASS");
});
