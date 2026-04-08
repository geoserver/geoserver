/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.data;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.vectortiles.store.VectorTilesFeatureSource;
import org.geotools.vectortiles.store.VectorTilesRequestScale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the GeoServer PMTiles plugin integration with the WMS service.
 *
 * <p>This configuration class defines the beans required for PMTiles integration with GeoServer:
 *
 * <ul>
 *   <li>{@link VectorTilesRequestScaleDenominatorHook} - Sets scale denominator for optimal zoom level selection
 * </ul>
 *
 * @see VectorTilesRequestScaleDenominatorHook
 */
@Configuration(proxyBeanMethods = false)
public class PMTilesWmsIntegrationConfiguration {
    /**
     * Creates the dispatcher callback hook for setting the scale denominator on WMS requests.
     *
     * @return the scale denominator hook bean
     */
    @Bean
    VectorTilesRequestScaleDenominatorHook pmTilesScaleSetter() {
        return new VectorTilesRequestScaleDenominatorHook();
    }

    /**
     * Hooks into {@code GetMap} and {@code GetFeatureInfo} requests pre-processing to set the scale denominator on
     * {@link VectorTilesRequestScale}, which supersedes the {@code GEOMETRY_GENERALIZATION} and
     * {@code GEOMETRY_DISTANCE} hints provided by the renderer when querying {@link VectorTilesFeatureSource}.
     *
     * <p>Rationale being that {@code StreamingRenderer} computes the generalization distance in a way that's
     * inconsistent with the map scale, results in different values for different tiles on the same map, and often,
     * especially when doing reprojection, the generalization distances result in querying much higher tile levels than
     * it should, leading to traversing too many tiles from the PMTiles or other vector tiles datastore. It also always
     * performs the query in the layer's native CRS.
     */
    static class VectorTilesRequestScaleDenominatorHook extends AbstractDispatcherCallback
            implements GetMapCallback, DispatcherCallback {

        /**
         * {@link GetMapCallback} method to set the scale denominator from {@link WMSMapContent#getScaleDenominator()}
         * on {@link VectorTilesRequestScale}, which supersedes the {@code GEOMETRY_GENERALIZATION} and
         * {@code GEOMETRY_DISTANCE} hints provided by the renderer when querying {@link VectorTilesFeatureSource}.
         *
         * <p>Rationale being that {@link StreamingRenderer} computes the generalization distance in a way that's
         * inconsistent with the map scale, results in different values for different tiles on the same map, and often,
         * especially when doing reprojection, the generalization distances result in querying much higher tile levels
         * than it should, leading to traversing too many tiles from the PMTiles or other vector tiles datastore. It
         * also always performs the query in the layer's native CRS.
         */
        @Override
        public WMSMapContent beforeRender(WMSMapContent mapContent) {
            double scaleDenominator = mapContent.getScaleDenominator();
            setScaleDenominator(scaleDenominator);
            return mapContent;
        }

        /**
         * {@link DispatcherCallback} hook to handle {@link GetFeatureInfoRequest} to compute the scale denominator set
         * to {@link VectorTilesRequestScale} so it matches the one from the {@code GetMap} request and the search is
         * performed against the same zoom level in the {@link VectorTilesFeatureSource}.
         */
        @Override
        public Operation operationDispatched(Request request, Operation operation) {
            GetFeatureInfoRequest gfi = extractGetFeatureInfo(operation);
            if (gfi != null) {
                FeatureInfoRequestParameters paramsExtractor = new FeatureInfoRequestParameters(gfi);
                double scaleDenominator = paramsExtractor.getScaleDenominator();
                setScaleDenominator(scaleDenominator);
            }
            return operation;
        }

        /**
         * {@link DispatcherCallback} hook to clear the {@link VectorTilesRequestScale} regardless of the executed
         * request.
         */
        @Override
        public void finished(Request request) {
            VectorTilesRequestScale.clear();
        }

        private void setScaleDenominator(double scaleDenominator) {
            VectorTilesRequestScale.set(scaleDenominator);
        }

        private GetFeatureInfoRequest extractGetFeatureInfo(Operation operation) {
            Object[] params = operation.getParameters();
            if (params != null && params.length > 0 && params[0] instanceof GetFeatureInfoRequest gfi) {
                return gfi;
            }
            return null;
        }

        /** No-op */
        @Override
        public WebMap finished(WebMap map) {
            return map;
        }

        /** No-op */
        @Override
        public void failed(Throwable t) {
            // no-op
        }

        /** No-op */
        @Override
        public GetMapRequest initRequest(GetMapRequest request) {
            return request;
        }

        /** No-op */
        @Override
        public void initMapContent(WMSMapContent mapContent) {
            // no-op
        }

        /** No-op */
        @Override
        public Layer beforeLayer(WMSMapContent mapContent, Layer layer) {
            return layer;
        }
    }
}
