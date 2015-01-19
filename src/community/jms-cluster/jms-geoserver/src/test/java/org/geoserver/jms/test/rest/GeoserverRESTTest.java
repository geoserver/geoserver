package org.geoserver.jms.test.rest;
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
//package org.geoserver.cluster.test.rest;
//
//
//
//import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
//import it.geosolutions.geoserver.rest.GeoServerRESTReader;
//import it.geosolutions.geoserver.rest.decoder.RESTCoverage;
//import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
//import it.geosolutions.geoserver.rest.decoder.RESTDataStore;
//import it.geosolutions.geoserver.rest.decoder.RESTFeatureType;
//import it.geosolutions.geoserver.rest.decoder.RESTLayer;
//import it.geosolutions.geoserver.rest.decoder.RESTLayerGroup;
//import it.geosolutions.geoserver.rest.decoder.utils.NameLinkElem;
//
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.List;
//
//import org.apache.log4j.Logger;
//import org.junit.Assert;
//import org.junit.Before;
//
///**
// * Initializes REST params.
// * <P>
// * <B>These tests are destructive, so you have to explicitly enable them</B>
// * by setting the env var <TT>jms_mastertest</TT> to <TT>true</TT>.
// * <P>
// * The target geoserver instance can be customized by defining the following env vars: <ul>
// * <LI><TT>jms_masterurl</TT> (default <TT>http://localhost:8181/geoserver</TT>)</LI>
// * <LI><TT>jms_masteruser</TT> (default: <TT>admin</TT>)</LI>
// * <LI><TT>jms_masterpass</TT> (default: <TT>geoserver</TT>)</LI>
// * </ul>
// *
// * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
// * @author etj
// */
//public abstract class GeoserverRESTTest extends Assert {
//    private final static Logger LOGGER = Logger.getLogger(GeoserverRESTTest.class);
//
//    protected static final String DEFAULT_WS = "geosolutions";
//
//	public static final String RESTURL;
//	public static final String RESTUSER;
//	public static final String RESTPW;
//	public static final GeoServerRESTReader masterReader;
//	
//	public static final String CLIENT_RESTURL[];
//	public static final String CLIENT_RESTUSER[];
//	public static final String CLIENT_RESTPW[];
//	public static final GeoServerRESTReader reader[];
//	public static final int nSlaves;
//    
//	public static final long TIMEOUT;
//	public static final GeoServerRESTPublisher publisher;
//
//    private static boolean enabled = false;
//    private static Boolean existgs = null;
//    
//	static {
//        RESTURL  = getenv("jms_masterurl",  "http://192.168.1.56:8080/geoserver");
//        RESTUSER = getenv("jms_masteruser", "admin");
//        RESTPW   = getenv("jms_masterpass",   "geoserver");
//        URL lurl = null;
//		try {
//			lurl = new URL(RESTURL);
//			
//		} catch (MalformedURLException e) {
//			fail(e.getLocalizedMessage());
//		}
//		masterReader = new GeoServerRESTReader(lurl, RESTUSER, RESTPW);
//		
//
//        final String CLIENT_RESTURL_str  = getenv("jms_slaveurl",  "http://192.168.1.57:8080/geoserver,http://192.168.1.58:8080/geoserver");
//        final String CLIENT_RESTUSER_str = getenv("jms_slaveuser", "admin,admin");
//        final String CLIENT_RESTPW_str   = getenv("jms_slavepw",   "geoserver,geoserver");
//        
//        TIMEOUT= Integer.parseInt(getenv("jms_slavetimeout",   "1000"));
//        
//        CLIENT_RESTURL=CLIENT_RESTURL_str.split(",");
//        CLIENT_RESTUSER=CLIENT_RESTUSER_str.split(",");
//        CLIENT_RESTPW=CLIENT_RESTPW_str.split(",");
//        
//        nSlaves=CLIENT_RESTURL.length;
//        if (nSlaves!= CLIENT_RESTPW.length || nSlaves!= CLIENT_RESTUSER.length){
//        	throw new IllegalArgumentException("bad nSlaves settings");
//        }
//        
//        // These tests will destroy data, so let's make sure we do want to run them
//        enabled  = getenv("jms_mastertest", "false").equalsIgnoreCase("true");
//        if( ! enabled )
//            LOGGER.warn("Tests are disabled. Please read the documentation to enable them.");
//
//		reader = new GeoServerRESTReader[nSlaves];
//		for (int i=0; i< nSlaves; i++){
//			try {
//				
//				lurl = new URL(CLIENT_RESTURL[i]);
//				reader[i] = new GeoServerRESTReader(lurl, CLIENT_RESTUSER[i], CLIENT_RESTPW[i]);
//				
//			} catch (MalformedURLException ex) {
//				fail(ex.getLocalizedMessage());
//			}
//		}
//		
//        publisher = new GeoServerRESTPublisher(RESTURL, RESTUSER, RESTPW);
//	}
//
//    private static String getenv(String envName, String envDefault) {
//        String env = System.getenv(envName);
//        String ret = System.getProperty(envName, env);
//        LOGGER.debug("env var " + envName + " is " + ret);
//        return ret != null? ret : envDefault;
//    }
//
//
//
//    private String testName;
//
//    public GeoserverRESTTest(String testName) {
//        this.testName=testName;
//    }
//    
//    @Before
//    public void setUp() throws Exception {
//
//        if(enabled) {
//            if(existgs == null) {
//            	existgs=masterReader.existGeoserver();
//            	for (int i=0; i< nSlaves; i++){
//	                existgs = (existgs.booleanValue() && reader[i].existGeoserver());
//            	}
//            	if ( ! existgs ) {
//                    LOGGER.error("TESTS WILL FAIL BECAUSE NO GEOSERVER WAS FOUND AT " + RESTURL + " ("+ RESTUSER+":"+RESTPW+")");
//                } else {
//                    LOGGER.info("Using geoserver instance " + RESTUSER+":"+RESTPW+ " @ " + RESTURL);
//                }
//            }
//            if ( ! existgs ) {
//                System.out.println("Failing test " + this.getClass().getSimpleName() + "::" + this.testName + " : geoserver not found");
//                fail("GeoServer not found");
//            }
//            System.out.println("\n-------------------> RUNNING TEST " + this.testName);
//        } else {
//            System.out.println("Skipping test " + this.getClass().getSimpleName() + "::" + this.testName);
//        }
//    }
//
//    protected boolean enabled() {
//        return enabled;
//    }
//
//    protected void deleteAll() {
//        LOGGER.info("Starting DELETEALL procedure");
//        deleteAllLayerGroups();
//        try {
//			Thread.sleep(TIMEOUT);
//		} catch (InterruptedException e) {
//			fail(e.getLocalizedMessage());
//		}
//        for (int i=0; i< nSlaves; i++){
//        	assertTrue("Some layergroups were not removed", reader[i].getLayerGroups().isEmpty());
//        }
//
//        deleteAllLayers();
//        try {
//			Thread.sleep(TIMEOUT);
//		} catch (InterruptedException e) {
//			fail(e.getLocalizedMessage());
//		}
//        for (int i=0; i< nSlaves; i++){
//        	assertTrue("Some layers were not removed", reader[i].getLayers().isEmpty());
//        }
//
//        deleteAllCoverageStores();
//        deleteAllDataStores();
//
//        deleteAllWorkspaces();
////        assertTrue("Some workspaces were not removed", reader.getWorkspaces().isEmpty());
//
//        deleteAllStyles();
//        try {
//			Thread.sleep(TIMEOUT);
//		} catch (InterruptedException e) {
//			fail(e.getLocalizedMessage());
//		}
//        for (int i=0; i< nSlaves; i++){
//        	assertTrue("Some styles were not removed", reader[i].getStyles().isEmpty());
//        }
//        LOGGER.info("ENDING DELETEALL procedure");
//    }
//
//    private void deleteAllLayerGroups() {
//    	
//    	List<String> groups = masterReader.getLayerGroups().getNames();
//    	LOGGER.info("Found " + groups.size() + " layerGroups");
//        for (String groupName : groups) {
//            RESTLayerGroup group = masterReader.getLayerGroup(groupName);
//            StringBuilder sb = new StringBuilder("Group: ").append(groupName).append(":");
//            for (NameLinkElem layer : group.getLayerList()) {
//                sb.append(" ").append(layer);
//            }
//
//            boolean removed = publisher.removeLayerGroup(groupName);
//            LOGGER.info(sb.toString()+ ": removed: " + removed);
//            assertTrue("LayerGroup not removed: " + groupName, removed);
//        }
//        
//    }
//
//    private void deleteAllLayers() {
//        List<String> layers = masterReader.getLayers().getNames();
//        for (String layerName : layers) {
//            RESTLayer layer = masterReader.getLayer(layerName);
//            if(layer.getType() == RESTLayer.Type.VECTOR)
//                deleteFeatureType(layer);
//            else if(layer.getType() == RESTLayer.Type.RASTER)
//                deleteCoverage(layer);
//            else
//                LOGGER.error("Unknown layer type " + layer.getType());
//        }
//    }
//
//    private void deleteAllCoverageStores() {
//        List<String> workspaces = masterReader.getWorkspaceNames();
//        for (String workspace : workspaces) {
//            List<String> stores = masterReader.getCoverageStores(workspace).getNames();
//            for (String storename : stores) {
////                RESTCoverageStore store = reader.getCoverageStore(workspace, storename);
//
//                LOGGER.warn("Deleting CoverageStore " + workspace + " : " + storename);
//                boolean removed = publisher.removeCoverageStore(workspace, storename);
//                assertTrue("CoverageStore not removed " + workspace + " : " + storename, removed);
//            }
//        }
//    }
//
//    private void deleteAllDataStores() {
//        List<String> workspaces = masterReader.getWorkspaceNames();
//        for (String workspace : workspaces) {
//            List<String> stores = masterReader.getDatastores(workspace).getNames();
//
//            for (String storename : stores) {
//                RESTDataStore store = masterReader.getDatastore(workspace, storename);
//
////                if(store.getType() == RESTDataStore.DBType.POSTGIS) {
////                    LOGGER.info("Skipping PG datastore " + store.getWorkspaceName()+":"+store.getName());
////                    continue;
////                }
//
//                LOGGER.warn("Deleting DataStore " + workspace + " : " + storename);
//                boolean removed = publisher.removeDatastore(workspace, storename);
//                assertTrue("DataStore not removed " + workspace + " : " + storename, removed);
//            }
//        }
//    }
//
//    protected void deleteAllWorkspaces() {
//        List<String> workspaces = masterReader.getWorkspaceNames();
//        for (String workspace : workspaces) {
//                LOGGER.warn("Deleting Workspace " + workspace );
//                boolean removed = publisher.removeWorkspace(workspace);
//                assertTrue("Workspace not removed " + workspace, removed );
//
//        }
//    }
//
//    private void deleteAllStyles() {
//        List<String> styles = masterReader.getStyles().getNames();
//        for (String style : styles) {
//                LOGGER.warn("Deleting Style " + style );
//                boolean removed = publisher.removeStyle(style);
//                assertTrue("Style not removed " + style, removed );
//
//        }
//    }
//
//    private void deleteFeatureType(RESTLayer layer) {
//        RESTFeatureType featureType = masterReader.getFeatureType(layer);
//        RESTDataStore datastore = masterReader.getDatastore(featureType);
//
//        LOGGER.warn("Deleting FeatureType"
//                + datastore.getWorkspaceName() + " : "
//                + datastore.getName() + " / "
//                + featureType.getName()
//                );
//
//        boolean removed = publisher.unpublishFeatureType(datastore.getWorkspaceName(), datastore.getName(), layer.getName());
//        assertTrue("FeatureType not removed:"
//                + datastore.getWorkspaceName() + " : "
//                + datastore.getName() + " / "
//                + featureType.getName(),
//                removed);
//
//    }
//
//    private void deleteCoverage(RESTLayer layer) {
//        RESTCoverage coverage = masterReader.getCoverage(layer);
//        RESTCoverageStore coverageStore = masterReader.getCoverageStore(coverage);
//
//        LOGGER.warn("Deleting Coverage "
//                + coverageStore.getWorkspaceName() + " : "
//                + coverageStore.getName() + " / "
//                + coverage.getName());
//
//        boolean removed = publisher.unpublishCoverage(coverageStore.getWorkspaceName(),
//                                                        coverageStore.getName(),
//                                                        coverage.getName());
//        assertTrue("Coverage not deleted "
//                + coverageStore.getWorkspaceName() + " : "
//                + coverageStore.getName() + " / "
//                + coverage.getName(),
//                removed);
//    }
//
//}