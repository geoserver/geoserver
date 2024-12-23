/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.feature.ReprojectingFeatureCollection;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

public class MapMLFeaturesBuilder {

    static final Logger LOGGER = Logging.getLogger(MapMLFeaturesBuilder.class);
    private MapMLSimplifier simplifier;
    private final SimpleFeatureSource featureSource;
    private final GeoServer geoServer;
    private final WMSMapContent mapContent;
    private final GetMapRequest getMapRequest;
    private final Query query;

    private boolean skipAttributes = false;

    private boolean skipHeadStyles = false;

    /**
     * Constructor
     *
     * @param mapContent the WMS map content
     * @param geoServer the GeoServer
     */
    public MapMLFeaturesBuilder(WMSMapContent mapContent, GeoServer geoServer, Query query) {
        if (mapContent.layers().size() != 1)
            throw new ServiceException(
                    "MapML WMS Feature format does not currently support Multiple Feature Type output.");
        FeatureSource fs = mapContent.layers().get(0).getFeatureSource();
        if (!(fs instanceof SimpleFeatureSource))
            throw new ServiceException("MapML WMS Feature format does not currently support Complex Features.");
        this.featureSource = (SimpleFeatureSource) fs;

        this.geoServer = geoServer;
        this.mapContent = mapContent;
        this.getMapRequest = mapContent.getRequest();
        this.query = new Query(query);
        this.simplifier = getSimplifier(mapContent, query);
    }

    private MapMLSimplifier getSimplifier(WMSMapContent mapContent, Query query) {
        try {
            CoordinateReferenceSystem layerCRS = featureSource.getSchema().getCoordinateReferenceSystem();
            MapMLSimplifier simplifier = new MapMLSimplifier(mapContent, layerCRS);
            if (featureSource.getSupportedHints().contains(Hints.GEOMETRY_DISTANCE)) {
                query.getHints().put(Hints.GEOMETRY_DISTANCE, simplifier.getQuerySimplificationDistance());
            }
            return simplifier;
        } catch (FactoryException e) {
            LOGGER.log(Level.WARNING, "Unable to create MapML Simplifier, proceeding with full geometries", e);
        }
        return null;
    }

    /** Enables/disables attribute representation skipping (false by default) */
    public void setSkipAttributes(boolean skipAttributes) {
        this.skipAttributes = skipAttributes;
    }

    /** Enables/disables attribute representation skipping (false by default) */
    public void setSkipHeadStyles(boolean skipHeadStyles) {
        this.skipHeadStyles = skipHeadStyles;
    }

    /**
     * Produce a MapML document
     *
     * @return a MapML document
     * @throws IOException If an error occurs while producing the map
     */
    public Mapml getMapMLDocument() throws IOException {
        if (!getMapRequest.getLayers().isEmpty()
                && MapLayerInfo.TYPE_VECTOR != getMapRequest.getLayers().get(0).getType()) {
            throw new ServiceException("MapML WMS Feature format does not currently support non-vector layers.");
        }
        SimpleFeatureCollection fc = null;
        if (query != null) {
            fc = featureSource.getFeatures(query);
        } else {
            fc = featureSource.getFeatures();
        }

        GeometryDescriptor sourceGeometryDescriptor = fc.getSchema().getGeometryDescriptor();
        if (sourceGeometryDescriptor == null) {
            throw new ServiceException("MapML WMS Feature format does not currently support non-geometry features.");
        }
        SimpleFeatureCollection reprojectedFeatureCollection = null;
        if (!sourceGeometryDescriptor.getCoordinateReferenceSystem().equals(getMapRequest.getCrs())) {
            try {
                reprojectedFeatureCollection = new ReprojectingFeatureCollection(fc, getMapRequest.getCrs());
                ((ReprojectingFeatureCollection) reprojectedFeatureCollection)
                        .setDefaultSource(sourceGeometryDescriptor.getCoordinateReferenceSystem());
            } catch (SchemaException | FactoryException e) {
                throw new ServiceException("Unable to reproject to the requested coordinate references system", e);
            }
        } else {
            reprojectedFeatureCollection = fc;
        }

        Map<String, MapMLStyle> styles = getMapMLStyleMap();

        LayerInfo layerInfo =
                geoServer.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        FeatureType featureType = fc.getSchema();
        ResourceInfo meta = geoServer.getCatalog().getResourceByName(featureType.getName(), ResourceInfo.class);
        if (query != null && query.getFilter() != null && query.getFilter().equals(Filter.EXCLUDE)) {
            // if the filter is exclude, return an empty MapML that has header metadata
            return MapMLFeatureUtil.getEmptyMapML(layerInfo, crs);
        }

        return MapMLFeatureUtil.featureCollectionToMapML(
                reprojectedFeatureCollection,
                layerInfo,
                getMapRequest.getBbox(), // clip on bound
                crs,
                null, // for WMS GetMap we don't include alternate projections
                getNumberOfDecimals(meta),
                getForcedDecimal(meta),
                getPadWithZeros(meta),
                styles,
                skipHeadStyles,
                skipAttributes,
                simplifier);
    }

    private Map<String, MapMLStyle> getMapMLStyleMap() throws IOException {
        Style style = getMapRequest.getStyles().get(0);
        if (style == null) {
            StyleInfo styleInfo =
                    getMapRequest.getLayers().get(0).getLayerInfo().getDefaultStyle();
            if (styleInfo != null && styleInfo.getStyle() != null) {
                style = styleInfo.getStyle();
            } else {
                throw new ServiceException("No style or default style found for layer"
                        + getMapRequest.getLayers().get(0).getLayerInfo().getName());
            }
        }
        return MapMLFeatureUtil.getMapMLStyleMap(style, mapContent.getScaleDenominator());
    }

    /**
     * Get the number of decimals for coordinates
     *
     * @param meta the ResourceInfo metadata for the feature type
     * @return the number of decimals
     */
    private int getNumberOfDecimals(ResourceInfo meta) {
        SettingsInfo settings = geoServer.getSettings();
        if (meta instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) meta;
            if (featureTypeInfo.getNumDecimals() > 0) {
                return featureTypeInfo.getNumDecimals();
            }
        }

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
