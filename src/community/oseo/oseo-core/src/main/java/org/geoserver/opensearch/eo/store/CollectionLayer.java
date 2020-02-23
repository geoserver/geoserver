/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

public class CollectionLayer {

    String workspace;

    String layer;

    boolean separateBands;

    String[] bands;

    String[] browseBands;

    boolean heterogeneousCRS;

    String mosaicCRS;

    boolean defaultLayer;

    boolean timeRanges;

    public CollectionLayer() {
        super();
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public boolean isSeparateBands() {
        return separateBands;
    }

    public void setSeparateBands(boolean separateBands) {
        this.separateBands = separateBands;
    }

    public String[] getBands() {
        return bands;
    }

    public void setBands(String[] bands) {
        this.bands = bands;
    }

    public String[] getBrowseBands() {
        return browseBands;
    }

    public void setBrowseBands(String[] browseBands) {
        this.browseBands = browseBands;
    }

    public boolean isHeterogeneousCRS() {
        return heterogeneousCRS;
    }

    public void setHeterogeneousCRS(boolean heterogeneousCRS) {
        this.heterogeneousCRS = heterogeneousCRS;
    }

    public String getMosaicCRS() {
        return mosaicCRS;
    }

    public void setMosaicCRS(String targetCRS) {
        this.mosaicCRS = targetCRS;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Builds a CollectionLayer bean from the {@link OpenSearchAccess#LAYERS} property of a
     * Collection feature.
     *
     * @return The layer, or null if the property was not found
     */
    public static List<CollectionLayer> buildCollectionLayersFromFeature(Feature feature)
            throws IOException {
        // map to a single bean
        List<CollectionLayer> result = new ArrayList<>();
        Collection<Property> layers = feature.getProperties(OpenSearchAccess.LAYERS);
        if (layers != null) {
            for (Property p : layers) {
                SimpleFeature lf = (SimpleFeature) p;
                CollectionLayer layer = new CollectionLayer();
                layer.setWorkspace((String) getAttribute(lf, "workspace"));
                layer.setLayer((String) getAttribute(lf, "layer"));
                layer.setSeparateBands(Boolean.TRUE.equals(getAttribute(lf, "separateBands")));
                layer.setBands((String[]) getAttribute(lf, "bands"));
                layer.setBrowseBands((String[]) getAttribute(lf, "browseBands"));
                layer.setHeterogeneousCRS(
                        Boolean.TRUE.equals(getAttribute(lf, "heterogeneousCRS")));
                layer.setMosaicCRS((String) getAttribute(lf, "mosaicCRS"));
                layer.setDefaultLayer(
                        Optional.ofNullable((Boolean) getAttribute(lf, "defaultLayer"))
                                .orElse(false));
                result.add(layer);
            }
        }
        return result;
    }

    private static Object getAttribute(Feature sf, String name) {
        Property p = sf.getProperty(name);
        if (p != null) {
            return p.getValue();
        } else {
            return null;
        }
    }

    /**
     * Returns the default layer property
     *
     * @return True is this is the default layer for the collection, false otherwise
     */
    public boolean isDefaultLayer() {
        return defaultLayer;
    }

    /** Sets the default layer property (only one should be the default) */
    public void setDefaultLayer(boolean defaultLayer) {
        this.defaultLayer = defaultLayer;
    }

    public boolean isTimeRanges() {
        return timeRanges;
    }

    public void setTimeRanges(boolean timeRanges) {
        this.timeRanges = timeRanges;
    }
}
