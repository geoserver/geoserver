package org.geoserver.wcs2_0;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Envelope2D;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WCSMultiDimSubsetTest extends WCSNetCDFBaseTest{
    private static final QName LAMBERTMOSAIC = new QName(CiteTestData.WCS_URI,"lambert",CiteTestData.WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // workaround to add our custom multi dimensional format
        try {
            Field field = GetCoverage.class.getDeclaredField("mdFormats");
            field.setAccessible(true);
            ((Set<String>) field.get(null)).add(WCSResponseInterceptor.MIME_TYPE);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        super.onSetUp(testData);
        testData.addRasterLayer(LAMBERTMOSAIC, "lambertmosaic.zip", null,null,this.getClass(),getCatalog());
        setupRasterDimension(getLayerId(LAMBERTMOSAIC), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        
    }
    
    /**
     * Tests if we can select a single pixel value using a WCS request
     * @throws Exception
     */
    @Test
    public void sliceLambert() throws Exception {
        
        // check we can read it as a TIFF and it is similare to the original one
        GridCoverage2D targetCoverage = null, sourceCoverage=null;
        
        try {            
            
            // === slicing on Y axis
            // source 
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName(LAMBERTMOSAIC.getLocalPart()).getGridCoverageReader(null, null).read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();
            
            
            // subsample using the original extension
            MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                            "&coverageId=wcs__lambert&&Format=application/custom" +
                            "&subset=E,http://www.opengis.net/def/crs/EPSG/0/31300(" +sourceEnvelope.x+","+(sourceEnvelope.x+25)+")"+
                            "&subset=N,http://www.opengis.net/def/crs/EPSG/0/31300(" +sourceEnvelope.y+","+(sourceEnvelope.y+25)+")");
                        
            assertNotNull(response);
            targetCoverage  = applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();
            
            assertEquals((Object)sourceCoverage.getCoordinateReferenceSystem(), (Object)targetCoverage.getCoordinateReferenceSystem());
            
            assertTrue(targetCoverage instanceof GranuleStack);
            GridCoverage2D firstResult = ((GranuleStack)targetCoverage).getGranules().get(0);
            // checks
            
            //TODO this grid geometry is not entirely what we expect yet, perhaps reveals another issue with this projection
            assertEquals(1, firstResult.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1, firstResult.getGridGeometry().getGridRange().getSpan(1));
            assertEquals(0, firstResult.getGridGeometry().getGridRange().getLow(0));
            assertEquals(326, firstResult.getGridGeometry().getGridRange().getLow(1));
            

        } finally {            
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,e.getLocalizedMessage(),e);
            }
            try{
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,e.getLocalizedMessage(),e);
            }
        }
            
    }
}
