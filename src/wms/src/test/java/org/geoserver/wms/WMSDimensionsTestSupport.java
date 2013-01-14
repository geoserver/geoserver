/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.junit.After;
import org.junit.Before;

public abstract class WMSDimensionsTestSupport extends WMSTestSupport {

    protected QName V_TIME_ELEVATION = new QName(MockData.SF_URI, "TimeElevation", MockData.SF_PREFIX);
    protected QName V_TIME_ELEVATION_EMPTY = new QName(MockData.SF_URI, "TimeElevationEmpty", MockData.SF_PREFIX);
    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    
    protected static final String UNITS = "foot";
    protected static final String UNIT_SYMBOL = "ft";

    CoverageInfo wattemp;
    FeatureTypeInfo te,teEmpty;
    
    @Before
    public void saveOriginalInfoObjects () throws Exception {
        wattemp=getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        te=getCatalog().getFeatureTypeByName(V_TIME_ELEVATION.getLocalPart());
        teEmpty=getCatalog().getFeatureTypeByName(V_TIME_ELEVATION_EMPTY.getLocalPart());
    }
    
    @After
    public void restoreOriginalInfoObjects() throws Exception {
        wattemp.getMetadata().clear();
        getCatalog().save(wattemp);
        te.getMetadata().clear();
        getCatalog().save(te);
        teEmpty.getMetadata().clear();
        getCatalog().save(teEmpty);
    }
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("", "http://www.opengis.net/wms");
        namespaces.put("wms", "http://www.opengis.net/wms");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);
        
        Map map = new HashMap();
        map.put(MockData.KEY_STYLE, "TimeElevation");
        Catalog catalog = getCatalog();
        testData.addStyle("TimeElevation","TimeElevation.sld",WMSDimensionsTestSupport.class,catalog);
        testData.addVectorLayer(V_TIME_ELEVATION,map,
                "TimeElevation.properties",
                WMSDimensionsTestSupport.class,catalog);
        testData.addVectorLayer(V_TIME_ELEVATION_EMPTY,map,
                "TimeElevationEmpty.properties",
                WMSDimensionsTestSupport.class,catalog);        
        
        
        testData.addStyle("temperature","../temperature.sld",getClass(),catalog);
        Map propertyMap = new HashMap();
        propertyMap.put(LayerProperty.STYLE,"temperature");
        testData.addRasterLayer(WATTEMP, "watertemp.zip", null, propertyMap, SystemTestData.class, catalog);

        
    }
    

//    @Override
//    protected void setUpInternal() throws Exception {
//        super.setUpInternal();
//        
//        GeoServerInfo global = getGeoServer().getGlobal();
//        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
//        getGeoServer().save(global);
//        
//        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
//        wms.getSRS().add("EPSG:4326");
//        getGeoServer().save(wms);
//        
//        Map<String, String> namespaces = new HashMap<String, String>();
//        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
//        namespaces.put("wfs", "http://www.opengis.net/wfs");
//        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
//        namespaces.put("gml", "http://www.opengis.net/gml");
//        namespaces.put("", "http://www.opengis.net/wms");
//        namespaces.put("wms", "http://www.opengis.net/wms");
//        getTestData().registerNamespaces(namespaces);
//        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
//    }
//    
    protected void setupVectorDimension(String featureTypeName, String metadata, String attribute, 
            DimensionPresentation presentation, Double resolution, String units, String unitSymbol) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(attribute);
        di.setPresentation(presentation);
        if(resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        di.setUnits(units);
        di.setUnitSymbol(unitSymbol);
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }

    protected void setupVectorDimension(String metadata, String attribute, 
            DimensionPresentation presentation, Double resolution, String units, String unitSymbol) {
        setupVectorDimension("TimeElevation", metadata, attribute, presentation, resolution, units, unitSymbol);
    }
    
    protected void setupRasterDimension(String metadata, DimensionPresentation presentation, 
            Double resolution, String units, String unitSymbol) {
        CoverageInfo info = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if(resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        di.setUnits(units);
        di.setUnitSymbol(unitSymbol);
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }

}
