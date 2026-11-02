/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Dismissals and snoozes, kept in this browser only. Each notification carries the storage key of its dismissal in
 * data-key, and optionally a second key a dismissal also sets in data-also; the value is "dismissed" or
 * "until:<epoch ms>". A snooze action says how long it hides the notification in data-snooze (milliseconds). If storage
 * is blocked, nothing is hidden and a click hides the notification for this view only.
 */
(function (global) {
    'use strict';


    function storage(item) {
        try {
            return item.hasAttribute('data-session-only') ? global.sessionStorage : global.localStorage;
        } catch (e) {
            return null;
        }
    }

    function read(item, key) {
        try {
            return key ? storage(item).getItem(key) : null;
        } catch (e) {
            return null;
        }
    }

    function write(item, key, value) {
        try {
            if (key && value === null) {
                storage(item).removeItem(key);
            } else if (key) {
                storage(item).setItem(key, value);
            }
        } catch (e) {
            // not kept: hidden for this view only
        }
    }

    // the longest snooze the notification offers; a snooze that outlasts it was written before the clock was corrected
    function longestSnooze(item) {
        var longest = 0;
        item.querySelectorAll('a[data-snooze]').forEach(function (link) {
            longest = Math.max(longest, Number(link.getAttribute('data-snooze')));
        });
        return longest;
    }

    function isDismissed(item) {
        var value = read(item, item.getAttribute('data-key')) || '';
        var until = /^until:(\d+)$/.exec(value);
        var left = until === null ? 0 : Number(until[1]) - Date.now();
        return value === 'dismissed' || (left > 0 && left <= longestSnooze(item));
    }

    // the banner shows its first active notification and counts the others; the Status section shows them all
    function refresh(root) {
        var banner = root.hasAttribute('data-banner');
        var active = 0;
        var dismissed = 0;
        root.querySelectorAll('.gs-updates-checker-item').forEach(function (item) {
            if (item.gsHidden || isDismissed(item)) {
                item.hidden = true;
                dismissed++;
            } else {
                item.hidden = banner && active > 0;
                active++;
            }
        });
        var more = root.querySelector('.gs-updates-checker-more');
        if (more) {
            more.hidden = active < 2;
            more.querySelector('.gs-updates-checker-more-count').textContent = String(active - 1);
        }
        var none = root.querySelector('.gs-updates-checker-none');
        if (none) {
            none.hidden = active > 0;
        }
        var clear = root.querySelector('.gs-updates-checker-clear');
        if (clear) {
            clear.hidden = dismissed === 0;
            clear.querySelector('a').textContent = clear.getAttribute('data-template').replace('{0}', dismissed);
        }
    }

    function act(item, link) {
        var snooze = Number(link.getAttribute('data-snooze'));
        write(item, item.getAttribute('data-key'), snooze ? 'until:' + (Date.now() + snooze) : 'dismissed');
        if (!snooze) {
            write(item, item.getAttribute('data-also'), 'dismissed');
        }
        item.gsHidden = true;
    }

    function clearAll(root) {
        root.querySelectorAll('.gs-updates-checker-item').forEach(function (item) {
            if (isDismissed(item) || item.gsHidden) {
                write(item, item.getAttribute('data-key'), null);
                write(item, item.getAttribute('data-also'), null);
                item.gsHidden = false;
            }
        });
    }

    function init(root) {
        if (!root || root.gsInit) {
            return;
        }
        root.gsInit = true;
        refresh(root);
        root.addEventListener('click', function (event) {
            var link = event.target.closest('a[data-action], a[data-clear]');
            if (!link || !root.contains(link)) {
                return;
            }
            event.preventDefault();
            if (link.hasAttribute('data-clear')) {
                clearAll(root);
            } else {
                act(link.closest('.gs-updates-checker-item'), link);
            }
            refresh(root);
        });
    }

    // Check Now: the class swaps the label and greys the button. Wicket ignores a click while a check runs (the ACTIVE
    // channel), and the answer replaces the button, so only a failed request needs the class removed
    function checkNow(id, busy) {
        var button = global.document.getElementById(id);
        if (button) {
            button.classList.toggle('gs-updates-checker-busy', busy);
        }
    }

    global.GsUpdatesChecker = {init: init, checkNow: checkNow};
})(window);
