/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;

public class HTMLFeatureInfoOutputFormatTest extends WMSTestSupport {
    private HTMLFeatureInfoOutputFormat outputFormat;
    
    private FeatureCollectionType fcType;
    
    Map<String, Object> parameters;
    
    GetFeatureInfoRequest getFeatureInfoRequest;
    
    private static final String templateFolder = "/org/geoserver/wms/featureinfo/";
    
    @Before
    public void setUp() throws URISyntaxException, IOException {
        outputFormat = new HTMLFeatureInfoOutputFormat(getWMS());
        
        // configure template loader
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(
                this.getClass(), getDataDirectory()) {
    
            @Override
            public Object findTemplateSource(String path) throws IOException {
                String templatePath;
                if (path.toLowerCase().contains("content")) {
                    templatePath = "test_content.ftl";
    
                } else {
                    templatePath = "empty.ftl";
                }
                try {
                    return new File(this.getClass()
                            .getResource(templateFolder + templatePath).toURI());
                } catch (URISyntaxException e) {
                    return null;
    
                }
    
            }
    
        };
        outputFormat.templateLoader = templateLoader;
        
        // test request with some parameters to use in templates
        Request request = new Request();
        parameters = new HashMap<String, Object>();
        parameters.put("LAYER", "testLayer");        
        Map<String, String> env = new HashMap<String, String>();
        env.put("TEST1", "VALUE1");
        env.put("TEST2", "VALUE2");        
        parameters.put("ENV", env);
        request.setKvp(parameters);
        
        Dispatcher.REQUEST.set(request);
        
        final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
        
        fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fcType.getFeature().add(featureType.getFeatureSource(null, null).getFeatures());
        
        // fake layer list
        List<MapLayerInfo> queryLayers = new ArrayList<MapLayerInfo>();               
        LayerInfo layerInfo = new LayerInfoImpl();
        layerInfo.setType(LayerInfo.Type.VECTOR);
        ResourceInfo resourceInfo = new FeatureTypeInfoImpl(null);
        NamespaceInfo nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("topp");
        nameSpace.setURI("http://www.topp.org");
        resourceInfo.setNamespace(nameSpace);
        layerInfo.setResource(resourceInfo);
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
                
    }

    /**
     * Test request values are inserted in processed template
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testRequestParametersAreEvaluatedInTemplate() throws IOException {        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
         
        assertEquals("VALUE1,VALUE2,testLayer" , result);    
    }
    
    /**
     * Test that if template asks a request parameter that is not present in request
     * an exception is thrown.
     * 
     */
    @Test
    public void testErrorWhenRequestParametersAreNotDefined() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        boolean error = false;
        
        // remove one parameter required in template
        parameters.remove("LAYER");
        try {
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        } catch (IOException e) {
            error = true;
        }
        assertTrue(error); 
    }
}
