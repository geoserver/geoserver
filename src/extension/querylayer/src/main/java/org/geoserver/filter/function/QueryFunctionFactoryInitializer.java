/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filter.function;

import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;

/**
 * Sets the catalog reference inside the {@link QueryLayerFunctionFactory}
 *
 * @author Andrea Aime - GeoSolutions
 */
// for the moment we implement both to make it testable, eventually this should
// be driven by configuration only and at that point we'll really need it to
// implement both
public class QueryFunctionFactoryInitializer
        implements GeoServerLifecycleHandler, GeoServerInitializer {

    GeoServer geoServer;

    public void onDispose() {
        // nothing do to
    }

    public void beforeReload() {
        // nothing to do
    }

    public void onReload() {
        configure();
    }

    private void configure() {
        Integer maxFeatures =
                parseInteger(GeoServerExtensions.getProperty("QUERY_LAYER_MAX_FEATURES"));
        Long maxCoordinates =
                parseLong(GeoServerExtensions.getProperty("GEOMETRY_COLLECT_MAX_COORDINATES"));

        Set<FunctionFactory> factories = CommonFactoryFinder.getFunctionFactories(null);
        for (FunctionFactory ff : factories) {
            if (ff instanceof QueryLayerFunctionFactory) {
                QueryLayerFunctionFactory factory = (QueryLayerFunctionFactory) ff;
                if (maxFeatures != null) {
                    factory.setMaxFeatures(maxFeatures);
                }
                if (maxCoordinates != null) {
                    factory.setMaxCoordinates(maxCoordinates);
                }
                factory.setCatalog(geoServer.getCatalog());
            }
        }
    }

    public void onReset() {
        configure();
    }

    private Integer parseInteger(String property) {
        if (property != null && !"".equals(property.trim())) {
            return Integer.parseInt(property);
        }
        return null;
    }

    private Long parseLong(String property) {
        if (property != null && !"".equals(property.trim())) {
            return Long.parseLong(property);
        }
        return null;
    }

    public void initialize(GeoServer geoServer) throws Exception {
        this.geoServer = geoServer;
        configure();
    }
}
