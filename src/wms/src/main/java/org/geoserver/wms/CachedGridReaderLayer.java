/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.map.GridReaderLayer;
import org.geotools.styling.Style;
import org.opengis.parameter.GeneralParameterValue;

/**
 * A grid reader layer that works with a cached reader, that is, a reader that the layer does not own
 * and thus should not dispose of
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CachedGridReaderLayer extends GridReaderLayer {

    public CachedGridReaderLayer(AbstractGridCoverage2DReader reader, Style style) {
        super(reader, style);
    }

    public CachedGridReaderLayer(AbstractGridCoverage2DReader reader, Style style, String title) {
        super(reader, style, title);
    }

    public CachedGridReaderLayer(AbstractGridCoverage2DReader reader, Style style,
            GeneralParameterValue[] params) {
        super(reader, style, params);
    }

    public CachedGridReaderLayer(AbstractGridCoverage2DReader reader, Style style, String title,
            GeneralParameterValue[] params) {
        super(reader, style, title, params);
    }
    
    @Override
    public void dispose() {
        this.reader = null;
        this.style = null;
        this.params = null;
        super.dispose();
    }

}
