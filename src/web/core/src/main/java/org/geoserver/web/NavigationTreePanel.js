function initNavigationTreePanel(workspacesScrollId, callbackUrl) {
    var $sidebar = $('aside.gs-sidebar');
    var $newToggle = $('#gs-sidebar-new-toggle');
    var $newMenu = $('#gs-sidebar-new-menu');

    // Helper: check if there are more items to load
    function hasMore($el) {
        if (!$el || !$el.length) {
            return false;
        }
        var value = $el.data('hasMore');
        if (value === undefined) {
            value = $el.attr('data-has-more') === 'true';
            $el.data('hasMore', value);
        }
        return value !== false;
    }

    // Helper: element bottom reaches gs-sidebar bottom
    function nearSidebarBottom($el, thresholdPx) {
        if (!$el || !$el.length || !$sidebar.length) {
            return false;
        }
        var threshold = thresholdPx || 20;
        var sidebarBottom = $sidebar.scrollTop() + $sidebar.innerHeight();
        var elTopInSidebar = $el.offset().top - $sidebar.offset().top + $sidebar.scrollTop();
        var elBottom = elTopInSidebar + $el.outerHeight();
        var distance = elBottom - sidebarBottom;
        return distance <= threshold;
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

    function requestWorkspacesPage() {
        var $wsScroll = $('#' + workspacesScrollId);
        if (!$wsScroll.length) return false;
        if (!hasMore($wsScroll)) return false;
        if (!nearSidebarBottom($wsScroll, 20)) return false;

        $wsScroll.data('hasMore', false); // throttle until response updates attr
        var ws = $wsScroll.attr('data-selected-workspace') || '';
        var layer = $wsScroll.attr('data-selected-layer') || '';
        var url = callbackUrl + '&kind=workspaces';
        if (ws) url += '&workspace=' + encodeURIComponent(ws);
        if (layer) url += '&layer=' + encodeURIComponent(layer);
        Wicket.Ajax.get({ u: url });
        return true;
    }

    function requestLayersPage() {
        $('[data-kind="layers"][data-workspace]').each(function () {
            var $el = $(this);
            var workspaceName = $el.attr('data-workspace');
            if (!hasMore($el)) return;
            if (!nearSidebarBottom($el, 20)) return;

            $el.data('hasMore', false); // throttle until response updates attr
            var url = callbackUrl + '&kind=layers&workspace=' + encodeURIComponent(workspaceName);
            Wicket.Ajax.get({ u: url });
            return false; // break .each()
        });
    }

    var filterTimer = null;

    // --- Sidebar-based Infinite Scroll (workspace + per-workspace layers) ---
    if ($sidebar.length) {
        $sidebar.off('scroll.gsNavTree');
        $sidebar.on('scroll.gsNavTree', function () {
            // SearchInputPanel-style trigger: check hasMore + bottom, then fetch
            if (requestWorkspacesPage()) return;
            requestLayersPage();
        });

        // Initial auto-pagination when list is short/no initial scroll yet.
        $sidebar.triggerHandler('scroll.gsNavTree');
    }

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
}