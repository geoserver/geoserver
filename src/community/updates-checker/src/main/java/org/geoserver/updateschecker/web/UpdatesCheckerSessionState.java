/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

/**
 * Remembers, per admin session, whether this session has already had its one feed check. The first view of an admin
 * session fetches; every later view in the same session is drawn from the process-wide cache with no network call.
 */
final class UpdatesCheckerSessionState {

    private static final MetaDataKey<Boolean> CHECKED = new MetaDataKey<>() {
        private static final long serialVersionUID = 1L;
    };

    private UpdatesCheckerSessionState() {}

    static boolean isChecked() {
        Boolean checked = Session.get().getMetaData(CHECKED);
        return checked != null && checked;
    }

    static void markChecked() {
        Session.get().setMetaData(CHECKED, Boolean.TRUE);
    }
}
