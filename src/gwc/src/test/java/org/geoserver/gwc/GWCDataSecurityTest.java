/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 * 
 * @author Niels Charlier, Scitus Development
 */
public class GWCDataSecurityTest extends WMSTestSupport {

    static final Logger LOGGER = Logging.getLogger(GWCDataSecurityTest.class);

    /**
     * Add the test resource access manager in the spring context
     */
   
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wms/ResourceAccessManagerContext.xml");
    }
    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        GWC.get().getConfig().setSecurityEnabled(true);
        
        testData.addStyle("raster","raster.sld",SystemTestData.class,getCatalog());
        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX),
                "raster-filter-test.zip",null, properties, SystemTestData.class, getCatalog());
                
        GeoServerUserGroupStore ugStore= getSecurityManager().
                loadUserGroupService(AbstractUserGroupService.DEFAULT_NAME).createStore();
        
        ugStore.addUser(ugStore.createUserObject("cite", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_nomosaic", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_cropmosaic", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_filtermosaic", "cite", true));
        ugStore.store();
        
        GeoServerRoleStore roleStore= getSecurityManager().getActiveRoleService().createStore();
        GeoServerRole role = roleStore.createRoleObject("ROLE_DUMMY");
        roleStore.addRole(role);
        roleStore.associateRoleToUser(role, "cite");
        roleStore.associateRoleToUser(role, "cite_nomosaic");
        roleStore.associateRoleToUser(role, "cite_cropmosaic");  
        roleStore.associateRoleToUser(role, "cite_filtermosaic");             
        roleStore.store();
        
        // populate the access manager
        Catalog catalog = getCatalog();
        TestResourceAccessManager tam = (TestResourceAccessManager) applicationContext
                .getBean("testResourceAccessManager");
        

        CoverageInfo coverage = catalog.getCoverageByName("sf:mosaic");
        
        //layer disable
        tam.putLimits("cite_nomosaic", coverage, new CoverageAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, null));
        
        // image cropping setup
        WKTReader wkt = new WKTReader();
        MultiPolygon cropper = (MultiPolygon) wkt.read("MULTIPOLYGON(((140 -50, 150 -50, 150 -30, 140 -30, 140 -50)))");
        tam.putLimits("cite_cropmosaic", coverage, new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, cropper, null));
        
        // filter setup
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Filter filter = ff.contains(ff.property("geometry"), ff.literal(cropper));
        tam.putLimits("cite_filtermosaic", coverage, new CoverageAccessLimits(CatalogMode.HIDE, filter, null, null));
        
    }
        
    @Test
    public void testNoMosaic() throws Exception {
        GWC.get().getConfig().setSecurityEnabled(true);
        
        //first to cache
        setRequestAuth("cite", "cite");
        String path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType() );
        
        // try again, now should be cached
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
        
        //try now as different user
        setRequestAuth("cite_nomosaic", "cite");        
        response = getAsServletResponse(path);
        assertEquals("application/xml", response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertTrue(str.contains("org.geotools.ows.ServiceException: Could not find layer sf:mosaic"));
    }
    
    @Test
    public void testCroppedMosaic() throws Exception {
        //first to cache
        setRequestAuth("cite", "cite");
        String path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
        
        // this should fail
        setRequestAuth("cite_cropmosaic", "cite");
        
        path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals("application/xml", response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertTrue(str.contains("org.geotools.ows.ServiceException: Access denied to bounding box on layer sf:mosaic"));
        
        //but this should be fine
        path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=143.4375,-42.1875,146.25,-39.375&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }  
    
    @Test
    public void testFilterMosaic() throws Exception {
        //first to cache
        setRequestAuth("cite", "cite");
        String path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
        
        // this should fail
        setRequestAuth("cite_filtermosaic", "cite");
        
        path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals("application/xml", response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertTrue(str.contains("org.geotools.ows.ServiceException: Access denied to bounding box on layer sf:mosaic"));
        
        //but this should be fine
        path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=143.4375,-42.1875,146.25,-39.375&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }  

}
