/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.csw.kvp.GetRepositoryItemKvpRequestReader;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test suite for {@link GetRepositoryItemKvpRequestReader}
 *
 * @version $Id$
 */
public class GetRepositoryItemKvpRequestReaderTest {

    private GeoServerImpl geoServerImpl;

    private Service csw;

    private Map<String, Object> params;

    @Before
    public void setUp() throws Exception {
        geoServerImpl = new GeoServerImpl();
        List<String> operations = new ArrayList<>();
        csw =
                new Service(
                        "csw",
                        new DefaultWebCatalogService(geoServerImpl),
                        new Version("2.0.2"),
                        operations);

        params = new HashMap<>();
    }

    @After
    public void tearDown() throws Exception {
        csw = null;
        params = null;
    }

    private GetRepositoryItemType getRequest(Map<String, Object> rawKvp) throws Exception {
        return getRequest(rawKvp, new HashMap<>(rawKvp));
    }

    private GetRepositoryItemType getRequest(Map<String, Object> rawKvp, Map<String, Object> kvp)
            throws Exception {

        GetRepositoryItemKvpRequestReader reader = new GetRepositoryItemKvpRequestReader(csw);
        GetRepositoryItemType req = (GetRepositoryItemType) reader.createRequest();
        return (GetRepositoryItemType) reader.read(req, kvp, rawKvp);
    }

    @Test
    public void testGetRequestNoIdRequested() throws Exception {
        params.put("VERSION", "2.0.2");
        try {
            getRequest(params);
            Assert.fail("expected ServiceException if no ID is requested");
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.MISSING_PARAMETER_VALUE, e.getCode());
            Assert.assertEquals("id", e.getLocator());
        }
    }

    @Test
    public void testParseValidRequest() throws Exception {
        params.put("service", "csw");
        params.put("VERSION", "2.0.2");
        params.put("id", "foo");
        GetRepositoryItemType request = getRequest(params);
        Assert.assertEquals("2.0.2", request.getVersion());
        Assert.assertEquals("csw", request.getService());
        Assert.assertEquals("foo", request.getId());
    }
}
