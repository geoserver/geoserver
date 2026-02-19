(function() {
    document.addEventListener("DOMContentLoaded", function() {

        // adjust top position of sticky header to keep them aligned correctly
        function updateHeaderStickyPosition() {
            const pageHeader = document.querySelector('.page-header');
            const tabRow = document.querySelector('.tab-row');
            const tableHeader = document.querySelector('.table-header');
            const pageHeaderBoundingClientRect = pageHeader ? pageHeader.getBoundingClientRect() : { height: 0 };

            const tabRowTop = pageHeaderBoundingClientRect.height;
            if (tabRow) {
                tabRow.style.top = `${tabRowTop}px`;
            }
            const tabRowBoundingClientRect = tabRow ? tabRow.getBoundingClientRect() : { height: 0 };
            const tableHeaderTop = tabRowBoundingClientRect.height + pageHeaderBoundingClientRect.height;
            if (tableHeader) {
                tableHeader.style.top = `${tableHeaderTop}px`;
            }
        }
        updateHeaderStickyPosition();
        window.addEventListener('resize', updateHeaderStickyPosition);
        // we need to control changes inside the page
        // when tab are controlled via js (e.g.: style editor page)
        const page = document.querySelector('#page');
        if (page) {
            let timeout = undefined;
            const observer = new MutationObserver(() => {
                if (timeout) {
                    clearTimeout(timeout);
                    timeout = undefined;
                }
                timeout = setTimeout(() => {
                    updateHeaderStickyPosition();
                }, 300)
            });
            observer.observe(page, {
                subtree: true,
                childList: true,
            });
        }

        // enabled sidebar menu button on small screen
        function initializeNavigationMenu() {
            const navigationMenu = document.querySelector('#sidebar-menu');
            const sidebarContent = document.querySelector('#sidebar-content');
            const sidebarClose = document.querySelector('#sidebar-close');
            if (navigationMenu && sidebarContent && sidebarClose) {
                navigationMenu.addEventListener('click', function() {
                    sidebarContent.style.display = 'flex';
                })
                sidebarClose.addEventListener('click', function() {
                    sidebarContent.style.display = 'none';
                });
            }
        }
        initializeNavigationMenu();
        function resetNavigationMenu() {
            const sidebarContent = document.querySelector('#sidebar-content');
            if (sidebarContent) {
                sidebarContent.style.removeProperty('display');
            }
        }
        window.addEventListener('resize', resetNavigationMenu);
    });
})();

