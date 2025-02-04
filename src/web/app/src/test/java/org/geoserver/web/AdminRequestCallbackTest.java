/* (c) 2024 - Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertNotNull;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/** Verify both {@link AdminRequestCallback} and {@link AdminRequestWicketCallback} beans exist (GEOS-11696). */
public class AdminRequestCallbackTest extends GeoServerSystemTestSupport {

    @Test
    public void checkWicketAdminRequestCallback() {
        assertNotNull(GeoServerExtensions.bean(AdminRequestWicketCallback.class));
    }

    @Test
    public void checkRestAdminRequestCallback() {
        assertNotNull(GeoServerExtensions.bean(AdminRequestCallback.class));
    }
}
