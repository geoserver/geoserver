/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;

abstract class AbstractVectorLayerIdentifier implements LayerIdentifier {

    private static final double TOLERANCE = 1e-6;

    public boolean canHandle(MapLayerInfo layer) {
        int type = layer.getType();
        return type == MapLayerInfo.TYPE_VECTOR || type == MapLayerInfo.TYPE_REMOTE_VECTOR;
    }

    /**
     * Selects the rules active at this zoom level
     *
     * @param style
     * @param scaleDenominator
     */
    protected List<Rule> getActiveRules(Style style, double scaleDenominator) {
        List<Rule> result = new ArrayList<Rule>();

        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule r : fts.rules()) {
                if ((r.getMinScaleDenominator() - TOLERANCE <= scaleDenominator)
                        && (r.getMaxScaleDenominator() + TOLERANCE > scaleDenominator)
                        && r.getSymbolizers() != null
                        && r.getSymbolizers().length > 0) {
                    result.add(r);
                }
            }
        }
        return result;
    }
}
