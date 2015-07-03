/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import static org.easymock.EasyMock.*;
import static org.easymock.classextension.EasyMock.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.test.XMLTestSupport;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class GSSXMLTestSupport extends XMLTestSupport {
    
    protected static final String SF_NAMESPACE = "http://www.openplans.org/spearfish";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        registerNamespaceMapping("sf", SF_NAMESPACE);
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("sf", SF_NAMESPACE);
        namespaces.put("gss", GSS.NAMESPACE);
        namespaces.put("wfs", WFS.NAMESPACE);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected Configuration createConfiguration() {
        // enough mocking to make tests work, we don't even attempt to create a real mock catalog
        ResourcePool resourcePool = createNiceMock(ResourcePool.class);
        
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getResourceLoader()).andReturn(null).anyTimes();
        expect(catalog.getResourcePool()).andReturn(resourcePool).anyTimes();
        expect(catalog.getFeatureTypes()).andReturn(Collections.EMPTY_LIST).anyTimes();
        replay(catalog);
        
        GeoServer gs = createNiceMock(GeoServer.class);
        expect(gs.getService(WFSInfo.class)).andReturn(new WFSInfoImpl()).anyTimes();
        expect(gs.getCatalog()).andReturn(catalog).anyTimes();
        replay(gs);
        
        FeatureTypeSchemaBuilder.GML3 schemaBuilder = new FeatureTypeSchemaBuilder.GML3(gs);
        WFS wfs = new WFS(schemaBuilder);
        
        return new GSSConfiguration(new WFSConfiguration(catalog, schemaBuilder, wfs), new GSS(wfs), catalog);
    }
    
    Document dom(String resourceName) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(this.getClass().getResourceAsStream(resourceName));
    }

}
