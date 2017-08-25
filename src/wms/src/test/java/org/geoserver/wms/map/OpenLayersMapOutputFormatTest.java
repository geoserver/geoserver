/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;

public class OpenLayersMapOutputFormatTest extends WMSTestSupport {

    private OpenLayersMapOutputFormat mapProducer;
    
    Pattern lookForEscapedParam = Pattern
            .compile(Pattern
                    .quote("\"</script><script>alert('x-scripted');</script><script>\": 'foo'"));
    
    @Before
    public void setMapProducer() throws Exception {
        Logging.getLogger("org.geotools.rendering").setLevel(Level.OFF);
        this.mapProducer = getProducerInstance();
    }
    
    protected OpenLayersMapOutputFormat getProducerInstance() {
        return new OpenLayersMapOutputFormat(getWMS());
    }
    
    @After
    public void unsetMapProducer() throws Exception {
        this.mapProducer = null;
    }
    
    /**
     * Test for GEOS-5318: xss vulnerability when a weird parameter is added to the
     * request (something like: %3C%2Fscript%
     * 3E%3Cscript%3Ealert%28%27x-scripted%27%29%3C%2Fscript%3E%3Cscript%3E=foo) the
     * causes js code execution.
     * 
     * @throws IOException
     */
    @Test
    public void testXssFix() throws Exception {
    
        Catalog catalog = getCatalog();
        final FeatureSource fs = catalog.getFeatureTypeByName(
                MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart())
                .getFeatureSource(null, null);
    
        final Envelope env = fs.getBounds();
    
        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);
    
        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        request.getRawKvp().put(
                "</script><script>alert('x-scripted');</script><script>", "foo");
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(
                new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);
    
        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        FeatureLayer layer = new FeatureLayer(fs, basicStyle);
        layer.setTitle("Title");
        map.addLayer(layer);
        request.setFormat("application/openlayers");
        RawMap rawMap = this.mapProducer.produceMap(map);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rawMap.writeTo(bos);
        String htmlDoc = new String(bos.toByteArray(), "UTF-8");
        // check that weird param is correctly encoded to avoid js code execution
        int index = htmlDoc
                .replace("\\n", "")
                .replace("\\r", "")
                .indexOf(
                        "\"</script\\><script\\>alert(\\'x-scripted\\');</script\\><script\\>\": 'foo'");
        assertTrue(index > -1);
    }
}
