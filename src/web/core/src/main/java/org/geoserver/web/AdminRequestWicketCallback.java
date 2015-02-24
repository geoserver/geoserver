/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.target.component.IBookmarkablePageRequestTarget;
import org.apache.wicket.request.target.component.PageRequestTarget;
import org.geoserver.security.AdminRequest;

/**
 * Wicket callback that sets the {@link AdminRequest} thread local.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
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
    public void onAfterTargetsDetached() {
    }

    @Override
    public void onRequestTargetSet(IRequestTarget requestTarget) {
        //for non secured page requests we abort the admin request since they are meant to be 
        // accessible anonymously, so we don't consider this an admin request
        Class pageClass = null;
        if (requestTarget instanceof PageRequestTarget) {
            pageClass = ((PageRequestTarget) requestTarget).getPage().getPageClass();
        }
        if (requestTarget instanceof IBookmarkablePageRequestTarget) {
            pageClass = ((IBookmarkablePageRequestTarget) requestTarget).getPageClass();
        }
        if (requestTarget instanceof AjaxRequestTarget) {
            Page p = ((AjaxRequestTarget)requestTarget).getPage();
            pageClass = p != null ? p.getClass() : null;
        }
        if (pageClass == null || !(GeoServerSecuredPage.class.isAssignableFrom(pageClass) || 
            GeoServerHomePage.class.isAssignableFrom(pageClass))) {
            AdminRequest.abort();
        }
    }

    @Override
    public void onRuntimeException(Page page, RuntimeException e) {
    }

}
