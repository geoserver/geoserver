/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.DeleteType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.wfs.request.Delete;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Testcases for {@link WFSException}.
 *
 * @author awaterme
 */
public class WFSExceptionTest {

    // WFS 1
    private DeleteElementType deleteElementType1;
    private GetFeatureType getFeatureType1;

    // WFS 2
    private DeleteType deleteType2;
    private net.opengis.wfs20.GetFeatureType getFeatureType2;

    @Before
    public void setupMocks() {
        deleteElementType1 = WfsFactory.eINSTANCE.createDeleteElementType();
        getFeatureType1 = WfsFactory.eINSTANCE.createGetFeatureType();

        deleteType2 = Wfs20Factory.eINSTANCE.createDeleteType();
        getFeatureType2 = Wfs20Factory.eINSTANCE.createGetFeatureType();
    }

    /**
     * Test {@link WFSException#init(Object)} for Exception with a WFS11 {@link Delete}-Action.
     *
     * @see "https://osgeo-org.atlassian.net/browse/GEOS-5857"
     */
    @Test
    public void testWFS11Delete() {
        WFSException tmpEx = new WFSException(new Delete.WFS11(deleteElementType1), "test");
        // WFS 1.x: no locator
        Assert.assertNull(tmpEx.getLocator());
    }

    /**
     * Test {@link WFSException#init(Object)} for Exception with a WFS20 {@link Delete}-Action.
     *
     * @see "https://osgeo-org.atlassian.net/browse/GEOS-5857"
     */
    @Test
    public void testWFS20Delete() {
        WFSException tmpEx = new WFSException(new Delete.WFS20(deleteType2), "test");
        Assert.assertEquals("Delete", tmpEx.getLocator());
    }

    /**
     * Test {@link WFSException#init(Object)} for Exception with a WFS11 {@link
     * GetFeatureRequest}-Action.
     *
     * @see "https://osgeo-org.atlassian.net/browse/GEOS-5857"
     */
    @Test
    public void testWFS11GetFeatureType() {
        WFSException tmpEx = new WFSException(new GetFeatureRequest.WFS11(getFeatureType1), "test");
        // WFS 1.x: no locator, GetFeature type is a top-level request and provides a default
        // version (1.1.0)
        Assert.assertNull(tmpEx.getLocator());
    }

    /**
     * Test {@link WFSException#init(Object)} for Exception with a WFS20 {@link
     * GetFeatureRequest}-Action.
     *
     * @see "https://osgeo-org.atlassian.net/browse/GEOS-5857"
     */
    @Test
    public void testWFS20GetFeatureType() {
        WFSException tmpEx = new WFSException(new GetFeatureRequest.WFS20(getFeatureType2), "test");
        Assert.assertEquals("GetFeature", tmpEx.getLocator());
    }
}
