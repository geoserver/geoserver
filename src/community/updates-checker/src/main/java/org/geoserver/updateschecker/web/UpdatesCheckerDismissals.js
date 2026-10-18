/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Browser-side dismissal and snooze state for the updates-checker notifications. Nothing is sent to the server.
 *
 * Storage key:   updates-checker|<type>|<discriminator>   (each part passed through encodeURIComponent)
 * Storage value: "dismissed" | "until:<epoch-ms>"
 * Storage:       localStorage, except VERSION_UNKNOWN (sessionStorage: session only)
 *
 * Fail-safe: the server renders the first notification visible and the rest hidden; a notification is only hidden here
 * when a valid entry is found. If storage is unavailable or throws, nothing is hidden and a click only hides the
 * notification for the current page view.
 */
(function (global) {
    'use strict';

    var PREFIX = 'updates-checker';
    var DAY_MS = 24 * 60 * 60 * 1000;

    function key(type, discriminator) {
        return [PREFIX, type, discriminator].map(encodeURIComponent).join('|');
    }

    // longest snooze a notification type may carry; a longer "until:" (hand-edited, clock change) counts as expired
    function maxSnoozeMs(type) {
        return type === 'SERIES_MAINTENANCE' ? 30 * DAY_MS : DAY_MS;
    }

    /*
     * Whether a stored value currently hides a notification. A CRITICAL confirmed CVE can only be snoozed, never
     * permanently dismissed, so for it a "dismissed" entry is ignored (the key carries no severity, and an
     * advisory's severity can change in the feed).
     */
    function isHidden(value, type, severity, now) {
        if (value === 'dismissed') {
            return !(type === 'CVE_CONFIRMED' && severity === 'CRITICAL');
        }
        var match = /^until:(\d+)$/.exec(value || '');
        if (!match) {
            return false;
        }
        var until = Number(match[1]);
        return until > now && until - now <= maxSnoozeMs(type);
    }

    function storageFor(sessionOnly) {
        try {
            return sessionOnly ? global.sessionStorage : global.localStorage;
        } catch (e) {
            return null;
        }
    }

    function describe(item) {
        return {
            type: item.getAttribute('data-type'),
            severity: item.getAttribute('data-severity'),
            discriminator: item.getAttribute('data-discriminator'),
            target: item.getAttribute('data-target-version'),
            dismissible: item.getAttribute('data-dismissible') === 'true',
            sessionOnly: item.getAttribute('data-session-only') === 'true'
        };
    }

    function hiddenByStorage(item, now) {
        var d = describe(item);
        if (!d.dismissible) {
            return false;
        }
        try {
            var store = storageFor(d.sessionOnly);
            return !!store && isHidden(store.getItem(key(d.type, d.discriminator)), d.type, d.severity, now);
        } catch (e) {
            return false;
        }
    }

    // removes the entries that no longer apply: another discriminator, expired, or no longer valid
    function cleanup(items, now) {
        [false, true].forEach(function (sessionOnly) {
            try {
                var store = storageFor(sessionOnly);
                if (!store) {
                    return;
                }
                var keep = {};
                for (var i = 0; i < items.length; i++) {
                    var d = describe(items[i]);
                    if (d.dismissible && d.sessionOnly === sessionOnly
                            && isHidden(store.getItem(key(d.type, d.discriminator)), d.type, d.severity, now)) {
                        keep[key(d.type, d.discriminator)] = true;
                    }
                }
                var prefix = PREFIX + '|';
                for (var j = store.length - 1; j >= 0; j--) {
                    var k = store.key(j);
                    if (k && k.indexOf(prefix) === 0 && !keep[k]) {
                        store.removeItem(k);
                    }
                }
            } catch (e) {
                // storage blocked or broken: leave it alone
            }
        });
    }

    function hide(item) {
        item.hidden = true;
        item.style.display = 'none';
    }

    function reveal(item) {
        item.hidden = false;
        item.style.display = '';
    }

    function itemsOf(root) {
        return Array.prototype.slice.call(root.querySelectorAll('.gs-updates-checker-notification'));
    }

    /*
     * Shows the first notification that is not dismissed and hides the rest; the "N more notices" count is the number
     * of other notifications that are not dismissed.
     */
    function layout(root, items, hiddenFlags) {
        var shown = -1;
        var others = 0;
        for (var i = 0; i < items.length; i++) {
            if (hiddenFlags[i]) {
                hide(items[i]);
            } else if (shown < 0) {
                shown = i;
                reveal(items[i]);
            } else {
                hide(items[i]);
                others++;
            }
        }
        // the heading names the plugin, so it goes when nothing of the plugin's is showing
        var heading = root.querySelector('.gs-updates-checker-heading');
        if (heading) {
            heading.hidden = shown < 0;
        }
        var more = root.querySelector('.gs-updates-checker-more');
        if (more) {
            var count = more.querySelector('.gs-updates-checker-more-count');
            if (count) {
                count.textContent = String(others);
            }
            more.hidden = others === 0;
        }
    }

    /*
     * The element of the given kind that a click landed on inside root, or null. Used by the banner and by the Status
     * section, whose action links all work the same way.
     */
    function clickedOn(event, root, selector) {
        var target = event.target && event.target.closest ? event.target : null;
        var found = target ? target.closest(selector) : null;
        return found && root.contains(found) ? found : null;
    }

    // records the dismissal or snooze an action link stands for, and returns the notification it belongs to
    function applyAction(link, root, itemSelector) {
        var item = link.closest(itemSelector);
        if (item) {
            apply(link.getAttribute('data-action'), describe(item));
        }
        return item;
    }

    function init(root) {
        if (!root || root.gsUpdatesCheckerInit) {
            return;
        }
        root.gsUpdatesCheckerInit = true;
        var items = itemsOf(root);
        var now = Date.now();
        var dismissedFlags = items.map(function (item) {
            return hiddenByStorage(item, now);
        });
        layout(root, items, dismissedFlags);
        cleanup(items, now);

        root.addEventListener('click', function (event) {
            var link = clickedOn(event, root, 'a[data-action]');
            if (!link) {
                return;
            }
            event.preventDefault();
            var item = applyAction(link, root, '.gs-updates-checker-notification');
            var index = items.indexOf(item);
            if (index >= 0) {
                dismissedFlags[index] = true;
                layout(root, items, dismissedFlags);
            }
        });
    }

    function write(store, k, value) {
        try {
            if (store) {
                store.setItem(k, value);
            }
        } catch (e) {
            // not persisted: hidden for this page view only
        }
    }

    function apply(action, d) {
        var now = Date.now();
        var store = storageFor(d.sessionOnly);
        var value;
        if (action === 'remind-24h') {
            value = 'until:' + (now + DAY_MS);
        } else if (action === 'remind-30d') {
            value = 'until:' + (now + 30 * DAY_MS);
        } else {
            value = 'dismissed';
        }
        write(store, key(d.type, d.discriminator), value);
        if (d.type === 'SECURITY_RELEASE' && value === 'dismissed' && d.target) {
            // dismissing a security release also settles the plain patch notice for the version to upgrade to
            write(storageFor(false), key('PATCH_AVAILABLE', d.target), 'dismissed');
        }
    }

    // the Status section hides what this browser has dismissed or snoozed, and shows its "nothing active" line when
    // every notification is hidden
    function hideDismissed(root) {
        var now = Date.now();
        var items = root.querySelectorAll('.gs-updates-checker-status-item');
        var visible = 0;
        var dismissed = 0;
        for (var i = 0; i < items.length; i++) {
            if (hiddenByStorage(items[i], now)) {
                hide(items[i]);
                dismissed++;
            } else {
                reveal(items[i]);
                visible++;
            }
        }
        var none = root.querySelector('.gs-updates-checker-status-none');
        if (none) {
            none.hidden = visible > 0;
        }
        var clear = root.querySelector('.gs-updates-checker-clear');
        if (clear) {
            clear.hidden = dismissed === 0;
            var link = clear.querySelector('a');
            if (link) {
                link.textContent = (clear.getAttribute('data-template') || '{0}').replace('{0}', String(dismissed));
            }
        }
    }

    // forgets this browser's dismissals and snoozes for the notifications the Status section lists
    function clearDismissals(root) {
        var now = Date.now();
        var items = root.querySelectorAll('.gs-updates-checker-status-item');
        for (var i = 0; i < items.length; i++) {
            if (!hiddenByStorage(items[i], now)) {
                continue;
            }
            var d = describe(items[i]);
            try {
                var store = storageFor(d.sessionOnly);
                if (store) {
                    store.removeItem(key(d.type, d.discriminator));
                }
                if (d.type === 'SECURITY_RELEASE' && d.target) {
                    // dismissing a security release also settled the plain patch notice for the version to upgrade to
                    storageFor(false).removeItem(key('PATCH_AVAILABLE', d.target));
                }
            } catch (e) {
                // storage blocked or broken: nothing to clear
            }
        }
    }

    // the Status section's action links write the same entries as the banner's, and the notification disappears
    function initStatus(root) {
        if (!root) {
            return;
        }
        hideDismissed(root);
        root.addEventListener('click', function (event) {
            if (clickedOn(event, root, 'a[data-clear]')) {
                event.preventDefault();
                clearDismissals(root);
                hideDismissed(root);
                return;
            }
            var link = clickedOn(event, root, 'a[data-action]');
            if (link) {
                event.preventDefault();
                applyAction(link, root, '.gs-updates-checker-status-item');
                hideDismissed(root);
            }
        });
    }

    var CHECK_NOW_MIN_MS = 2000;
    var checkNowStarted = 0;
    var checkNowLabel = null;

    function checkNowButton(id) {
        return global.document.getElementById(id);
    }

    function setBusy(button) {
        button.setAttribute('aria-disabled', 'true');
        button.classList.add('gs-updates-checker-busy');
        if (checkNowLabel === null) {
            checkNowLabel = button.textContent;
        }
        button.textContent = button.getAttribute('data-checking') || button.textContent;
    }

    function setIdle(button) {
        button.removeAttribute('aria-disabled');
        button.classList.remove('gs-updates-checker-busy');
        if (checkNowLabel !== null) {
            button.textContent = checkNowLabel;
        }
        checkNowLabel = null;
        // Wicket gives focus back to a replaced link, and GeoServer 2.x draws a focused link in black
        button.blur();
    }

    // Check Now: disabled and labelled "Checking" from the click until at least CHECK_NOW_MIN_MS later. The button is
    // replaced when the answer redraws the Status section, so the end handler puts the busy state back on the new one.
    // returns false, which makes Wicket drop the click, while a check is already running
    function checkNowBegin(id) {
        var button = checkNowButton(id);
        if (!button || button.getAttribute('aria-disabled') === 'true') {
            return false;
        }
        checkNowStarted = Date.now();
        setBusy(button);
        return true;
    }

    function checkNowEnd(id) {
        var button = checkNowButton(id);
        if (!button) {
            return;
        }
        var remaining = CHECK_NOW_MIN_MS - (Date.now() - checkNowStarted);
        if (remaining <= 0) {
            setIdle(button);
            return;
        }
        setBusy(button);
        global.setTimeout(function () {
            var current = checkNowButton(id);
            if (current) {
                setIdle(current);
            }
        }, remaining);
    }

    global.GsUpdatesChecker = {
        checkNowBegin: checkNowBegin,
        checkNowEnd: checkNowEnd,
        init: init,
        initStatus: initStatus,
        isHidden: isHidden,
        key: key
    };
})(typeof window !== 'undefined' ? window : globalThis);
