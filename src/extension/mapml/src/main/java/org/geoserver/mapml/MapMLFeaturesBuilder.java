/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
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
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.map.Layer;

public class MapMLFeaturesBuilder {
    private List<FeatureSource> featureSources;
    private final GeoServer geoServer;
    private final WMSMapContent mapContent;
    private final GetMapRequest getMapRequest;
    private final Query query;

    /**
     * Constructor
     *
     * @param mapContent the WMS map content
     * @param geoServer the GeoServer
     */
    public MapMLFeaturesBuilder(WMSMapContent mapContent, GeoServer geoServer, Query query) {
        this.geoServer = geoServer;
        this.mapContent = mapContent;
        this.getMapRequest = mapContent.getRequest();
        featureSources =
                mapContent.layers().stream()
                        .map(Layer::getFeatureSource)
                        .collect(Collectors.toList());
        this.query = query;
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
        FeatureCollection featureCollection = null;
        if (query != null) {
            featureCollection = featureSources.get(0).getFeatures(query);
        } else {
            featureCollection = featureSources.get(0).getFeatures();
        }
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            throw new ServiceException(
                    "MapML WMS Feature format does not currently support Complex Features.");
        }
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;

        GeometryDescriptor sourceGeometryDescriptor = fc.getSchema().getGeometryDescriptor();
        if (sourceGeometryDescriptor == null) {
            throw new ServiceException(
                    "MapML WMS Feature format does not currently support non-geometry features.");
        }
        SimpleFeatureCollection reprojectedFeatureCollection = null;
        if (!sourceGeometryDescriptor
                .getCoordinateReferenceSystem()
                .equals(getMapRequest.getCrs())) {
            try {
                reprojectedFeatureCollection =
                        new ReprojectingFeatureCollection(fc, getMapRequest.getCrs());
                ((ReprojectingFeatureCollection) reprojectedFeatureCollection)
                        .setDefaultSource(sourceGeometryDescriptor.getCoordinateReferenceSystem());
            } catch (SchemaException | FactoryException e) {
                throw new ServiceException(
                        "Unable to reproject to the requested coordinate references system", e);
            }
        } else {
            reprojectedFeatureCollection = fc;
        }

        LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        FeatureType featureType = fc.getSchema();
        ResourceInfo meta =
                geoServer.getCatalog().getResourceByName(featureType.getName(), ResourceInfo.class);
        return MapMLFeatureUtil.featureCollectionToMapML(
                reprojectedFeatureCollection,
                layerInfo,
                crs,
                null, // for WMS GetMap we don't include alternate projections
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
