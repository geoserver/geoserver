
;(function() {

    "use strict";

    // The dropTarget function is copied from wicket-dnd.js with a small patch to resolve unsafe-eval CSP violations.

    window.wicketdnd.dropTarget = function(id, attrs, operations, types, selectors) {
        var element = Wicket.$(id);

        $(element).data(wicketdnd.DROP_TARGET_PREFIX + id, {
            'operations' : operations,
            'types' : types,
            'selectors' : selectors,
            'findLocation' : function(event) {
                var candidate = event.target;
                var position = wicketdnd.position(event);
                var location = wicketdnd.locationNone;

                do {
                    location = findLocation(position, candidate, location);

                    if (location != wicketdnd.locationNone && location.anchor != 'CENTER') {
                        break;
                    }

                    if (candidate == element) {
                        break;
                    }
                    candidate = candidate.parentNode;
                } while (candidate);


                if (location != wicketdnd.locationNone && !location.id) {
                    Wicket.Log.error('wicket-dnd: drop ' + location.anchor + ' matched selector but does not have markup id');
                    location = wicketdnd.locationNone;
                }

                return location;
            },
            'notify' : function(phase, operation, id, behavior, path, location, success) {
                attrs.ep = attrs.ep || {};
                attrs.ep['phase'] = phase;
                attrs.ep['operation'] = operation.name;
                attrs.ep['drag'] = id;
                attrs.ep['behavior'] = behavior;
                attrs.ep['path'] = path;
                attrs.ep['component'] = location.id;
                attrs.ep['anchor'] = location.anchor;
                // Start GEOS-11817 Changes
                // Original Code:
                // attrs['sh'] = [success];
                if (typeof success === "function") {
                    attrs['sh'] = [success];
                }
                // End GEOS-11817 Changes
                Wicket.Ajax.ajax(attrs);
            }
        });

        function findLocation(position, candidate, location) {

            if (location == wicketdnd.locationNone && $(candidate).is(selectors.center)) {
                location = {
                    'id' : candidate.id,
                    'operations' : operations,
                    'anchor' : 'CENTER',
                    'mark' : function() {
                        $('#' + candidate.id).addClass('dnd-drop-center');
                    },
                    'unmark' : function() {
                        $('#' + candidate.id).removeClass('dnd-drop-center');
                    }
                };
            }

            var topMargin = wicketdnd.MARGIN;
            var bottomMargin = wicketdnd.MARGIN;
            var leftMargin = wicketdnd.MARGIN;
            var rightMargin = wicketdnd.MARGIN;

            var base = $(element).offset();
            var offset = $(candidate).offset();
            var width = $(candidate).outerWidth();
            var height = $(candidate).outerHeight();

            if (location == wicketdnd.locationNone) {
                // no location yet thus using full bounds
                topMargin = height / 2;
                bottomMargin = height / 2;
                leftMargin = width / 2;
                rightMargin = width / 2;
            }

            if ($(candidate).is(selectors.top) && (position.top <= offset.top + topMargin)) {
                var _div = $('<div>').addClass('dnd-drop-top');
                location = {
                    'id' : candidate.id,
                    'operations' : operations,
                    'anchor' : 'TOP',
                    'mark' : function() {
                        $(element).append(_div);
                        _div.css({ 'left' : (offset.left - base.left) + 'px', 'top' : (offset.top - base.top - _div.outerHeight()/2) + 'px', 'width' : width + 'px'});
                    },
                    'unmark' : function() {
                        _div.remove();
                    }
                };
            } else if ($(candidate).is(selectors.bottom) && (position.top >= offset.top + height - bottomMargin)) {
                var _div = $('<div>').addClass('dnd-drop-bottom');
                location = {
                    'id' : candidate.id,
                    'operations' : operations,
                    'anchor' : 'BOTTOM',
                    'mark' : function() {
                        $(element).append(_div);
                        _div.css({ 'left' : (offset.left - base.left)  + 'px', 'top' : (offset.top - base.top + height - _div.outerHeight()/2) + 'px', 'width' : width + 'px'});
                    },
                    'unmark' : function() {
                        _div.remove();
                    }
                };
            } else if ($(candidate).is(selectors.left) && (position.left <= offset.left + leftMargin)) {
                var _div = $('<div>').addClass('dnd-drop-left');
                location = {
                    'id' : candidate.id,
                    'operations' : operations,
                    'anchor' : 'LEFT',
                    'mark' : function() {
                        $(element).append(_div);
                        _div.css({ 'left' : (offset.left - base.left - _div.outerWidth()/2) + 'px', 'top' : (offset.top - base.top) + 'px', 'height' : height + 'px'});
                    },
                    'unmark' : function() {
                        _div.remove();
                    }
                };
            } else if ($(candidate).is(selectors.right) && (position.left >= offset.left + width - rightMargin)) {
                var _div = $('<div>').addClass('dnd-drop-right');
                location = {
                    'id' : candidate.id,
                    'operations' : operations,
                    'anchor' : 'RIGHT',
                    'mark' : function() {
                        $(element).append(_div);
                        _div.css({ 'left' : (offset.left - base.left + width - _div.outerWidth()/2) + 'px', 'top' : (offset.top - base.top)  + 'px', 'height' : height + 'px'});
                    },
                    'unmark' : function() {
                        _div.remove();
                    }
                };
            }

            return location;
        };
    }
})();
