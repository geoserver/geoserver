package com.boundlessgeo.gsr.translate.map;

import com.boundlessgeo.gsr.model.map.LayerNameComparator;
import com.boundlessgeo.gsr.model.map.LayerOrTable;
import com.boundlessgeo.gsr.model.map.LayersAndTables;
import org.geoserver.catalog.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LayerDAO {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayerDAO.class);

    /**
     * Look up a single GSR layer (with at least one geometry column) or table.
     *
     * @param catalog       GeoServer Catalog
     * @param workspaceName GeoServer workspace name
     * @param id            Index of Layer (based on sorting by layer name)
     * @return LayerOrTable from workspaceName identified by layerId
     * @throws IOException
     */
    public static LayerOrTable find(Catalog catalog, String workspaceName, Integer id) throws IOException {
        // short list all layers
        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace()
                .getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        // sort for "consistent" order
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);

        // retrieve indicated layer as LayerOrTable
        if (id < layersInWorkspace.size()) {
            LayerInfo resource = layersInWorkspace.get(id);
            return entry(resource, id);
        }
        return null; // not found
    }

    /**
     * Create LayerOrTable entry for layer.
     * <p>
     * Will return null, and log a warning if layer could not be represented
     * as LayerOrTable.
     *
     * @param layer
     * @param idCounter
     * @return LayerOrTable, or null if layer could not be represented
     */
    public static LayerOrTable entry(LayerInfo layer, int idCounter) throws IOException {
        ResourceInfo resource = layer.getResource();

        if (resource instanceof CoverageInfo || resource instanceof FeatureTypeInfo) {
            return new LayerOrTable(layer, idCounter);
        }
        return null; // Skipping layer
    }

    /**
     * LayersAndTables lookup for GeoServer workspace.
     *
     * @param catalog
     * @param workspaceName
     * @return GeoServer Layers gathered into GSR layers (with at least one geometry column) or tables.
     */
    public static LayersAndTables find(Catalog catalog, String workspaceName) {
        List<LayerOrTable> layers = new ArrayList<>();
        List<LayerOrTable> tables = new ArrayList<>();
        int idCounter = 0;
        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace()
                .getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);
        for (LayerInfo l : layersInWorkspace) {
            try {
                LayerOrTable entry = entry(l, idCounter);
                if (entry != null) {
                    if (entry.getGeometryType() != null) {
                        layers.add(entry);
                    } else {
                        tables.add(entry);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Skipping layer " + l, e);
            }
            idCounter++;
        }
        return new LayersAndTables(new ArrayList<>(layers), new ArrayList<>(tables));
    }
}
