/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

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

    public CollectionLayer(String workspace, String layer, boolean separateBands, String[] bands,
            String[] browseBands, boolean heterogeneousCRS, String targetCRS) {
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

}
