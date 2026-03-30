(function() {
    'use strict';

    // Use event delegation on document — works for both full page loads and Wicket AJAX re-renders
    document.addEventListener('click', function(event) {
        var isToggle = event.target.closest('.gs-dropdown-header');
        var isMenu = event.target.closest('.gs-dropdown-menu');
        if (isToggle) {
            var menu = isToggle.nextElementSibling;
            if (menu && menu.classList.contains('gs-dropdown-menu')) {
                menu.classList.toggle('show');
                var expanded = menu.classList.contains('show');
                isToggle.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            }
        }
        if (!isToggle && !isMenu) {
            var dropdowns = document.querySelectorAll('.gs-dropdown-menu.show');
            dropdowns.forEach(function(dropdown) {
                var toggle = dropdown.previousElementSibling;
                if (toggle && toggle.classList && toggle.classList.contains('gs-dropdown-header')) {
                    toggle.setAttribute('aria-expanded', 'false');
                }
                dropdown.classList.remove('show');
            });
        }
    });

    // Enable keyboard interaction for the breadcrumb context menu trigger.
    document.addEventListener('keydown', function(event) {
        var isToggle = event.target.closest('.gs-dropdown-header');
        if (!isToggle) return;
        if (event.key !== 'Enter' && event.key !== ' ') return;

        event.preventDefault();
        var menu = isToggle.nextElementSibling;
        if (menu && menu.classList.contains('gs-dropdown-menu')) {
            menu.classList.toggle('show');
            var expanded = menu.classList.contains('show');
            isToggle.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        }
    });

})();
