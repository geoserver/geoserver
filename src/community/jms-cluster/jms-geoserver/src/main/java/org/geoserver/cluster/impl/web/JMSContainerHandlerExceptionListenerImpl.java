/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.web;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.geoserver.cluster.client.JMSContainerHandlerExceptionListener;

/** @author carlo cancellieri - geosolutions sas */
public class JMSContainerHandlerExceptionListenerImpl
        implements JMSContainerHandlerExceptionListener {

    private FeedbackPanel fp;

    private Session session;

    public JMSContainerHandlerExceptionListenerImpl() {}

    public void setFeedbackPanel(FeedbackPanel fp) {
        this.fp = fp;
    }

    public void setSession(Session s) {
        if (session != null) {
            synchronized (this.session) {
                this.session = s;
            }
        } else {
            synchronized (s) {
                this.session = s;
            }
        }
    }

    //
    @Override
    public void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
        if (session != null) {
            synchronized (session) {
                if (session.isSessionInvalidated()) {
                    return; // skip
                }
                // what was this doing...
                // Session.set(session);

                if (fp != null) {
                    if (alreadyRecovered) {
                        fp.warn(
                                "There was an error which seems already fixed: "
                                        + ex.getLocalizedMessage());
                    } else {
                        fp.error(ex.getLocalizedMessage());
                    }
                }
            }
        }
    }
}
