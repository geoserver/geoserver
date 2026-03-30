function initSearchInputPanel(containerId, inputId, callbackUrl, autocompleteEnabled) {
    var $ul = $('#' + containerId);
    var $input = $('#' + inputId);
    var $announcer = $('#search-announcer');
    var currentIndex = -1;
    var baseAutocompleteEnabled = autocompleteEnabled !== false;

    function isAutocompleteEnabled() {
        return baseAutocompleteEnabled || window.matchMedia('(max-width: 768px)').matches;
    }

    // Helper: Update accessibility and visibility state
    function toggleDropdown(show) {
        if (show) {
            $ul.show();
            $input.attr('aria-expanded', 'true');
            $announcer.text('Results available. Use up and down arrows to navigate.');
        } else {
            $ul.hide();
            $input.attr('aria-expanded', 'false');
            $input.removeAttr('aria-activedescendant');
            currentIndex = -1;
            $ul.find('li').removeClass('active-descendant');
        }
    }

    // --- Keyboard Navigation ---
    $input.on('keydown', function(e) {
        if (!isAutocompleteEnabled()) return;
        // Find all current items (re-queried in case Wicket appended more)
        var $items = $ul.find('li');
        var maxIndex = $items.length - 1;

        // If dropdown is closed and user presses an arrow key, open it
        if (!$ul.is(':visible') && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
            if ($items.length > 0) toggleDropdown(true);
            return;
        }

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                currentIndex = currentIndex < maxIndex ? currentIndex + 1 : 0;
                highlightItem($items);
                break;
            case 'ArrowUp':
                e.preventDefault();
                currentIndex = currentIndex > 0 ? currentIndex - 1 : maxIndex;
                highlightItem($items);
                break;
            case 'Enter':
                e.preventDefault(); // Prevent form submission
                if (currentIndex >= 0 && $ul.is(':visible')) {
                    $items.eq(currentIndex).click();
                }
                break;
            case 'Escape':
                toggleDropdown(false);
                $input.focus();
                break;
        }
    });

    // Helper: Manage ARIA activedescendant and visual highlighting
    function highlightItem($items) {
        $items.removeClass('active-descendant');
        
        if (currentIndex >= 0) {
            var $active = $items.eq(currentIndex);
            $active.addClass('active-descendant');
            
            var activeId = containerId + '-opt-' + currentIndex;
            $active.attr('id', activeId);
            $input.attr('aria-activedescendant', activeId);

            // Auto-scroll logic
            var itemTop = $active.position().top;
            var ulScrollTop = $ul.scrollTop();
            var ulHeight = $ul.height();
            var itemHeight = $active.outerHeight();

            if (itemTop < 0) {
                $ul.scrollTop(ulScrollTop + itemTop);
            } else if (itemTop + itemHeight > ulHeight) {
                $ul.scrollTop(ulScrollTop + itemTop + itemHeight - ulHeight);
            }
        }
    }

    // --- Infinite Scroll ---
    $ul.on('scroll', function() {
        if ($ul.data('hasMore') === false) return;
        
        // Trigger if scrolled within 20px of bottom
        if ($ul[0].scrollHeight - $ul.scrollTop() - $ul.outerHeight() < 20) {
            $ul.data('hasMore', false); // Throttle
            $announcer.text('Loading more results...');
            Wicket.Ajax.get({ u: callbackUrl }); // Call Wicket backend
        }
    });

    // Broadcast query only when autocomplete is OFF (tree filtering mode).
    $input.off('input.gsNavTreeFilter');
    $input.on('input.gsNavTreeFilter', function () {
        if (isAutocompleteEnabled()) return;
        $(document).trigger('gsNavTreeFilter', [$input.val() || '']);
    });

    // --- Mouse Clicks ---
    $ul.on('click', 'li', function() {
        var $item = $(this);
        var selectedText = $item.find('.item-label').text();
        var type = $item.data('type');
        var workspace = $item.data('workspace');
        var layer = $item.data('layer');

        $input.val(selectedText);
        toggleDropdown(false);
        $input.trigger('change'); // Sync the value back to Wicket's model

        // Update URL parameters based on selection
        try {
            var url = new URL(window.location.href);

            if (type === 'workspace') {
                if (workspace) {
                    url.searchParams.set('workspace', workspace);
                }
                url.searchParams.delete('layer');
            } else if (type === 'layer' || type === 'layerGroup') {
                if (workspace) {
                    url.searchParams.set('workspace', workspace);
                }
                if (layer) {
                    url.searchParams.set('layer', layer);
                }
            }

            window.location.href = url.toString();
        } catch (e) {
            // Fallback: do nothing if URL API is not available
        }
    });

    // Click outside to close (unbind first to avoid duplicate handlers on AJAX re-init)
    $(document).off('click.gsSearchInput').on('click.gsSearchInput', function(e) {
        if (!$(e.target).closest('.custom-search-wrapper').length) {
            toggleDropdown(false);
        }
    });
}