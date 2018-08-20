/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

public class CollectionLayer {

    String workspace;

    String layer;

    boolean separateBands;

    String[] bands;

    String[] browseBands;

    boolean heterogeneousCRS;

    String mosaicCRS;

    public CollectionLayer() {
        super();
        // TODO Auto-generated constructor stub
    }

    public CollectionLayer(
            String workspace,
            String layer,
            boolean separateBands,
            String[] bands,
            String[] browseBands,
            boolean heterogeneousCRS,
            String targetCRS) {
        super();
        this.workspace = workspace;
        this.layer = layer;
        this.separateBands = separateBands;
        this.bands = bands;
        this.browseBands = browseBands;
        this.heterogeneousCRS = heterogeneousCRS;
        this.mosaicCRS = targetCRS;
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
     * Builds a CollectionLayer bean from the {@link OpenSearchAccess#LAYER} property of a
     * Collection feature.
     *
     * @param feature
     * @return The layer, or null if the property was not found
     */
    public static CollectionLayer buildCollectionLayerFromFeature(Feature feature) {
        // map to a single bean
        CollectionLayer layer = null;
        Property p = feature.getProperty(OpenSearchAccess.LAYER);
        if (p != null && p instanceof Feature) {
            Feature lf = (Feature) p;
            layer = new CollectionLayer();
            layer.setWorkspace((String) getAttribute(lf, "workspace"));
            layer.setLayer((String) getAttribute(lf, "layer"));
            layer.setSeparateBands(Boolean.TRUE.equals(getAttribute(lf, "separateBands")));
            layer.setBands((String[]) getAttribute(lf, "bands"));
            layer.setBrowseBands((String[]) getAttribute(lf, "browseBands"));
            layer.setHeterogeneousCRS(Boolean.TRUE.equals(getAttribute(lf, "heterogeneousCRS")));
            layer.setMosaicCRS((String) getAttribute(lf, "mosaicCRS"));
        }
        return layer;
    }

    private static Object getAttribute(Feature sf, String name) {
        Property p = sf.getProperty(name);
        if (p != null) {
            return p.getValue();
        } else {
            return null;
        }
    }
}
