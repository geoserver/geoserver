package org.geoserver.wms;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;

public class WMSDimensionsTestSupport extends WMSTestSupport {

    protected QName V_TIME_ELEVATION = new QName(MockData.SF_URI, "TimeElevation", MockData.SF_PREFIX);
    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // add vector data set with time and elevation
    	dataDirectory.addStyle("TimeElevation", getClass().getResource("../TimeElevation.sld"));
        dataDirectory.addPropertiesType(V_TIME_ELEVATION, getClass().getResource("../TimeElevation.properties"), 
        		Collections.singletonMap(MockData.KEY_STYLE, "TimeElevation"));
        
        // add a raster mosaic with time and elevation
        URL style = getClass().getResource("../temperature.sld");
        String styleName = "temperature";
        dataDirectory.addStyle(styleName, style);
        dataDirectory.addCoverage(WATTEMP, TestData.class.getResource("watertemp.zip"),
                        null, styleName);
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);
        
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
    
    protected void setupVectorDimension(String metadata, String attribute, DimensionPresentation presentation, Double resolution) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName("TimeElevation");
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(attribute);
        di.setPresentation(presentation);
        if(resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }
    
    protected void setupRasterDimension(String metadata, DimensionPresentation presentation, Double resolution) {
        CoverageInfo info = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if(resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }

}
