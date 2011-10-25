/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wfs.xml.v1_0_0.WFSConfiguration;


/**
 * Base support class for wfs tests.
 * <p>
 * Deriving from this test class provides the test case with preconfigured
 * geoserver and wfs objects.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public abstract class WFSTestSupport extends GeoServerTestSupport {
    /**
     * @return The global wfs instance from the application context.
     */
    protected WFSInfo getWFS() {
        return getGeoServer().getService( WFSInfo.class );
    }
    
    /**
     * @return The 1.0 service descriptor.
     */
    protected Service getServiceDescriptor10() {
        return (Service) GeoServerExtensions.bean( "wfsService-1.0.0" );
    }
    
    /**
     * @return The 1.1 service descriptor.
     */
    protected Service getServiceDescriptor11() {
        return (Service) GeoServerExtensions.bean( "wfsService-1.1.0" );
    }
    
    /**
     * @return The 1.0 xml configuration.
     */
    protected WFSConfiguration getXmlConfiguration10() {
        return (WFSConfiguration) applicationContext.getBean( "wfsXmlConfiguration-1.0" );
    }
    
    /**
     * @return The 1.1 xml configuration.
     */
    protected org.geoserver.wfs.xml.v1_1_0.WFSConfiguration getXmlConfiguration11() {
        return  (org.geoserver.wfs.xml.v1_1_0.WFSConfiguration) applicationContext.getBean( "wfsXmlConfiguration-1.1" );
    }
    
    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("gs", "http://geoserver.org");
        
        getTestData().registerNamespaces(namespaces);
        setUpNamespaces(namespaces);
        
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
    
    protected void setUpNamespaces(Map<String,String> namespaces) {
    }
}
