package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.platform.OWS20Exception;
import org.junit.Test;

public class InterpolationKvpParserTest {

    InterpolationKvpParser parser = new InterpolationKvpParser();
    
    @Test
    public void testInvalidValues() throws Exception {
        try {
            parser.parse(":interpolation");
            fail("should have thrown an exception");
        } catch(OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
        
        try {
            parser.parse("a:linear,,b:nearest");
            fail("should have thrown an exception");
        } catch(OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
        
        try {
            parser.parse("a::linear");
            fail("should have thrown an exception");
        } catch(OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
    }

    private void checkInvalidSyntaxException(OWS20Exception e) {
        assertNotNull(e.getHttpCode());
        assertEquals(400, e.getHttpCode().intValue());
        assertEquals("InvalidEncodingSyntax", e.getCode());
        assertEquals("interpolation", e.getLocator());
    }
    
    @Test
    public void testUniformValue() throws Exception {
        InterpolationType it = (InterpolationType) parser.parse("http://www.opengis.net/def/interpolation/OGC/1/linear");
        assertEquals("http://www.opengis.net/def/interpolation/OGC/1/linear", it.getInterpolationMethod().getInterpolationMethod());
    }
    
    
    @Test
    public void testSingleAxis() throws Exception {
        InterpolationType it = (InterpolationType) parser.parse("http://www.opengis.net/def/axis/OGC/1/latitude:http://www.opengis.net/def/interpolation/OGC/1/linear");
        EList<InterpolationAxisType> axes = it.getInterpolationAxes().getInterpolationAxis();
        assertEquals(1, axes.size());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/latitude", axes.get(0).getAxis());
        assertEquals("http://www.opengis.net/def/interpolation/OGC/1/linear", axes.get(0).getInterpolationMethod());
    }
    
    @Test
    public void testMultiAxis() throws Exception {
        InterpolationType it = (InterpolationType) parser.parse("http://www.opengis.net/def/axis/OGC/1/latitude:" +
        		"http://www.opengis.net/def/interpolation/OGC/1/linear," +
        		"http://www.opengis.net/def/axis/OGC/1/longitude:" +
        		"http://www.opengis.net/def/interpolation/OGC/1/nearest");
        EList<InterpolationAxisType> axes = it.getInterpolationAxes().getInterpolationAxis();
        assertEquals(2, axes.size());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/latitude", axes.get(0).getAxis());
        assertEquals("http://www.opengis.net/def/interpolation/OGC/1/linear", axes.get(0).getInterpolationMethod());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/longitude", axes.get(1).getAxis());
        assertEquals("http://www.opengis.net/def/interpolation/OGC/1/nearest", axes.get(1).getInterpolationMethod());
    }
    
}

