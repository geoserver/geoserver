/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WebMapServer;
import org.opengis.filter.FilterFactory;

/**
 * Utility class used to check wheter REMOTE_OWS_XXX related tests can be run against the demo
 * server or not or not.
 *
 * @author Andrea Aime - TOPP
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class RemoteOWSTestSupport {

    // support for remote OWS layers
    public static final String TOPP_STATES = "topp:states";

    public static final String WFS_SERVER_URL = "http://demo.opengeo.org/geoserver/wfs?";

    public static final String WMS_SERVER_URL = "http://demo.opengeo.org/geoserver/wms?";

    static Boolean remoteWMSStatesAvailable;

    static Boolean remoteWFSStatesAvailable;

    public static boolean isRemoteWFSStatesAvailable(Logger logger) {
        if (remoteWFSStatesAvailable == null) {
            // let's see if the remote ows tests are enabled to start with
            String value = System.getProperty("remoteOwsTests");
            if (value == null || !"TRUE".equalsIgnoreCase(value)) {
                logger.log(
                        Level.WARNING,
                        "Skipping remote WFS test because they were not enabled via -DremoteOwsTests=true");
                remoteWFSStatesAvailable = Boolean.FALSE;
            } else {
                // let's check if the remote WFS tests are runnable
                try {
                    WFSDataStoreFactory factory = new WFSDataStoreFactory();
                    Map<String, Serializable> params =
                            new HashMap(factory.getImplementationHints());
                    URL url =
                            new URL(
                                    WFS_SERVER_URL
                                            + "service=WFS&request=GetCapabilities&version=1.1.0");
                    params.put(WFSDataStoreFactory.URL.key, url);
                    params.put(WFSDataStoreFactory.TRY_GZIP.key, Boolean.TRUE);
                    // give it five seconds to respond...
                    params.put(WFSDataStoreFactory.TIMEOUT.key, Integer.valueOf(5000));
                    DataStore remoteStore = factory.createDataStore(params);
                    FeatureSource fs = remoteStore.getFeatureSource(TOPP_STATES);
                    remoteWFSStatesAvailable = Boolean.TRUE;
                    // check a basic response can be answered correctly
                    Query dq = new Query(TOPP_STATES);
                    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
                    dq.setFilter(ff.greater(ff.property("PERSONS"), ff.literal(20000000)));
                    FeatureCollection fc = fs.getFeatures(dq);
                    if (fc.size() != 1) {
                        logger.log(
                                Level.WARNING,
                                "Remote database status invalid, there should be one and only one "
                                        + "feature with more than 20M persons in topp:states");
                        remoteWFSStatesAvailable = Boolean.FALSE;
                    }

                    logger.log(
                            Level.WARNING,
                            "Remote WFS tests are enabled, remote server appears to be up");
                } catch (IOException e) {
                    logger.log(
                            Level.WARNING,
                            "Skipping remote wms test, either demo  "
                                    + "is down or the topp:states layer is not there",
                            e);
                    remoteWFSStatesAvailable = Boolean.FALSE;
                }
            }
        }
        return remoteWFSStatesAvailable.booleanValue();
    }

    public static boolean isRemoteWMSStatesAvailable(Logger logger) {
        if (remoteWMSStatesAvailable == null) {
            // let's see if the remote ows tests are enabled to start with
            String value = System.getProperty("remoteOwsTests");
            if (value == null || !"TRUE".equalsIgnoreCase(value)) {
                logger.log(
                        Level.WARNING,
                        "Skipping remote OWS test because they were not enabled via -DremoteOwsTests=true");
                remoteWMSStatesAvailable = Boolean.FALSE;
            } else {
                // let's check if the remote WFS tests are runnable
                try {
                    remoteWMSStatesAvailable = Boolean.FALSE;
                    WebMapServer server =
                            new WebMapServer(
                                    new URL(WMS_SERVER_URL + "service=WMS&request=GetCapabilities"),
                                    5000);
                    for (Layer l : server.getCapabilities().getLayerList()) {
                        if ("topp:states".equals(l.getName())) {
                            remoteWMSStatesAvailable = Boolean.TRUE;
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.log(
                            Level.WARNING,
                            "Skipping remote WMS test, either demo  "
                                    + "is down or the topp:states layer is not there",
                            e);
                    remoteWMSStatesAvailable = Boolean.FALSE;
                }
            }
        }
        return remoteWMSStatesAvailable.booleanValue();
    }
}
