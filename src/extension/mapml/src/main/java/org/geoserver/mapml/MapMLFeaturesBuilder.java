/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import jakarta.servlet.http.HttpServletRequest;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.geotools.map.Layer;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public class MapMLFeaturesBuilder {

    static final Logger LOGGER = Logging.getLogger(MapMLFeaturesBuilder.class);
    private final List<Query> queryList = new ArrayList<>();
    private final GeoServer geoServer;
    private final WMSMapContent mapContent;
    private final GetMapRequest getMapRequest;

    private boolean skipAttributes = false;

    private boolean skipHeadStyles = false;

    private final HttpServletRequest request;

    /**
     * Constructor
     *
     * @param mapContent the WMS map content
     * @param geoServer the GeoServer
     */
    public MapMLFeaturesBuilder(
            WMSMapContent mapContent, GeoServer geoServer, List<Query> queries, HttpServletRequest request)
            throws IOException {
        int i = 0;
        if (queries.size() != mapContent.layers().size()) {
            throw new ServiceException("The number of queries must match the number of layers");
        }
        for (Layer layer : mapContent.layers()) {
            FeatureSource fs = layer.getFeatureSource();
            if (!(fs instanceof SimpleFeatureSource))
                throw new ServiceException("MapML WMS Feature format does not currently support Complex Features.");
            Query query = queries.get(i);
            queryList.add(query);
            i++;
        }
        this.geoServer = geoServer;
        this.mapContent = mapContent;
        this.getMapRequest = mapContent.getRequest();
        this.request = request;
    }

    private MapMLSimplifier getSimplifier(FeatureSource featureSource, WMSMapContent mapContent, Query query) {
        try {
            CoordinateReferenceSystem layerCRS = featureSource.getSchema().getCoordinateReferenceSystem();
            return new MapMLSimplifier(mapContent, layerCRS);
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
        List<MapMLFeatureUtil.LayerSimplfierContext> layerSimplfierContexts = new ArrayList<>();
        int i = 0;
        for (Layer layer : mapContent.layers()) {
            SimpleFeatureCollection fc = null;
            FeatureSource fs = layer.getFeatureSource();
            if (!(fs instanceof SimpleFeatureSource))
                throw new ServiceException("MapML WMS Feature format does not currently support Complex Features.");
            SimpleFeatureSource featureSource = (SimpleFeatureSource) fs;
            Query query = queryList.get(i);
            MapMLSimplifier simplifier = getSimplifier(featureSource, mapContent, query);

            if (query != null) {
                Set<RenderingHints.Key> fsHints = featureSource.getSupportedHints();
                if (fsHints.contains(Hints.SCREENMAP)) {
                    query.getHints().put(Hints.SCREENMAP, simplifier.getScreenMap());
                    // we don't want to run the screenmap simplification twice
                    simplifier.setScreenMap(null);
                }
                // LineString and MultiLineString geometries can be simplified without worrying about topology
                // while polygons and collections containing polygons need to have their topology preserved.
                // Points cannot be simplified, however, they can still be selected out by the pre-generalized plugin.
                Class<?> binding = featureSource
                        .getSchema()
                        .getGeometryDescriptor()
                        .getType()
                        .getBinding();
                double simplificationDistance = simplifier.getQuerySimplificationDistance();
                if (LineString.class.isAssignableFrom(binding)
                        || MultiLineString.class.isAssignableFrom(binding)
                                && fsHints.contains(Hints.GEOMETRY_SIMPLIFICATION)) {
                    query.getHints().put(Hints.GEOMETRY_SIMPLIFICATION, simplificationDistance);
                } else if (fsHints.contains(Hints.GEOMETRY_DISTANCE)) {
                    query.getHints().put(Hints.GEOMETRY_DISTANCE, simplificationDistance);
                }
                fc = featureSource.getFeatures(query);
            } else {
                fc = featureSource.getFeatures();
            }

            GeometryDescriptor sourceGeometryDescriptor = fc.getSchema().getGeometryDescriptor();
            if (sourceGeometryDescriptor == null) {
                throw new ServiceException(
                        "MapML WMS Feature format does not currently support non-geometry features.");
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

            LayerInfo layerInfo =
                    geoServer.getCatalog().getLayerByName(fc.getSchema().getTypeName());
            ResourceInfo resourceInfo = null;
            if (layerInfo == null) {
                resourceInfo = geoServer.getCatalog().getCoverageByName(layer.getTitle());
            } else {
                resourceInfo = layerInfo.getResource();
            }
            CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
            FeatureType featureType = fc.getSchema();
            ResourceInfo meta = geoServer.getCatalog().getResourceByName(featureType.getName(), ResourceInfo.class);
            if (query != null && query.getFilter() != null && query.getFilter().equals(Filter.EXCLUDE)) {
                // if the filter is exclude, return an empty MapML that has header metadata
                return MapMLFeatureUtil.getEmptyMapML(layerInfo, crs);
            }
            Map<String, MapMLStyle> styles =
                    getMapMLStyleMap(getMapRequest.getStyles().get(i), layerInfo);
            MapMLFeatureUtil.LayerSimplfierContext layerSimplfierContext = new MapMLFeatureUtil.LayerSimplfierContext(
                    reprojectedFeatureCollection,
                    resourceInfo,
                    simplifier,
                    getNumberOfDecimals(meta),
                    getForcedDecimal(meta),
                    getPadWithZeros(meta),
                    styles);
            layerSimplfierContexts.add(layerSimplfierContext);
            i++;
        }
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        return MapMLFeatureUtil.layerContextsToMapMLDocument(
                layerSimplfierContexts,
                getMapRequest.getBbox(), // clip on bound
                crs,
                null, // for WMS GetMap we don't include alternate projections
                skipHeadStyles,
                skipAttributes,
                request,
                getMapRequest);
    }

    private Map<String, MapMLStyle> getMapMLStyleMap(Style style, LayerInfo layerInfo) throws IOException {
        if (style == null) {
            StyleInfo styleInfo = layerInfo.getDefaultStyle();
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
        if (meta instanceof FeatureTypeInfo featureTypeInfo) {
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
        if (meta instanceof FeatureTypeInfo info) {
            return info.getPadWithZeros();
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
        if (meta instanceof FeatureTypeInfo info) {
            return info.getForcedDecimal();
        }
        return false;
    }
}
