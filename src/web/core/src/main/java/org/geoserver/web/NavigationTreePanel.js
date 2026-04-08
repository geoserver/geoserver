function initNavigationTreePanel(workspacesScrollId, callbackUrl) {
    var $sidebar = $('aside.gs-sidebar');
    var $headerSearchContainer = $('.gs-header-search-container');
    var $newToggle = $('#gs-sidebar-new-toggle');
    var $newMenu = $('#gs-sidebar-new-menu');
    var mobileQuery = window.matchMedia('(max-width: 768px)');

    function placeSearchByViewport() {
        if (!$headerSearchContainer.length) {
            return;
        }

        var isMobile = mobileQuery.matches;
        var $sidebarFilter = $('.gs-sidebar-filter').first();
        var $sidebarSearch = $('.gs-sidebar-search').first();
        if (!$sidebarSearch.length) {
            $sidebarSearch = $headerSearchContainer.find('.gs-sidebar-search').first();
        }
        if (!$sidebarSearch.length) {
            return;
        }

        if (isMobile) {
            if (!$headerSearchContainer.find('.gs-sidebar-search').length) {
                $headerSearchContainer.empty().append($sidebarSearch);
            }
        } else if ($sidebarFilter.length && !$sidebarFilter.find('.gs-sidebar-search').length) {
            $sidebarFilter.prepend($sidebarSearch);
            $headerSearchContainer.empty();
        }
    }

    function closeNewMenu() {
        $newMenu.attr('hidden', 'hidden');
        $newToggle.attr('aria-expanded', 'false');
    }

    function openNewMenu() {
        $newMenu.removeAttr('hidden');
        $newToggle.attr('aria-expanded', 'true');
    }

    if ($newToggle.length && $newMenu.length) {
        $newToggle.off('click.gsNavNewMenu');
        $newToggle.on('click.gsNavNewMenu', function (e) {
            e.preventDefault();
            if ($newMenu.is('[hidden]')) {
                openNewMenu();
            } else {
                closeNewMenu();
            }
        });

        $newMenu.off('click.gsNavNewMenu').on('click.gsNavNewMenu', 'a', function () {
            closeNewMenu();
        });

        $(document).off('click.gsNavNewMenu').on('click.gsNavNewMenu', function (e) {
            if (!$(e.target).closest('.gs-sidebar-new').length) {
                closeNewMenu();
            }
        });
    }

    placeSearchByViewport();
    $(window).off('resize.gsNavTreeSearchPlacement');
    $(window).on('resize.gsNavTreeSearchPlacement', function () {
        placeSearchByViewport();
    });

    var filterTimer = null;

    // --- Page-based pagination UI ---
    function renderPagination($container) {
        var totalItems = parseInt($container.attr('data-total-items') || '0', 10);
        var totalPages = parseInt($container.attr('data-total-pages') || '1', 10);
        var currentPage = parseInt($container.attr('data-current-page') || '1', 10);
        var pageSize = parseInt($container.attr('data-page-size') || '20', 10);

        if (!totalItems || totalItems <= 0) {
            $container.empty();
            return;
        }

        if (!Number.isFinite(totalPages) || totalPages < 1) totalPages = 1;
        if (!Number.isFinite(currentPage) || currentPage < 1) currentPage = 1;
        if (!Number.isFinite(pageSize) || pageSize < 1) pageSize = 20;

        var prevPage = currentPage - 1;
        var nextPage = currentPage + 1;

        var prevDisabled = totalPages <= 1 || currentPage <= 1;
        var nextDisabled = totalPages <= 1 || currentPage >= totalPages;
        var firstDisabled = prevDisabled;
        var lastDisabled = nextDisabled;
        var firstPage = 1;
        var lastPage = totalPages;

        var html =
            '<div class="gs-pagination-row">' +
            '<button type="button" class="gs-page-btn" data-page="' +
            firstPage +
            '"' +
            (firstDisabled ? ' disabled="disabled"' : '') +
            '>&lsaquo;&lsaquo;</button>' +
            '<button type="button" class="gs-page-btn" data-page="' +
            prevPage +
            '"' +
            (prevDisabled ? ' disabled="disabled"' : '') +
            '>&lsaquo;</button>' +
            '<span class="gs-page-label">' +
            currentPage +
            '/' +
            totalPages +
            '</span>' +
            '<button type="button" class="gs-page-btn" data-page="' +
            nextPage +
            '"' +
            (nextDisabled ? ' disabled="disabled"' : '') +
            '>&rsaquo;</button>' +
            '<button type="button" class="gs-page-btn" data-page="' +
            lastPage +
            '"' +
            (lastDisabled ? ' disabled="disabled"' : '') +
            '>&rsaquo;&rsaquo;</button>' +
            '</div>';

        $container.empty().append(html);
    }

    function renderAllPaginations() {
        $('.gs-pagination[data-section]').each(function () {
            renderPagination($(this));
        });
    }

    $(document)
        .off('click.gsNavTreePagination')
        .on('click.gsNavTreePagination', '.gs-pagination .gs-page-btn', function (e) {
            e.preventDefault();
            var $btn = $(this);
            if ($btn.is('[disabled]')) return;

            var $container = $btn.closest('.gs-pagination');
            var section = $container.attr('data-section');
            if (!section) return;

            var page = parseInt($btn.attr('data-page'), 10);
            if (!Number.isFinite(page) || page < 1) return;

            var pageSize = parseInt($container.attr('data-page-size') || '20', 10);
            if (!Number.isFinite(pageSize) || pageSize < 1) pageSize = 20;

            var wsName = $container.attr('data-workspace');
            var url =
                callbackUrl +
                '&kind=' +
                encodeURIComponent(section) +
                '&page=' +
                encodeURIComponent(page) +
                '&pageSize=' +
                encodeURIComponent(pageSize);
            if (section === 'layers' && wsName) {
                url += '&workspace=' + encodeURIComponent(wsName);
            }

            Wicket.Ajax.get({ u: url });
        });

    renderAllPaginations();

    // Listen to search input changes and refresh tree on server.
    $(document).off('gsNavTreeFilter.navTree');
    $(document).on('gsNavTreeFilter.navTree', function (e, query) {
        if (filterTimer) {
            window.clearTimeout(filterTimer);
        }
        filterTimer = window.setTimeout(function () {
            var url = callbackUrl + '&kind=filter&q=' + encodeURIComponent(query || '');
            Wicket.Ajax.get({ u: url });
        }, 150);
    });

    // Keyboard accessibility for expandable/collapsible section toggles.
    $(document)
        .off('keydown.gsNavTreeToggle')
        .on('keydown.gsNavTreeToggle', '.gs-toggle-button[role="button"]', function (e) {
            if (e.key === 'Enter' || e.key === ' ' || e.key === 'Space' || e.key === 'Spacebar') {
                e.preventDefault();
                this.click();
            }
        });
}