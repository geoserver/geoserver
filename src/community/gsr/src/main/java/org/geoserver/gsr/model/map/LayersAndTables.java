/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.util.ArrayList;
import org.geoserver.catalog.Catalog;
import org.geoserver.gsr.model.AbstractGSRModel;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.translate.map.LayerDAO;

/**
 * A list of {@link LayerOrTable}, that can be serialized as JSON
 *
 * <p>Also provides a number of static utility methods for interacting with this list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayersAndTables extends AbstractGSRModel implements GSRModel {

    public final ArrayList<LayerOrTable> layers;

    public final ArrayList<LayerOrTable> tables;

    public LayersAndTables(ArrayList<LayerOrTable> layers, ArrayList<LayerOrTable> tables) {
        this.layers = layers;
        this.tables = tables;
    }

    @Override
    public String toString() {
        return layers.toString() + ";" + tables.toString();
    }

    /**
     * Layer names are just integers IDs in Esri, but not in GeoServer. This method is basically a
     * hack and really ought to be rethought.
     *
     * <p>TODO
     *
     * @param catalog
     * @param layerName
     * @param workspaceName
     * @return
     */
    public static String integerIdToGeoserverLayerName(
            Catalog catalog, String layerName, String workspaceName) {
        String name = layerName;
        try {
            LayerOrTable layerOrTable =
                    LayerDAO.find(catalog, workspaceName, Integer.parseInt(layerName));
            name = layerOrTable.getName();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (NumberFormatException e) {
            // Just use string layer name for now.
        }
        return workspaceName + ":" + name;
    }

    public ArrayList<LayerOrTable> getLayers() {
        return layers;
    }

    public ArrayList<LayerOrTable> getTables() {
        return tables;
    }
}
