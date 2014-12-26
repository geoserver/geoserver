package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.InterpolationType;
import net.opengis.wcs20.RangeItemType;
import net.opengis.wcs20.RangeSubsetType;
import net.opengis.wcs20.ScaleAxisByFactorType;
import net.opengis.wcs20.ScaleAxisType;
import net.opengis.wcs20.ScaleByFactorType;
import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.ScaleToSizeType;
import net.opengis.wcs20.ScalingType;
import net.opengis.wcs20.TargetAxisExtentType;
import net.opengis.wcs20.TargetAxisSizeType;

import org.eclipse.emf.common.util.EList;
import org.geotools.wcs.v2_0.RangeSubset;
import org.geotools.wcs.v2_0.Scaling;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetCoverageKvpTest extends WCSKVPTestSupport {

    @Test
    public void testParseBasic() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=theCoverage");
        
        assertEquals("theCoverage", gc.getCoverageId());
    }
    
    @Test
    public void testNotExistent() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=NotThere&&Format=image/tiff");  
        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");      
    }


    @Test
    public void testExtensionScaleFactor() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&scaleFactor=2");
        
        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleByFactorType sbf = scaling.getScaleByFactor();
        assertEquals(2.0, sbf.getScaleFactor(), 0d);
    }
    
    @Test
    public void testExtensionScaleAxes() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&scaleaxes=http://www.opengis.net/def/axis/OGC/1/i(3.5)," +
                "http://www.opengis.net/def/axis/OGC/1/j(5.0),http://www.opengis.net/def/axis/OGC/1/k(2.0)");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleAxisByFactorType sax = scaling.getScaleAxesByFactor();
        EList<ScaleAxisType> saxes = sax.getScaleAxis();
        assertEquals(3, saxes.size());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/i", saxes.get(0).getAxis());
        assertEquals(3.5d, saxes.get(0).getScaleFactor(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/j", saxes.get(1).getAxis());
        assertEquals(5.0d, saxes.get(1).getScaleFactor(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/k", saxes.get(2).getAxis());
        assertEquals(2.0d, saxes.get(2).getScaleFactor(), 0d);
    }
    
    @Test
    public void testExtensionScaleSize() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&scalesize=http://www.opengis.net/def/axis/OGC/1/i(1000)," +
                "http://www.opengis.net/def/axis/OGC/1/j(1000),http://www.opengis.net/def/axis/OGC/1/k(10)");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleToSizeType sts = scaling.getScaleToSize();
        EList<TargetAxisSizeType> scaleAxes = sts.getTargetAxisSize();
        assertEquals(3, scaleAxes.size());
        assertEquals("http://www.opengis.net/def/axis/OGC/1/i", scaleAxes.get(0).getAxis());
        assertEquals(1000d, scaleAxes.get(0).getTargetSize(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/j", scaleAxes.get(1).getAxis());
        assertEquals(1000d, scaleAxes.get(1).getTargetSize(), 0d);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/k", scaleAxes.get(2).getAxis());
        assertEquals(10d, scaleAxes.get(2).getTargetSize(), 0d);
    }
    
    @Test
    public void testExtensionScaleExtent() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&scaleextent=http://www.opengis.net/def/axis/OGC/1/i(10,20),http://www.opengis.net/def/axis/OGC/1/j(20,30)");
        
        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(1, extensions.size());
        ScalingType scaling = (ScalingType) extensions.get(Scaling.NAMESPACE + ":Scaling");
        ScaleToExtentType ste = scaling.getScaleToExtent();
        assertEquals(2, ste.getTargetAxisExtent().size());
        TargetAxisExtentType tax = ste.getTargetAxisExtent().get(0);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/i", tax.getAxis());
        assertEquals(10.0, tax.getLow(), 0d);        
        assertEquals(20.0, tax.getHigh(), 0d);
        tax = ste.getTargetAxisExtent().get(1);
        assertEquals("http://www.opengis.net/def/axis/OGC/1/j", tax.getAxis());
        assertEquals(20.0, tax.getLow(), 0d);        
        assertEquals(30.0, tax.getHigh(), 0d);
    }
    
    @Test
    public void testExtensionRangeSubset() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&rangesubset=band01,band03:band05,band10,band19:band21");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        
        assertEquals(1, extensions.size());
        RangeSubsetType rangeSubset = (RangeSubsetType) extensions.get(RangeSubset.NAMESPACE + ":RangeSubset");
        
        EList<RangeItemType> items = rangeSubset.getRangeItems();
        assertEquals(4, items.size());
        RangeItemType i1 = items.get(0);
        assertEquals("band01", i1.getRangeComponent());
        RangeItemType i2 = items.get(1);
        assertEquals("band03", i2.getRangeInterval().getStartComponent());
        assertEquals("band05", i2.getRangeInterval().getEndComponent());
        RangeItemType i3 = items.get(2);
        assertEquals("band10", i3.getRangeComponent());
        RangeItemType i4 = items.get(3);
        assertEquals("band19", i4.getRangeInterval().getStartComponent());
        assertEquals("band21", i4.getRangeInterval().getEndComponent());
    }
    
    @Test
    public void testExtensionCRS() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/4326&outputcrs=http://www.opengis.net/def/crs/EPSG/0/32632");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        
        assertEquals(2, extensions.size());
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", extensions.get("http://www.opengis.net/wcs/service-extension/crs/1.0:subsettingCrs"));
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/32632", extensions.get("http://www.opengis.net/wcs/service-extension/crs/1.0:outputCrs"));
    }
    
    @Test
    public void testExtensionInterpolationLinear() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&interpolation=http://www.opengis.net/def/interpolation/OGC/1/linear");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        
        InterpolationType interp = (InterpolationType) extensions.get("http://www.opengis.net/WCS_service-extension_interpolation/1.0:Interpolation");
        assertEquals("http://www.opengis.net/def/interpolation/OGC/1/linear", interp.getInterpolationMethod().getInterpolationMethod());
    }
    
    @Test
    public void testExtensionInterpolationMixed() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=theCoverage&interpolation=http://www.opengis.net/def/interpolation/OGC/1/linear");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        
        InterpolationType interp = (InterpolationType) extensions.get("http://www.opengis.net/WCS_service-extension_interpolation/1.0:Interpolation");
        assertEquals("http://www.opengis.net/def/interpolation/OGC/1/linear", interp.getInterpolationMethod().getInterpolationMethod());
    }
    
    @Test
    public void testGetMissingCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=notThereBaby");
    
        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }
    
    
}
