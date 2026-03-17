(function() {
    'use strict';

    document.addEventListener("DOMContentLoaded", function() {
        document.addEventListener('click', function(event) {
            var isToggle = event.target.matches('.gs-breadcrumb-dropdown-toggle');
            var isMenu = event.target.closest('.gs-breadcrumb-dropdown-menu');
            if (isToggle) {
                var menu = event.target.nextElementSibling;
                if (menu && menu.classList.contains('gs-breadcrumb-dropdown-menu')) {
                    menu.classList.toggle('show');
                }
            } 
            if (!isToggle && !isMenu) {
                var dropdowns = document.querySelectorAll('.gs-breadcrumb-dropdown-menu.show');
                dropdowns.forEach(function(dropdown) {
                    dropdown.classList.remove('show');
                });
            }
        });
    });

})();