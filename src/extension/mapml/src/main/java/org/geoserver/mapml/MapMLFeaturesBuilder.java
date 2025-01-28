/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.CoverageInfo;
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
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.map.Layer;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public class MapMLFeaturesBuilder {

    static final Logger LOGGER = Logging.getLogger(MapMLFeaturesBuilder.class);
    private List<FeatureSourceSimplifier> featureSourceSimplifiers = new ArrayList<>();
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
        for (Layer layer : mapContent.layers()) {
            FeatureSource fs = layer.getFeatureSource();
            if (!(fs instanceof SimpleFeatureSource))
                throw new ServiceException("MapML WMS Feature format does not currently support Complex Features.");
            Query query = queries.get(i);
            MapMLSimplifier simplifier = getSimplifier(fs, mapContent, query);
            FeatureSourceSimplifier fss = new FeatureSourceSimplifier(
                    (SimpleFeatureSource) fs,
                    simplifier,
                    (SimpleFeatureCollection) getFeatureCollection(fs, query, simplifier),
                    query);
            featureSourceSimplifiers.add(fss);
            i++;
        }
        this.geoServer = geoServer;
        this.mapContent = mapContent;
        this.getMapRequest = mapContent.getRequest();
        this.request = request;
    }

    private FeatureCollection getFeatureCollection(FeatureSource featureSource, Query query, MapMLSimplifier simplifier)
            throws IOException {
        FeatureCollection fc = null;

        if (query != null) {
            if (featureSource.getSupportedHints().contains(Hints.SCREENMAP)) {
                query.getHints().put(Hints.SCREENMAP, simplifier.getScreenMap());
                // we don't want to run the screenmap simplification twice
                simplifier.setScreenMap(null);
            }
            // LineString and MultiLineString geometries can be simplified without worrying about topology
            Class<?> binding =
                    featureSource.getSchema().getGeometryDescriptor().getType().getBinding();
            if (LineString.class.isAssignableFrom(binding) || MultiLineString.class.isAssignableFrom(binding)) {
                if (featureSource.getSupportedHints().contains(Hints.GEOMETRY_SIMPLIFICATION)) {
                    query.getHints().put(Hints.GEOMETRY_SIMPLIFICATION, simplifier);
                }
            }
            fc = featureSource.getFeatures(query);
        } else {
            fc = featureSource.getFeatures();
        }
        return fc;
    }

    private MapMLSimplifier getSimplifier(FeatureSource featureSource, WMSMapContent mapContent, Query query) {
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
        List<MapMLFeatureUtil.FeatureCollectionInfoSimplifier> featureCollectionInfoSimplifiers = new ArrayList<>();
        int i = 0;
        for (Layer layer : mapContent.layers()) {
            SimpleFeatureCollection fc = null;
            FeatureSource fs = layer.getFeatureSource();
            if (!(fs instanceof SimpleFeatureSource))
                throw new ServiceException("MapML WMS Feature format does not currently support Complex Features.");
            SimpleFeatureSource featureSource = (SimpleFeatureSource) fs;
            Query query = featureSourceSimplifiers.get(i).getQuery();
            MapMLSimplifier simplifier = getSimplifier(featureSource, mapContent, query);

            if (query != null) {
                if (featureSource.getSupportedHints().contains(Hints.SCREENMAP)) {
                    query.getHints().put(Hints.SCREENMAP, simplifier.getScreenMap());
                    // we don't want to run the screenmap simplification twice
                    simplifier.setScreenMap(null);
                }
                // LineString and MultiLineString geometries can be simplified without worrying about topology
                Class<?> binding = featureSource
                        .getSchema()
                        .getGeometryDescriptor()
                        .getType()
                        .getBinding();
                if (LineString.class.isAssignableFrom(binding) || MultiLineString.class.isAssignableFrom(binding)) {
                    if (featureSource.getSupportedHints().contains(Hints.GEOMETRY_SIMPLIFICATION)) {
                        query.getHints().put(Hints.GEOMETRY_SIMPLIFICATION, simplifier);
                    }
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
            CoverageInfo coverageInfo = null;
            if (layerInfo == null) {
                coverageInfo = geoServer.getCatalog().getCoverageByName(layer.getTitle());
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
            MapMLFeatureUtil.FeatureCollectionInfoSimplifier featureCollectionInfoSimplifier =
                    new MapMLFeatureUtil.FeatureCollectionInfoSimplifier(
                            reprojectedFeatureCollection,
                            layerInfo,
                            coverageInfo,
                            simplifier,
                            getNumberOfDecimals(meta),
                            getForcedDecimal(meta),
                            getPadWithZeros(meta),
                            styles);
            featureCollectionInfoSimplifiers.add(featureCollectionInfoSimplifier);
            i++;
        }
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        return MapMLFeatureUtil.featureCollectionToMapML(
                featureCollectionInfoSimplifiers,
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

    private class FeatureSourceSimplifier {
        private final SimpleFeatureSource featureSource;
        private final MapMLSimplifier simplifier;
        private final SimpleFeatureCollection fc;
        private final Query query;

        private FeatureSourceSimplifier(
                SimpleFeatureSource featureSource,
                MapMLSimplifier simplifier,
                SimpleFeatureCollection fc,
                Query query) {
            this.featureSource = featureSource;
            this.simplifier = simplifier;
            this.fc = fc;
            this.query = new Query(query);
        }

        SimpleFeatureSource getFeatureSource() {
            return featureSource;
        }

        MapMLSimplifier getSimplifier() {
            return simplifier;
        }

        SimpleFeatureCollection getFeatureCollection() {
            return fc;
        }

        Query getQuery() {
            return query;
        }
    }
}
