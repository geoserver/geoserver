/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.ConfigPersister;
import org.geoserver.updateschecker.Status;
import org.geoserver.updateschecker.UpdatesCheckerService;

/**
 * The one feed check of an admin session, run from an AJAX call once the page has loaded, so a slow feed host never
 * delays the page. Later views in the session are drawn from the cache.
 */
abstract class FirstViewBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    private static final MetaDataKey<Boolean> CHECKED = new MetaDataKey<>() {
        private static final long serialVersionUID = 1L;
    };

    // stops the AJAX response from rendering the trigger again, which would loop
    private boolean triggered = false;

    /** @return whether this session has had its check */
    static boolean isChecked() {
        return Boolean.TRUE.equals(Session.get().getMetaData(CHECKED));
    }

    static void markChecked() {
        Session.get().setMetaData(CHECKED, Boolean.TRUE);
    }

    /** Redraws whatever shows the status. */
    protected abstract void onStatus(AjaxRequestTarget target, Status status);

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
        // the services are looked up here because Wicket serializes the page and they are not serializable
        if (GeoServerExtensions.bean(ConfigPersister.class).getConfig().isEnabled()) {
            // marked before the fetch, so leaving the page during it does not cause a second one
            markChecked();
        }
        onStatus(
                target,
                GeoServerExtensions.bean(UpdatesCheckerService.class).check(UpdatesCheckerService.Trigger.FIRST_VIEW));
    }
}
