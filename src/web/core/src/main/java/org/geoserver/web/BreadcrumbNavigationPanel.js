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
            }
        }
        if (!isToggle && !isMenu) {
            var dropdowns = document.querySelectorAll('.gs-dropdown-menu.show');
            dropdowns.forEach(function(dropdown) {
                dropdown.classList.remove('show');
            });
        }
    });

})();
