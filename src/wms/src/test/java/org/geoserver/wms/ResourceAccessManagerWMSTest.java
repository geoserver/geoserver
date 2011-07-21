package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ResourceAccessManagerWMSTest extends WMSTestSupport {

    static final Logger LOGGER = Logging.getLogger(ResourceAccessManagerWMSTest.class);

    static final String BASE = "wms?" + //
            "SERVICE=WMS&VERSION=1.1.1" + "&HEIGHT=330&WIDTH=780" + //
            "&LAYERS=rstates&STYLES=" + //
            "&FORMAT=image%2Fpng" + //
            "&SRS=EPSG%3A4326" + //
            "&BBOX=-139.84813671875,18.549615234375,-51.85286328125,55.778384765625";

    static final String GET_MAP = BASE + "&REQUEST=GetMap";

    static final String BASE_GET_FEATURE_INFO = BASE + // 
            "&REQUEST=GetFeatureInfo" + //
            "&QUERY_LAYERS=rstates" + //
            "&INFO_FORMAT=text/plain";

    static final String GET_FEATURE_INFO_CALIFORNIA = BASE_GET_FEATURE_INFO + // 
            "&X=191" + // 
            "&Y=178";

    static final String GET_FEATURE_INFO_TEXAS = BASE_GET_FEATURE_INFO + // 
            "&X=368" + // 
            "&Y=227";

    /**
     * Add the test resource access manager in the spring context
     */
    protected String[] getSpringContextLocations() {
        String[] base = super.getSpringContextLocations();
        String[] extended = new String[base.length + 1];
        System.arraycopy(base, 0, extended, 0, base.length);
        extended[base.length] = "classpath:/org/geoserver/wms/ResourceAccessManagerContext.xml";
        return extended;
    }

    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    /**
     * Add the users
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        // add a mosaic as well
        URL style = MockData.class.getResource("raster.sld");
        dataDirectory.addStyle("raster", style);
        dataDirectory.addCoverage(new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX), 
                MockData.class.getResource("raster-filter-test.zip"), null, "raster");

        
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();

        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,ROLE_DUMMY");
        props.put("cite_nostates", "cite,ROLE_DUMMY");
        props.put("cite_noinfo", "cite,ROLE_DUMMY");
        props.put("cite_texas", "cite,ROLE_DUMMY");
        props.put("cite_mosaic1", "cite,ROLE_DUMMY");
        props.put("cite_mosaic2", "cite,ROLE_DUMMY");
        props.store(new FileOutputStream(users), "");
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        // populate the access manager
        Catalog catalog = getCatalog();
        TestResourceAccessManager tam = (TestResourceAccessManager) applicationContext
                .getBean("testResourceAccessManager");
        
        // tile filtering setup
        CoverageInfo coverage = catalog.getCoverageByName("sf:mosaic");
        Filter green = CQL.toFilter("location like 'green%'");
        tam.putLimits("cite_mosaic1", coverage, new CoverageAccessLimits(CatalogMode.HIDE, green, null, null));
        
        // image cropping setup
        WKTReader wkt = new WKTReader();
        MultiPolygon cropper = (MultiPolygon) wkt.read("MULTIPOLYGON(((0 0, 0.5 0, 0.5 0.5, 0 0.5, 0 0)))");
        tam.putLimits("cite_mosaic2", coverage, new CoverageAccessLimits(CatalogMode.HIDE, green, cropper, null));

        // add a wms store too, if possible
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            return;
        }

        // setup the wms store, resource and layer
        CatalogBuilder cb = new CatalogBuilder(catalog);
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL(RemoteOWSTestSupport.WMS_SERVER_URL
                + "service=WMS&request=GetCapabilities");
        catalog.save(wms);
        cb.setStore(wms);
        WMSLayerInfo states = cb.buildWMSLayer("topp:states");
        states.setName("rstates");
        catalog.add(states);
        LayerInfo layer = cb.buildLayer(states);
        catalog.add(layer);

        // hide the layer to cite_nostates
        tam.putLimits("cite_nostates", states, new WMSAccessLimits(CatalogMode.HIDE,
                Filter.EXCLUDE, null, false));
        // disallow getfeatureinfo on the states layer
        tam.putLimits("cite_noinfo", states, new WMSAccessLimits(CatalogMode.HIDE, Filter.INCLUDE,
                null, false));
        // cascade CQL filter, allow feature info
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        Filter texas = ff.equal(ff.property("STATE_NAME"), ff.literal("Texas"), false);
        tam.putLimits("cite_texas", states,
                new WMSAccessLimits(CatalogMode.HIDE, texas, null, true));
    }

    public void testGetMapNoRestrictions() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testNoRestrictions");
            return;
        }

        authenticate("cite", "cite");
        MockHttpServletResponse response = getAsServletResponse(GET_MAP);

        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertNotNull(image);
        assertNotBlank("testNoRestrictions", image);

        // check the colors of some pixels to ensure there has been no filtering
        // a Texas one
        int[] pixel = new int[4];
        image.getData().getPixel(368, 227, pixel);
        assertEquals(77, pixel[0]);
        assertEquals(77, pixel[1]);
        assertEquals(255, pixel[2]);
        // a California one
        image.getData().getPixel(191, 178, pixel);
        assertEquals(77, pixel[0]);
        assertEquals(77, pixel[1]);
        assertEquals(255, pixel[2]);
    }

    public void testGetMapDisallowed() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testGetMapDisallowed");
            return;
        }

        authenticate("cite_nostates", "cite");
        MockHttpServletResponse response = getAsServletResponse(GET_MAP);
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("LayerNotDefined", "//ServiceException/@code", dom);
    }

    public void testGetMapFiltered() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testGetMapFiltered");
            return;
        }

        authenticate("cite_texas", "cite");
        MockHttpServletResponse response = getAsServletResponse(GET_MAP);

        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertNotNull(image);
        assertNotBlank("testGetMapFiltered", image);

        // check the colors of some pixels to ensure there has been no filtering
        // a Texas one
        int[] pixel = new int[4];
        image.getData().getPixel(368, 227, pixel);
        assertEquals(77, pixel[0]);
        assertEquals(77, pixel[1]);
        assertEquals(255, pixel[2]);
        // a California one, this one should be white
        image.getData().getPixel(191, 178, pixel);
        assertEquals(255, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(255, pixel[2]);
    }

    public void testGetFeatureInfoNoRestrictions() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testNoRestrictions");
            return;
        }

        authenticate("cite", "cite");
        String texas = getAsString(GET_FEATURE_INFO_TEXAS);
        assertTrue(texas.contains("STATE_NAME = Texas"));
        String california = getAsString(GET_FEATURE_INFO_CALIFORNIA);
        assertTrue(california.contains("STATE_NAME = California"));
    }
    
    public void testGetFeatureInfoDisallowedLayer() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testNoRestrictions");
            return;
        }

        authenticate("cite_nostates", "cite");
        MockHttpServletResponse response = getAsServletResponse(GET_FEATURE_INFO_TEXAS);
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("LayerNotDefined", "//ServiceException/@code", dom);
    }
    
    public void testGetFeatureInfoDisallowedInfo() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testNoRestrictions");
            return;
        }

        authenticate("cite_noinfo", "cite");
        MockHttpServletResponse response = getAsServletResponse(GET_FEATURE_INFO_TEXAS);
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("OperationNotSupported", "//ServiceException/@code", dom);
    }
    
    public void testGetFeatureInfoFiltered() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.log(Level.WARNING, "Skipping testNoRestrictions");
            return;
        }

        authenticate("cite_texas", "cite");
        String texas = getAsString(GET_FEATURE_INFO_TEXAS);
        assertTrue(texas.contains("STATE_NAME = Texas"));
        String california = getAsString(GET_FEATURE_INFO_CALIFORNIA);
        assertTrue(california.contains("no features were found"));
    }
    
    public void testDoubleMosaic() throws Exception {
        authenticate("cite_mosaic1", "cite");
        String path = "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
        // this one would fail due to the wrapper finalizer dispose of the coverage reader 
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }
    
    public void testRasterFilterGreen() throws Exception {
        // no cql filter, the security one should do
        authenticate("cite_mosaic1", "cite");
        MockHttpServletResponse response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false");
        
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
    }
    
    public void testRasterCrop() throws Exception {
        // this time we should get a cropped image
        authenticate("cite_mosaic2", "cite");
        MockHttpServletResponse response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false");
        
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        
        // bottom right pixel, should be green (inside the crop area)
        int[] pixel = new int[3];
        image.getData().getPixel(0, 149, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
        
        // bottom left, out of the crop area should be black (bgcolor)
        image.getData().getPixel(149, 149, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(0, pixel[1]);
        assertEquals(0, pixel[2]);

    }

}
