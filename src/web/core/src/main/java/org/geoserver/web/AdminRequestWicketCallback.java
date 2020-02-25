/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.geoserver.security.AdminRequest;

/**
 * Wicket callback that sets the {@link AdminRequest} thread local.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AdminRequestWicketCallback implements WicketCallback {

    @Override
    public void onBeginRequest() {
        AdminRequest.start(this);
    }

    @Override
    public void onEndRequest() {
        AdminRequest.finish();
    }

    @Override
    public void onAfterTargetsDetached() {}

    @Override
    public void onRequestTargetSet(
            RequestCycle cycle, Class<? extends IRequestablePage> requestTarget) {
        // for non secured page requests we abort the admin request since they are meant to be
        // accessible anonymously, so we don't consider this an admin request
        if (requestTarget == null
                || !(GeoServerSecuredPage.class.isAssignableFrom(requestTarget)
                        || GeoServerHomePage.class.isAssignableFrom(requestTarget))) {
            AdminRequest.abort();
        }
    }

    @Override
    public void onRuntimeException(RequestCycle cycle, Exception ex) {
        // nothing to do
    }
}
