(function() {
    document.addEventListener("DOMContentLoaded", function() {

        // Theme toggle: light theme by default, persist choice in localStorage
        (function initThemeToggle() {
            var STORAGE_KEY = 'gs-theme';
            var html = document.documentElement;
            var checkbox = document.getElementById('gs-switch');

            function applyTheme(isDark) {
                html.setAttribute('data-gs-theme', isDark ? 'dark' : 'light');
                if (checkbox) checkbox.checked = isDark;
            }

            var saved = localStorage.getItem(STORAGE_KEY);
            var isDark = saved === 'dark';
            applyTheme(isDark);

            if (checkbox) {
                checkbox.addEventListener('change', function() {
                    isDark = checkbox.checked;
                    localStorage.setItem(STORAGE_KEY, isDark ? 'dark' : 'light');
                    applyTheme(isDark);
                });
            }
        })();

        // Mobile navigation: hamburger toggles overlay panel, ESC/backdrop/close button close it
        function initializeNavigationMenu() {
            const hamburger = document.querySelector('#navigation-menu');
            const mainNavigation = document.querySelector('#main-navigation');
            const navigationClose = document.querySelector('#navigation-close');
            const backdrop = document.querySelector('#header-backdrop');

            function openNavigation() {
                if (mainNavigation) mainNavigation.classList.add('is-open');
                if (backdrop) {
                    backdrop.classList.add('is-visible');
                    backdrop.setAttribute('aria-hidden', 'false');
                }
                if (hamburger) hamburger.setAttribute('aria-expanded', 'true');
                document.body.style.overflow = 'hidden';
            }

            function closeNavigation() {
                if (mainNavigation) mainNavigation.classList.remove('is-open');
                if (backdrop) {
                    backdrop.classList.remove('is-visible');
                    backdrop.setAttribute('aria-hidden', 'true');
                }
                if (hamburger) hamburger.setAttribute('aria-expanded', 'false');
                document.body.style.overflow = '';
            }

            function isNavigationOpen() {
                return mainNavigation && mainNavigation.classList.contains('is-open');
            }

            if (hamburger) {
                hamburger.addEventListener('click', function() {
                    isNavigationOpen() ? closeNavigation() : openNavigation();
                });
            }
            if (navigationClose) {
                navigationClose.addEventListener('click', closeNavigation);
            }
            if (backdrop) {
                backdrop.addEventListener('click', closeNavigation);
            }
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape' && isNavigationOpen()) closeNavigation();
            });

            window.addEventListener('resize', function() {
                if (window.innerWidth > 768 && isNavigationOpen()) closeNavigation();
            });
        }
        initializeNavigationMenu();

        // User dropdown: click avatar to toggle, close on outside click or ESC
        function initializeUserDropdown() {
            const trigger = document.querySelector('#user-avatar-trigger');
            const panel = document.querySelector('#user-dropdown-panel');
            if (!trigger || !panel) return;

            function open() {
                panel.removeAttribute('hidden');
                trigger.setAttribute('aria-expanded', 'true');
            }
            function close() {
                panel.setAttribute('hidden', '');
                trigger.setAttribute('aria-expanded', 'false');
            }
            function toggle() {
                if (panel.hasAttribute('hidden')) open(); else close();
            }

            trigger.addEventListener('click', function(e) {
                e.preventDefault();
                toggle();
            });
            trigger.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggle();
                }
            });
            document.addEventListener('click', function(e) {
                if (!trigger.contains(e.target) && !panel.contains(e.target)) close();
            });
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') close();
            });
        }
        initializeUserDropdown();

        // Initialize avatar initials from username text in the dropdown
        function initializeUserInitials() {
            var avatarInitials = document.querySelector('.gs-user-avatar .gs-user-initials');
            if (!avatarInitials) return;

            var usernameSpan = document.querySelector('.gs-user-dropdown-panel .username span');
            var username = usernameSpan && usernameSpan.textContent
                ? usernameSpan.textContent.trim()
                : '';

            if (username) {
                avatarInitials.textContent = username.charAt(0).toUpperCase();
            } else {
                // No username available: clear initials so CSS can show anonymous icon
                avatarInitials.textContent = '';
            }
        }
        initializeUserInitials();

        function applyResponsiveTableCards() {
            var tables = document.querySelectorAll('table');
        
            tables.forEach(function (table) {
                if (table.classList.contains('gs-mobile-card-table')) return;
        
                var thead = table.querySelector('thead');
                var tbody = table.querySelector('tbody');
                if (!thead || !tbody) return;
                var headerRow = thead.querySelector('tr');
                if (!headerRow) return;
                var headers = Array.from(headerRow.children).map(function (th) {
                    var headerText = th.textContent ? th.textContent.trim() : '';
                    if (!headerText) return null;
                    return headerText;
                });
        
                table.classList.add('gs-mobile-card-table');
        
                tbody.querySelectorAll('tr').forEach(function (row) {
                    Array.from(row.children).forEach(function (cell, index) {
        
                        var header = headers[index] || '';
                        if (cell.dataset.gsMobileFieldReady === 'true') return;
                        var field = document.createElement('div');
                        field.className = 'gs-mobile-field';
        
                        var fieldName = document.createElement('span');
                        fieldName.className = 'field-name';
                        fieldName.textContent = header;
                        var fieldValue = document.createElement('span');
                        fieldValue.className = 'field-value';
        
                        while (cell.firstChild) {
                            fieldValue.appendChild(cell.firstChild);
                        }
                        field.appendChild(fieldName);
                        field.appendChild(fieldValue);
                        cell.appendChild(field);
                        cell.dataset.gsMobileFieldReady = 'true';
                    });
                });
            });
        }

        function resetResponsiveTableCards() {
            var tables = document.querySelectorAll('table.gs-mobile-card-table');
            tables.forEach(function (table) {
                table.classList.remove('gs-mobile-card-table');
                table.querySelectorAll('tbody td[data-gs-mobile-field-ready="true"]').forEach(function (cell) {
                    var field = cell.querySelector('.gs-mobile-field');
                    if (!field) return;
                    var fieldValue = field.querySelector('.field-value');
                    if (fieldValue) {
                        while (fieldValue.firstChild) {
                            cell.insertBefore(fieldValue.firstChild, field);
                        }
                    }
                    field.remove();
                    delete cell.dataset.gsMobileFieldReady;
                });
            });
        }

        function syncResponsiveTableCards() {
            if (window.innerWidth < 768) {
                applyResponsiveTableCards();
                return;
            }
            resetResponsiveTableCards();
        }

        syncResponsiveTableCards();
    });
})();

