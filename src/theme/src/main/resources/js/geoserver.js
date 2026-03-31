(function () {
    document.addEventListener("DOMContentLoaded", function () {
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
        // Helpers
        function debounce(fn, delay) {
            let t;
            return function () {
                clearTimeout(t);
                t = setTimeout(fn, delay);
            };
        }
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
        // Navigation Tabs
        function initializeNavigationTabs() {
            const navRoot = document.querySelector('#navigation');
            if (!navRoot) return;
            const tabs = Array.from(navRoot.querySelectorAll('.navigation-tab'));
            const headers = Array.from(navRoot.querySelectorAll('.navigation-tab-header'));
            if (!tabs.length || !headers.length) return;
            const mobileQuery = window.matchMedia('(max-width: 768px)');
            function isMobile() {
                return mobileQuery.matches;
            }
            function setTabState(tab, open) {
                const trigger = tab.querySelector('.navigation-tab-header');
                tab.classList.toggle('is-open', open);
                if (trigger) trigger.setAttribute('aria-expanded', open ? 'true' : 'false');
            }
            function closeAll() {
                tabs.forEach(tab => setTabState(tab, false));
            }
            function openExclusive(tab) {
                closeAll();
                setTabState(tab, true);
            }
            function toggleTab(header) {
                if (!header || isMobile()) return;
                const tab = header.closest('.navigation-tab');
                if (!tab) return;
                const isOpen = tab.classList.contains('is-open');
                isOpen ? setTabState(tab, false) : openExclusive(tab);
            }
            headers.forEach(header => {
                header.addEventListener('click', function (e) {
                    e.preventDefault();
                    toggleTab(header);
                });
                header.addEventListener('keydown', function (e) {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        toggleTab(header);
                    }
                });
                header.addEventListener('focus', function () {
                    if (!isMobile()) {
                        const tab = header.closest('.navigation-tab');
                        if (tab) openExclusive(tab);
                    }
                });
            });
            document.addEventListener('click', function (e) {
                if (!navRoot.contains(e.target) && !isMobile()) closeAll();
            });
            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape' && !isMobile()) closeAll();
            });
            function syncForViewport() {
                if (isMobile()) {
                    tabs.forEach(tab => setTabState(tab, true));
                } else {
                    closeAll();
                }
            }
            syncForViewport();
            mobileQuery.addEventListener('change', syncForViewport);
            window.addEventListener('resize', debounce(syncForViewport, 150));
        }
        initializeNavigationTabs();
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
            if (tables.length === 0) return;
        
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
            window.addEventListener('resize', function() {
                if (window.innerWidth < 768) {
                    applyResponsiveTableCards();
                } else {
                    resetResponsiveTableCards();
                }
            });
        }

        // Initialize responsive table cards
        syncResponsiveTableCards();

        // Keep sticky UI elements clear of dynamic feedback panels
        function initFeedbackOffsets() {
            var page = document.getElementById('page');
            if (!page) return;
            var topFeedback = document.getElementById('topFeedback');
            var bottomFeedback = document.getElementById('bottomFeedback');

            function getFeedbackHeight(feedbackElement) {
                if (!feedbackElement) return 0;
                var panel = feedbackElement.querySelector('.feedbackPanel');
                return panel && panel.offsetHeight > 0 ? feedbackElement.offsetHeight : 0;
            }

            function updateOffsets() {
                var topHeight = getFeedbackHeight(topFeedback);
                var bottomHeight = getFeedbackHeight(bottomFeedback);
                page.style.setProperty('--gs-feedback-top-height', topHeight + 'px');
                page.style.setProperty('--gs-feedback-bottom-height', bottomHeight + 'px');
            }

            function refreshFeedbackRefs() {
                topFeedback = document.getElementById('topFeedback');
                bottomFeedback = document.getElementById('bottomFeedback');
            }

            updateOffsets();
            window.addEventListener('resize', updateOffsets);

            var pageHeader = document.querySelector('.page-header');
            var pageFooter = document.querySelector('.page-footer');
            if (window.MutationObserver && (pageHeader || pageFooter)) {
                var mutationObserver = new MutationObserver(function () {
                    refreshFeedbackRefs();
                    updateOffsets();
                });
                if (pageHeader) {
                    mutationObserver.observe(pageHeader, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        characterData: true
                    });
                }
                if (pageFooter) {
                    mutationObserver.observe(pageFooter, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        characterData: true
                    });
                }
            }

            if (window.ResizeObserver) {
                var resizeObserver = new ResizeObserver(function () {
                    refreshFeedbackRefs();
                    updateOffsets();
                });
                if (pageHeader) {
                    resizeObserver.observe(pageHeader);
                }
                if (pageFooter) {
                    resizeObserver.observe(pageFooter);
                }
            }
        };
        initFeedbackOffsets();
    });
})();
