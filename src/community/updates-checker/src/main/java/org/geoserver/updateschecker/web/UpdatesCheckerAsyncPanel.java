/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.updateschecker.UpdatesCheckerStatus;

/**
 * Delivers the banner for the current admin session: the first view gets an invisible placeholder that
 * {@link UpdatesCheckerFirstViewBehavior} fills in, later views draw the banner straight from the cache.
 */
public class UpdatesCheckerAsyncPanel extends Panel {

    private static final String INNER_ID = "inner";

    public UpdatesCheckerAsyncPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        if (UpdatesCheckerSessionState.isChecked()) {
            UpdatesCheckerStatus status =
                    GeoServerExtensions.bean(UpdatesCheckerService.class).getStatus();
            add(UpdatesCheckerBannerFactory.build(INNER_ID, status));
            return;
        }

        add(UpdatesCheckerBannerFactory.invisible(INNER_ID));

        add(new UpdatesCheckerFirstViewBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onStatus(AjaxRequestTarget target, UpdatesCheckerStatus status) {
                showStatus(target, status);
            }
        });
    }

    /** Replaces the banner from an already-known status, without fetching. */
    void showStatus(AjaxRequestTarget target, UpdatesCheckerStatus status) {
        addOrReplace(UpdatesCheckerBannerFactory.build(INNER_ID, status));
        target.add(this);
    }
}
