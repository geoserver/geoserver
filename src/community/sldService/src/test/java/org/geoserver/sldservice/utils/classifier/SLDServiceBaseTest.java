/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Context;
import org.restlet.data.ClientInfo;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;

import com.vividsolutions.jts.geom.Polygon;

public abstract class SLDServiceBaseTest extends TestCase {

    protected Request request;
    protected Response response;
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    protected Object responseEntity;
    protected Catalog catalog;
    protected Context context;
    protected ResourcePool resourcePool;
    protected FeatureTypeInfoImpl testFeatureTypeInfo;

    protected SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
    protected StyleBuilder styleBuilder = new StyleBuilder();
    protected SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());
    
    protected static final String FEATURETYPE_LAYER = "featuretype_layer";

    protected static final String COVERAGE_LAYER = "coverage_layer";
    
    @Override
    public void setUp() throws Exception {
        responseEntity = null;
        
        context = Mockito.mock(Context.class);
        
        request = Mockito.mock(Request.class);
        ClientInfo clientInfo = new ClientInfo();
        Mockito.when(request.getClientInfo()).thenReturn(clientInfo);
        Mockito.when(request.getAttributes()).thenReturn(attributes);
        
        
        response = Mockito.mock(Response.class);
        catalog = Mockito.mock(Catalog.class);
        
        resourcePool = Mockito.mock(ResourcePool.class);
        Mockito.when(catalog.getResourcePool()).thenReturn(resourcePool);
        
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                if(args.length > 0) {
                    responseEntity = args[0];
                }
                return "";
            }
        }).when(response).setEntity((Representation)Mockito.any());
        
        LayerInfoImpl testFeatureLayerInfo = new LayerInfoImpl();
        
        
        
        testFeatureTypeInfo = new FeatureTypeInfoImpl(catalog);
        
        
        featureTypeBuilder.setName(FEATURETYPE_LAYER);
        featureTypeBuilder.add("id", Integer.class);
        featureTypeBuilder.add("name", String.class);
        featureTypeBuilder.add("foo", Integer.class);
        featureTypeBuilder.add("geometry", Polygon.class);
        
        Mockito.when(resourcePool.getFeatureType(testFeatureTypeInfo)).thenReturn(featureTypeBuilder.buildFeatureType());
        testFeatureLayerInfo.setResource(testFeatureTypeInfo);
        Mockito.when(catalog.getLayerByName(FEATURETYPE_LAYER)).thenReturn(testFeatureLayerInfo);
        
        LayerInfoImpl testCoverageLayerInfo = new LayerInfoImpl();
        CoverageInfoImpl testCoverageInfo = new CoverageInfoImpl(catalog);
        StyleInfoImpl coverageStyleInfo = new StyleInfoImpl(catalog);
        testCoverageLayerInfo.setDefaultStyle(coverageStyleInfo);
        testCoverageLayerInfo.setResource(testCoverageInfo);
        Style coverageStyle = styleBuilder.createStyle(styleBuilder.createRasterSymbolizer());
        
        Mockito.when(resourcePool.getStyle(coverageStyleInfo)).thenReturn(coverageStyle);
        
        Mockito.when(catalog.getLayerByName(COVERAGE_LAYER)).thenReturn(testCoverageLayerInfo);
        
    }
    
    private Class<?> getGeometryType() {
        // TODO Auto-generated method stub
        return null;
    }

    protected void initRequestUrl(Request request, String type) {
        Reference reference = new Reference("http://www.geoserver.org/geoserver/rest/sldservice/"+getServiceUrl()+"."+type);
        String query = "";
        for(String key : attributes.keySet()) {
            query += "&"+key+"=" + attributes.get(key).toString();
        }
        if(query.length() > 0) {
            reference.setQuery(query.substring(1));
        }
        Mockito.when(request.getResourceRef()).thenReturn(reference);
    }
    
    protected Rule[] checkSLD(String resultXml) {
        sldParser.setInput(new StringReader(resultXml));
        StyledLayerDescriptor descriptor = sldParser.parseSLD();
        assertNotNull(descriptor);
        assertNotNull(descriptor.getStyledLayers());
        assertEquals(1, descriptor.getStyledLayers().length);
        StyledLayer layer = descriptor.getStyledLayers()[0];
        assertTrue(layer instanceof NamedLayer);
        NamedLayer namedLayer =(NamedLayer)layer;
        assertNotNull(namedLayer.getStyles());
        assertEquals(1, namedLayer.getStyles().length);
        Style style = namedLayer.getStyles()[0];
        assertNotNull(style.getFeatureTypeStyles());
        assertEquals(1, style.getFeatureTypeStyles().length);
        FeatureTypeStyle featureTypeStyle = style.getFeatureTypeStyles()[0];
        assertNotNull(featureTypeStyle.getRules());
        return featureTypeStyle.getRules();
    }

    protected abstract String getServiceUrl();
}
