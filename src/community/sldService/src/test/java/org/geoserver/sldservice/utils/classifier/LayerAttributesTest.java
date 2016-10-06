/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.io.IOException;

import org.geoserver.sldservice.rest.resource.ListAttributesResource;
import org.restlet.resource.Representation;

public class LayerAttributesTest extends SLDServiceBaseTest {
    
    
    ListAttributesResource resource;
    
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        resource = new ListAttributesResource(context, request, response, catalog);
    }

    
    
    public void testListAttributesForFeatureXml() throws IOException {
        
        attributes.put("layer", FEATURETYPE_LAYER);
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        assertTrue(resultXml.contains("<name>id</name>"));
        assertTrue(resultXml.contains("<type>Integer</type>"));
        assertTrue(resultXml.contains("<name>name</name>"));
        assertTrue(resultXml.contains("<type>String</type>"));
    }
    
    public void testListAttributesForFeatureJson() throws IOException {
        
        attributes.put("layer", FEATURETYPE_LAYER);
        initRequestUrl(request, "json");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultJson = representation.getText().replace("\r", "").replace("\n", "");
        assertTrue(resultJson.contains("{\"name\":\"id\",\"type\":\"Integer\"}"));
        assertTrue(resultJson.contains("{\"name\":\"name\",\"type\":\"String\"}"));
        
    }
    
    public void testListAttributesForCoverageIsEmpty() throws IOException {
        
        attributes.put("layer", COVERAGE_LAYER);
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        assertEquals("<list/>", resultXml);
    }
    
    @Override
    protected String getServiceUrl() {
        return "attributes";
    }
}
