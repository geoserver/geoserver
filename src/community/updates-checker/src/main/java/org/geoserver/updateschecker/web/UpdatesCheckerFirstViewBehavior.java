/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.UpdatesCheckerConfigPersister;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.updateschecker.UpdatesCheckerStatus;

/**
 * The one feed check of an admin session, run from an AJAX call as soon as the page has loaded so a slow or unreachable
 * feed host never delays page rendering. Without scripts a first view shows no banner; later views in the session are
 * drawn from the cache. The services are looked up inside {@code respond()} because they are not serializable and
 * Wicket keeps page state through Java serialization.
 */
abstract class UpdatesCheckerFirstViewBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    // renderHead() runs again for every component included in an AJAX response, including this one once the caller
    // adds it to the target; without this guard the DOM-ready trigger would be emitted into the very response
    // respond() is building, and re-fire this same callback forever
    private boolean triggered = false;

    /** Called with the status after the check, to redraw whatever shows it. */
    protected abstract void onStatus(AjaxRequestTarget target, UpdatesCheckerStatus status);

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        if (!triggered) {
            response.render(OnDomReadyHeaderItem.forScript(getCallbackScript().toString()));
        }
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        triggered = true;
        boolean enabled = GeoServerExtensions.bean(UpdatesCheckerConfigPersister.class)
                .getConfig()
                .isEnabled();
        if (enabled) {
            // counted as this session's one check as soon as the fetch is attempted, even if the admin navigates
            // away before it completes
            UpdatesCheckerSessionState.markChecked();
        }
        onStatus(
                target,
                GeoServerExtensions.bean(UpdatesCheckerService.class).check(UpdatesCheckerService.Trigger.FIRST_VIEW));
    }
}
