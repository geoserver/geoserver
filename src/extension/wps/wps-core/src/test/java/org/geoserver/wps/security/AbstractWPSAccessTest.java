/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.security;

import java.util.Collections;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractWPSAccessTest extends WPSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // Add test/test user with roles ROLE_TEST
        addUser("test", "test", null, Collections.singletonList("ROLE_TEST"));
    }

    @Before
    public void setUpInternal() throws Exception {

        GeoServer gs = getGeoServer();
        WPSInfo wps = gs.getService(WPSInfo.class);

        // restrict buffer process to TEST role
        ProcessInfo pai = new ProcessInfoImpl();
        NameImpl bufferName = new NameImpl("JTS", "buffer");
        pai.setName(bufferName);
        pai.setEnabled(true);
        pai.getRoles().add("ROLE_TEST");

        // create group and add buffer to filtered process (buffer process is enabled but has roles)
        ProcessFactory jts = Processors.createProcessFactory(bufferName);
        ProcessGroupInfo jtsGroup = new ProcessGroupInfoImpl();
        jtsGroup.setFactoryClass(jts.getClass());
        jtsGroup.setEnabled(true);
        jtsGroup.getFilteredProcesses().add(pai);

        List<ProcessGroupInfo> pgs = wps.getProcessGroups();
        pgs.clear();
        pgs.add(jtsGroup);

        wps.setCatalogMode(getMode());

        gs.save(wps);
    }

    @After
    public void cleanup() throws Exception {
        GeoServer gs = getGeoServer();
        WPSInfo wps = gs.getService(WPSInfo.class);
        wps.getProcessGroups().clear();
        gs.save(wps);
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    protected abstract CatalogMode getMode();

    protected static final String executeRequestXml =
            "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                    + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                    + "<ows:Identifier>JTS:buffer</ows:Identifier>"
                    + "<wps:DataInputs>"
                    + "<wps:Input>"
                    + "<ows:Identifier>distance</ows:Identifier>"
                    + "<wps:Data>"
                    + "<wps:LiteralData>1</wps:LiteralData>"
                    + "</wps:Data>"
                    + "</wps:Input>"
                    + "<wps:Input>"
                    + "<ows:Identifier>geom</ows:Identifier>"
                    + "<wps:Data>"
                    + "<wps:ComplexData mimeType=\"text/xml; subtype=gml/2.1.2\">"
                    + "<gml:Polygon xmlns:gml='http://www.opengis.net/gml'>"
                    + "<gml:exterior>"
                    + "<gml:LinearRing>"
                    + "<gml:coordinates>1 1 2 1 2 2 1 2 1 1</gml:coordinates>"
                    + "</gml:LinearRing>"
                    + "</gml:exterior>"
                    + "</gml:Polygon>"
                    + "</wps:ComplexData>"
                    + "</wps:Data>"
                    + "</wps:Input>"
                    + "</wps:DataInputs>"
                    + "<wps:ResponseForm>"
                    + "<wps:ResponseDocument storeExecuteResponse='false'>"
                    + "<wps:Output>"
                    + "<ows:Identifier>result</ows:Identifier>"
                    + "</wps:Output>"
                    + "</wps:ResponseDocument>"
                    + "</wps:ResponseForm>"
                    + "</wps:Execute>";
}
