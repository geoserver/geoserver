/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gsr.model.AbstractGSRModel;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.geometry.SpatialReference;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geoserver.gsr.translate.geometry.SpatialReferences;
import org.geoserver.wms.WMSInfo;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;

/** Detailed model of a MapService */
public class MapServiceRoot extends AbstractGSRModel implements GSRModel {

    public final String mapName;
    public final List<LayerEntry> layers = new ArrayList<>();
    public final DateRange timeInfo;
    public final Boolean singleFusedMapCache;
    public final String capabilities;
    private SpatialReference spatialReference;

    private String workspace;

    public MapServiceRoot(WMSInfo service, String workspace, List<LayerInfo> layers)
            throws IOException {
        this.mapName = service.getTitle() != null ? service.getTitle() : service.getName();
        this.workspace = workspace;
        int count = 0;
        for (LayerInfo l : layers) {
            this.layers.add(new LayerEntry(count, l.getName()));
            count++;
        }
        Date[] dateRange = getCumulativeDateRange(layers);
        if (dateRange != null) {
            this.timeInfo = new DateRange(dateRange);
        } else {
            this.timeInfo = null;
        }
        this.singleFusedMapCache = false;
        this.capabilities = "Query";
        /* TODO it's not clear what this should actually be in a GeoServer context
         * Services in ArcGIS are more like layerGroups than workspaces, and have an SRS associated with them.
         * GeoServer doesn't have an equivalent concept for workspaces. We could define an algorithm to calculate
         * a default WKID based on the layers in the workspace, but that would be no less hacky.
         */
        this.spatialReference = new SpatialReferenceWKID(SpatialReferences.DEFAULT_WKID);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Date[] getCumulativeDateRange(List<LayerInfo> layers) throws IOException {
        Comparable overallMin = null;
        Comparable overallMax = null;
        for (LayerInfo l : layers) {
            if (l.getResource().getClass().isAssignableFrom(FeatureTypeInfo.class)) {
                FeatureTypeInfo ftInfo = (FeatureTypeInfo) l.getResource();
                DimensionInfo dimensionInfo =
                        ftInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (dimensionInfo != null && dimensionInfo.isEnabled()) {
                    String timeProperty = dimensionInfo.getAttribute();
                    FeatureSource<? extends FeatureType, ? extends Feature> source =
                            ftInfo.getFeatureSource(null, null);
                    FeatureCollection<? extends FeatureType, ? extends Feature> features =
                            source.getFeatures();
                    MaxVisitor max =
                            new MaxVisitor(timeProperty, (SimpleFeatureType) features.getSchema());
                    MinVisitor min =
                            new MinVisitor(timeProperty, (SimpleFeatureType) features.getSchema());
                    features.accepts(min, null);
                    features.accepts(max, null);
                    if (min.getResult() != CalcResult.NULL_RESULT) {
                        if (overallMin == null) {
                            overallMin = min.getMin();
                        } else {
                            overallMin =
                                    min.getMin().compareTo(overallMin) < 0
                                            ? min.getMin()
                                            : overallMin;
                        }
                    }

                    if (max.getResult() != CalcResult.NULL_RESULT) {
                        if (overallMax == null) {
                            overallMax = max.getMax();
                        } else {
                            overallMax =
                                    max.getMax().compareTo(overallMax) > 0
                                            ? max.getMax()
                                            : overallMax;
                        }
                    }
                }
            }
        }
        if (overallMin == null || overallMax == null) {
            return null;
        } else {
            return new Date[] {(Date) overallMin, (Date) overallMax};
        }
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getMapName() {
        return mapName;
    }

    public List<LayerEntry> getLayers() {
        return layers;
    }

    public DateRange getTimeInfo() {
        return timeInfo;
    }

    public Boolean getSingleFusedMapCache() {
        return singleFusedMapCache;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }
}
