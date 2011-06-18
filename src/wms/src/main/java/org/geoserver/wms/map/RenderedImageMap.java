/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.util.Collections;
import java.util.List;

import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WebMap;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * A {@link WebMap} where the map is given by a {@link RenderedImage}
 * 
 * @author Gabriel Roldan
 */
public class RenderedImageMap extends WebMap {

    private RenderedImage image;

    private List<GridCoverage2D> renderedCoverages;

    public RenderedImageMap(final WMSMapContext mapContext, final RenderedImage image,
            final String mimeType) {
        super(mapContext);
        this.image = image;
        setMimeType(mimeType);
    }

    public RenderedImage getImage() {
        return image;
    }

    @Override
    protected void disposeInternal() {
        image = null;
    }

    /**
     * Returns the list of rendered coverages to produce this map, needed so they're disposed after
     * writing them down to the destination output stream when their rendered images are used
     * directly instead of pre-rendered to a buffered image or such.
     * 
     * @return the list of rendered coverages or {@code null}
     */
    @SuppressWarnings("unchecked")
    public List<GridCoverage2D> getRenderedCoverages() {
        return renderedCoverages == null ? Collections.EMPTY_LIST : renderedCoverages;
    }

    /**
     * Allows to store the coverages rendered for this map so that they can be disposed at a later
     * stage. Useful in case there's a rendering pipeline making direct use of the coverages
     */
    public void setRenderedCoverages(List<GridCoverage2D> renderedCoverages) {
        this.renderedCoverages = renderedCoverages;
    }

    /**
     * Access to the map's context this map is created for
     */
    public WMSMapContext getMapContext() {
        return mapContext;
    }

}
