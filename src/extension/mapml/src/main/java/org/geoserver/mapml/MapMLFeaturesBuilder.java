/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.map.Layer;

public class MapMLFeaturesBuilder {
    private List<FeatureSource> featureSources;
    private final GeoServer geoServer;
    private final WMSMapContent mapContent;
    private final HttpServletRequest httpServletRequest;
    private final GetMapRequest getMapRequest;

    /**
     * Constructor
     *
     * @param mapContent the WMS map content
     * @param geoServer the GeoServer
     * @param httpServletRequest the HTTP servlet request
     */
    public MapMLFeaturesBuilder(
            WMSMapContent mapContent, GeoServer geoServer, HttpServletRequest httpServletRequest) {
        this.geoServer = geoServer;
        this.mapContent = mapContent;
        this.getMapRequest = mapContent.getRequest();
        this.httpServletRequest = httpServletRequest;
        featureSources =
                mapContent.layers().stream()
                        .map(Layer::getFeatureSource)
                        .collect(Collectors.toList());
    }

    /**
     * Produce a MapML document
     *
     * @return a MapML document
     * @throws IOException If an error occurs while producing the map
     */
    public Mapml getMapMLDocument() throws IOException {
        if (featureSources.size() != 1) {
            throw new ServiceException(
                    "MapML WMS Feature format does not currently support Multiple Feature Type output.");
        }

        if (!getMapRequest.getLayers().isEmpty()
                && MapLayerInfo.TYPE_VECTOR != getMapRequest.getLayers().get(0).getType()) {
            throw new ServiceException(
                    "MapML WMS Feature format does not currently support non-vector layers.");
        }
        FeatureCollection featureCollection = featureSources.get(0).getFeatures();
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            throw new ServiceException(
                    "MapML WMS Feature format does not currently support Complex Features.");
        }
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;

        LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        FeatureType featureType = fc.getSchema();
        ResourceInfo meta =
                geoServer.getCatalog().getResourceByName(featureType.getName(), ResourceInfo.class);
        return MapMLFeatureUtil.featureCollectionToMapML(
                fc,
                layerInfo,
                crs,
                MapMLFeatureUtil.alternateProjections(
                        ResponseUtils.baseURL(httpServletRequest),
                        "wms",
                        KvpUtils.normalize(httpServletRequest.getParameterMap())),
                getNumberOfDecimals(meta),
                getForcedDecimal(meta),
                getPadWithZeros(meta));
    }

    /**
     * Get the number of decimals for coordinates
     *
     * @param meta the ResourceInfo metadata for the feature type
     * @return the number of decimals
     */
    private int getNumberOfDecimals(ResourceInfo meta) {

        if (meta instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) meta;
            if (featureTypeInfo.getNumDecimals() > 0) {
                return featureTypeInfo.getNumDecimals();
            }
            return ((FeatureTypeInfo) meta).getNumDecimals();
        }
        SettingsInfo settings = geoServer.getSettings();
        return settings.getNumDecimals();
    }

    /**
     * Get the pad with zeros setting
     *
     * @param meta the ResourceInfo metadata for the feature type
     * @return the pad with zeros setting
     */
    private boolean getPadWithZeros(ResourceInfo meta) {
        if (meta instanceof FeatureTypeInfo) {
            return ((FeatureTypeInfo) meta).getPadWithZeros();
        }
        return false;
    }

    /**
     * Get the forced decimal setting
     *
     * @param meta the ResourceInfo metadata for the feature type
     * @return the forced decimal setting
     */
    private boolean getForcedDecimal(ResourceInfo meta) {
        if (meta instanceof FeatureTypeInfo) {
            return ((FeatureTypeInfo) meta).getForcedDecimal();
        }
        return false;
    }
}
