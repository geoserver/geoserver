/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import org.geotools.map.Layer;

/**
 * RenderedImageTimeDecorator is a wrapper for objects of type RenderedImage which allows to track
 * rendering times of a raster render operation updating the rendering time whenever data is
 * computed
 */
public class RenderedImageTimeDecorator implements RenderedImage {

    private RenderedImage delegate;

    private RenderTimeStatistics statistics;

    private Layer layer;

    public RenderedImageTimeDecorator(RenderedImage delegate) {
        this.delegate = delegate;
        this.statistics = new RenderTimeStatistics();
    }

    @Override
    public Vector<RenderedImage> getSources() {
        return delegate.getSources();
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public ColorModel getColorModel() {
        return delegate.getColorModel();
    }

    @Override
    public SampleModel getSampleModel() {
        return delegate.getSampleModel();
    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public int getMinX() {
        return delegate.getMinX();
    }

    @Override
    public int getMinY() {
        return delegate.getMinY();
    }

    @Override
    public int getNumXTiles() {
        return delegate.getNumXTiles();
    }

    @Override
    public int getNumYTiles() {
        return delegate.getNumYTiles();
    }

    @Override
    public int getMinTileX() {
        return delegate.getMinTileX();
    }

    @Override
    public int getMinTileY() {
        return delegate.getMinTileY();
    }

    @Override
    public int getTileWidth() {
        return delegate.getTileWidth();
    }

    @Override
    public int getTileHeight() {
        return delegate.getTileHeight();
    }

    @Override
    public int getTileGridXOffset() {
        return delegate.getTileGridXOffset();
    }

    @Override
    public int getTileGridYOffset() {
        return delegate.getTileGridYOffset();
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        updateRenderingTime();
        return delegate.getTile(tileX, tileY);
    }

    @Override
    public Raster getData() {
        updateRenderingTime();
        return delegate.getData();
    }

    @Override
    public Raster getData(Rectangle rect) {
        return delegate.getData(rect);
    }

    @Override
    public WritableRaster copyData(WritableRaster raster) {
        return delegate.copyData(raster);
    }

    public void updateRenderingTime() {
        statistics.layerEnd(layer);
    }

    public RenderTimeStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(RenderTimeStatistics statistics) {
        this.statistics = statistics;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
        onSettingLayer(this.layer);
    }

    public void onSettingLayer(Layer layer) {
        this.statistics.layerStart(layer);
    }

    public RenderedImage getDelegate() {
        return delegate;
    }
}
