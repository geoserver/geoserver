/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.turbojpeg;

import java.util.Collection;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * Specific {@link RenderedImageMapResponse} for JPEG using LibJPEGTurbo.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class TurboJPEGMapResponseTest extends WMSTestSupport {

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new RenderedImageMapOutputFormat(
                "image/jpeg", new String[] {"image/jpeg"}, getWMS());
    }

    /** Make sure we find the {@link TurboJPEGMapResponse}. */
    @Test
    public void testFindTurboJPEGMapResponse() {

        // look for the turbojpegmapresponses
        final Collection<RenderedImageMapResponse> responses = getWMS().getAvailableMapResponses();
        for (RenderedImageMapResponse response : responses) {
            if (response instanceof TurboJPEGMapResponse) {
                return;
            }
        }

        // we did not find it
        Assert.assertFalse("Unable to find TurboJPEGMapResponse", false);
    }
}
