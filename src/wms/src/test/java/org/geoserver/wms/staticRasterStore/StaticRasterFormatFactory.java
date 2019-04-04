/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.staticRasterStore;

import java.awt.*;
import java.util.Collections;
import java.util.Map;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

/** Simple factory for the static raster format. */
public final class StaticRasterFormatFactory implements GridFormatFactorySpi {

    public boolean isAvailable() {
        // we don't need anything specific
        return true;
    }

    public StaticRasterFormat createFormat() {
        return new StaticRasterFormat();
    }

    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }
}
