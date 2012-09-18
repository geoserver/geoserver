/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.csw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.csw.kvp.GetRepositoryItemKvpRequestReader;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;

/**
 * Unit test suite for {@link GetRepositoryItemKvpRequestReader}
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetRepositoryItemKvpRequestReaderTest extends TestCase {

    private GeoServerImpl geoServerImpl;

    private Service csw;

    private Map<String, String> params;

    protected void setUp() throws Exception {
        geoServerImpl = new GeoServerImpl();
        List<String> operations = new ArrayList<String>();
        csw = new Service("csw", new DefaultCatalogService(geoServerImpl), new Version("2.0.2"), operations); 

        params = new HashMap<String, String>();
    }

    protected void tearDown() throws Exception {
        csw = null;
        params = null;
    }

    private GetRepositoryItemBean getRequest(Map<String, String> rawKvp) throws Exception {
        return getRequest(rawKvp, new HashMap<String, Object>(rawKvp));
    }

    private GetRepositoryItemBean getRequest(Map<String, String> rawKvp, Map<String, Object> kvp)
            throws Exception {

        GetRepositoryItemKvpRequestReader reader = new GetRepositoryItemKvpRequestReader(csw);
        GetRepositoryItemBean req = (GetRepositoryItemBean) reader.createRequest();
        return (GetRepositoryItemBean) reader.read(req, kvp, rawKvp);
    }

    public void testGetRequestNoVersion() throws Exception {
        params.put("id", "foo");
        try {
            getRequest(params);
            fail("expected ServiceException if version is not provided");
        } catch (ServiceException e) {
            assertEquals("NoVersionInfo", e.getCode());
        }
    }

    public void testGetRequestInvalidVersion() throws Exception {
        params.put("VERSION", "fakeVersion");
        try {
            getRequest(params);
            fail("expected ServiceException if the wrong version is requested");
        } catch (ServiceException e) {
            assertEquals("InvalidVersion", e.getCode());
        }
    }

    public void testGetRequestNoIdRequested() throws Exception {
        params.put("VERSION", "2.0.2");
        try {
            getRequest(params);
            fail("expected ServiceException if no ID is requested");
        } catch (ServiceException e) {
            assertEquals("NoID", e.getCode());
        }
    }

//    public void testGetRequest() throws Exception {
//    }
}
