/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.awt.Color;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.opengis.feature.Feature;

/**
 * Creates a "color" for each feature
 *
 * @author Andrea Aime - GeoSolutions
 */
class UTFGridColorFunction extends InternalVolatileFunction {

    UTFGridEntries entries;

    public UTFGridColorFunction(UTFGridEntries entries) {
        this.entries = entries;
    }

    @Override
    public Object evaluate(Object object) {
        if (!(object instanceof Feature)) {
            // cannot handle this, make it "transparent"
            return Color.BLACK;
        }
        Feature feature = (Feature) object;
        int key = entries.getKeyForFeature(feature);
        return new Color(key, false);
    }
}
