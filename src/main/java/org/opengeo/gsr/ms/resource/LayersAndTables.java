package org.opengeo.gsr.ms.resource;

import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
import org.opengeo.gsr.core.renderer.Renderer;
import org.opengeo.gsr.core.renderer.StyleEncoder;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayersAndTables {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayersAndTables.class);

    public final List<LayerOrTable> layers;
    public final List<LayerOrTable> tables;

    private LayersAndTables(List<LayerOrTable> layers, List<LayerOrTable> tables) {
        this.layers = layers;
        this.tables = tables;
    }

    public static LayersAndTables find(Catalog catalog, String workspaceName) {
        List<LayerOrTable> layers = new ArrayList<LayerOrTable>();
        List<LayerOrTable> tables = new ArrayList<LayerOrTable>();
        int idCounter = 0;
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == LayerInfo.Type.VECTOR && l.getResource().getStore().getWorkspace().getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);
        for (LayerInfo l : layersInWorkspace) {
            ResourceInfo resource = l.getResource();
            try {
                ReferencedEnvelope boundingBox = resource.getLatLonBoundingBox();
                if (boundingBox != null) {
                        CoordinateReferenceSystem lonLat = CRS.decode("EPSG:4326");
                        CoordinateReferenceSystem WEB_MERCATOR = CRS.decode("EPSG:3857");
                        double minx = Math.max(boundingBox.getMinX(),  -180);
                        double maxx = Math.min(boundingBox.getMaxX(),   180);
                        double miny = Math.max(boundingBox.getMinY(), -85);
                        double maxy = Math.min(boundingBox.getMaxY(),  85);
                        ReferencedEnvelope sphericalMercatorBoundingBox = new ReferencedEnvelope(minx, maxx, miny, maxy, lonLat);
                        sphericalMercatorBoundingBox = sphericalMercatorBoundingBox.transform(WEB_MERCATOR, true);
                        boundingBox = sphericalMercatorBoundingBox;
                }
                Renderer renderer = StyleEncoder.effectiveRenderer(l);
                if (resource instanceof CoverageInfo) {
                        layers.add(new LayerOrTable(l, idCounter, GeometryTypeEnum.POLYGON, boundingBox, renderer));
                } else if (resource instanceof FeatureTypeInfo) {
                    final GeometryTypeEnum gtype;
                    GeometryDescriptor gDesc = ((FeatureTypeInfo)resource).getFeatureType().getGeometryDescriptor();
                    if (gDesc == null) {
                        gtype = null;
                    } else { 
                        gtype = GeometryTypeEnum.forJTSClass(gDesc.getType().getBinding());
                    }
                    if (gtype == null) {
                        tables.add(new LayerOrTable(l, idCounter, gtype, boundingBox, renderer));
                    } else {
                        layers.add(new LayerOrTable(l, idCounter, gtype, boundingBox, renderer));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Skipping layer " + l, e);
            } 
            idCounter++;
        }
        return new LayersAndTables(Collections.unmodifiableList(layers), Collections.unmodifiableList(tables));
    }

    @Override
    public String toString() {
        return layers.toString() + ";" + tables.toString();
    }
}
