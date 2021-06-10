/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import org.geoserver.util.DimensionWarning;

/**
 * A {@link DimensionWarning}, with indication of which animation frame was affected by the
 * dimension issue
 */
public class FrameWarning extends DimensionWarning {

    private final int frame;

    protected FrameWarning(DimensionWarning warning, int frame) {
        super(warning);
        this.frame = frame;
    }

    public int getFrame() {
        return frame;
    }
}
